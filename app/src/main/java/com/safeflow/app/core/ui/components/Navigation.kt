package com.safeflow.app.core.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.safeflow.app.core.ui.icons.SafeFlowSymbol
import com.safeflow.app.core.ui.theme.SafeFlowRadius
import com.safeflow.app.core.ui.theme.SafeFlowSizes
import com.safeflow.app.core.ui.theme.SafeFlowSpacing
import com.safeflow.app.core.ui.theme.SafeFlowTheme

/** UI contract; it deliberately contains no IAM role or navigation destination. */
@Immutable
data class SafeFlowNavigationItem(val id: String, val label: String, val icon: SafeFlowSymbol)

@Composable
fun SafeFlowNavItem(
    item: SafeFlowNavigationItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SafeFlowTheme.colors
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = SafeFlowSizes.navigationItem).semantics {
            this.selected = selected
            role = Role.Tab
        },
        shape = RoundedCornerShape(SafeFlowRadius.medium),
        color = if (selected) colors.primaryContainer else colors.surface,
    ) {
        Column(
            Modifier.padding(SafeFlowSpacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(SafeFlowSpacing.xs),
        ) {
            SafeFlowIcon(item.icon, null, tint = colors.primary)
            SafeFlowText(item.label, variant = SafeFlowTextVariant.Caption, color = if (selected) colors.primary else colors.textSecondary)
        }
    }
}

@Composable
fun SafeFlowBottomNavigation(
    items: List<SafeFlowNavigationItem>,
    selectedId: String?,
    onItemClick: (SafeFlowNavigationItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier, color = SafeFlowTheme.colors.surface) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
        val itemWidth = ((maxWidth - SafeFlowSpacing.xl * 2 - SafeFlowSpacing.md * (items.size - 1)) / items.size.coerceAtLeast(1)).coerceAtMost(106.dp)
        Row(
            Modifier.fillMaxWidth().padding(horizontal = SafeFlowSpacing.xl, vertical = SafeFlowSpacing.sm),
            horizontalArrangement = Arrangement.spacedBy(SafeFlowSpacing.md, Alignment.CenterHorizontally),
        ) {
            items.forEach { item ->
                SafeFlowNavItem(
                    item = item, selected = item.id == selectedId,
                    onClick = { onItemClick(item) }, modifier = Modifier.width(itemWidth),
                )
            }
        }
    }
    }
}

@Composable
fun SafeFlowAppHeader(
    title: String,
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
    onAccountClick: (() -> Unit)? = null,
) {
    Surface(modifier, color = SafeFlowTheme.colors.surface) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 80.dp).padding(horizontal = SafeFlowSpacing.lg),
            horizontalArrangement = Arrangement.spacedBy(SafeFlowSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBackClick != null) SafeFlowIconButton(SafeFlowSymbol.Back, "Volver", onBackClick)
            SafeFlowText(title, Modifier.weight(1f), variant = SafeFlowTextVariant.Title)
            if (onAccountClick != null) SafeFlowIconButton(SafeFlowSymbol.Account, "Cuenta", onAccountClick)
        }
    }
}

@Composable
fun SafeFlowBrand(
    modifier: Modifier = Modifier,
    tagline: String? = null,
) {
    val colors = SafeFlowTheme.colors
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SafeFlowSpacing.md),
    ) {
        SafeFlowIcon(SafeFlowSymbol.Snowflake, null, size = 32.dp, tint = colors.primary)
        Column(verticalArrangement = Arrangement.spacedBy(SafeFlowSpacing.xs)) {
            SafeFlowText("SafeFlow", variant = SafeFlowTextVariant.Display)
            if (tagline != null) SafeFlowText(tagline, variant = SafeFlowTextVariant.Caption, color = colors.textSecondary)
        }
    }
}
