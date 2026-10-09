package com.safeflow.app.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import com.safeflow.app.core.ui.icons.SafeFlowSymbol
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.safeflow.app.core.ui.theme.SafeFlowRadius
import com.safeflow.app.core.ui.theme.SafeFlowSpacing
import com.safeflow.app.core.ui.theme.SafeFlowTheme

@Composable
fun SafeFlowListCard(
    title: String,
    detail: String,
    value: String,
    status: String,
    modifier: Modifier = Modifier,
    tone: SafeFlowTone = SafeFlowTone.Normal,
    onClick: (() -> Unit)? = null,
    icon: SafeFlowSymbol? = null,
    extra: (@Composable () -> Unit)? = null,
) {
    val colors = SafeFlowTheme.colors
    val content: @Composable () -> Unit = {
        Column(Modifier.padding(SafeFlowSpacing.lg), verticalArrangement = Arrangement.spacedBy(SafeFlowSpacing.sm)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SafeFlowSpacing.md)) {
                if (icon != null) SafeFlowIcon(icon, null, tint = toneContent(tone))
                SafeFlowText(title, variant = SafeFlowTextVariant.Title)
            }
            if (detail.isNotBlank()) SafeFlowText(detail, variant = SafeFlowTextVariant.BodyMedium, color = colors.textSecondary)
            if (value.isNotBlank()) SafeFlowText(value, variant = SafeFlowTextVariant.Headline)
            if (status.isNotBlank()) SafeFlowText(status, variant = SafeFlowTextVariant.Caption, color = if (tone in listOf(SafeFlowTone.Normal, SafeFlowTone.Success)) colors.textSecondary else toneContent(tone))
            extra?.invoke()
        }
    }
    val container = colors.surface
    val shape = RoundedCornerShape(SafeFlowRadius.medium)
    val border = BorderStroke(1.dp, colors.outline)
    if (onClick == null) {
        Surface(modifier, shape = shape, color = container, border = border) { content() }
    } else {
        Surface(onClick = onClick, modifier = modifier, shape = shape, color = container, border = border) { content() }
    }
}

@Composable
fun SafeFlowCallout(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    tone: SafeFlowTone = SafeFlowTone.Info,
    onClick: (() -> Unit)? = null,
) {
    val content: @Composable () -> Unit = {
        Column(Modifier.fillMaxWidth().padding(SafeFlowSpacing.lg), verticalArrangement = Arrangement.spacedBy(SafeFlowSpacing.sm)) {
            SafeFlowText(title, variant = SafeFlowTextVariant.Label, color = toneContent(tone))
            SafeFlowText(body, variant = SafeFlowTextVariant.BodyMedium)
        }
    }
    if (onClick == null) Surface(modifier = modifier, color = toneContainer(tone), shape = RoundedCornerShape(SafeFlowRadius.medium)) { content() }
    else Surface(onClick = onClick, modifier = modifier, color = toneContainer(tone), shape = RoundedCornerShape(SafeFlowRadius.medium)) { content() }
}
