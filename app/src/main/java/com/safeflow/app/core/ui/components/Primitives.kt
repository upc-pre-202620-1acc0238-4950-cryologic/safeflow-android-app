package com.safeflow.app.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.safeflow.app.R
import com.safeflow.app.core.ui.icons.SafeFlowSymbol
import com.safeflow.app.core.ui.theme.SafeFlowRadius
import com.safeflow.app.core.ui.theme.SafeFlowSizes
import com.safeflow.app.core.ui.theme.SafeFlowSpacing
import com.safeflow.app.core.ui.theme.SafeFlowTextStyles
import com.safeflow.app.core.ui.theme.SafeFlowTheme

enum class SafeFlowTextVariant { Display, Headline, Title, BodyLarge, BodyMedium, Label, Caption, Overline }
enum class SafeFlowButtonTone { Primary, Secondary, Outlined, Danger }
enum class SafeFlowTone { Normal, Warning, Critical, Muted, Info, Success }

internal fun textStyle(variant: SafeFlowTextVariant): TextStyle = when (variant) {
    SafeFlowTextVariant.Display -> SafeFlowTextStyles.display
    SafeFlowTextVariant.Headline -> SafeFlowTextStyles.headline
    SafeFlowTextVariant.Title -> SafeFlowTextStyles.title
    SafeFlowTextVariant.BodyLarge -> SafeFlowTextStyles.bodyLarge
    SafeFlowTextVariant.BodyMedium -> SafeFlowTextStyles.bodyMedium
    SafeFlowTextVariant.Label -> SafeFlowTextStyles.label
    SafeFlowTextVariant.Caption -> SafeFlowTextStyles.caption
    SafeFlowTextVariant.Overline -> SafeFlowTextStyles.overline
}

@Composable
fun SafeFlowText(
    text: String,
    modifier: Modifier = Modifier,
    variant: SafeFlowTextVariant = SafeFlowTextVariant.BodyMedium,
    color: Color = SafeFlowTheme.colors.textPrimary,
) {
    Text(text, modifier = modifier, style = textStyle(variant), color = color)
}

private val SymbolFont = FontFamily(Font(R.font.material_symbols_rounded))

/** Decorative symbols remain silent; meaningful icons receive a content description. */
@Composable
fun SafeFlowIcon(
    symbol: SafeFlowSymbol,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = SafeFlowSizes.iconLarge,
    tint: Color = SafeFlowTheme.colors.textSecondary,
) {
    val glyphSize = with(LocalDensity.current) { size.toSp() }
    Text(
        text = symbol.glyph,
        modifier = modifier.size(size).clearAndSetSemantics {
            if (contentDescription != null) this.contentDescription = contentDescription
        },
        fontFamily = SymbolFont,
        fontSize = glyphSize,
        lineHeight = glyphSize,
        color = tint,
    )
}

@Composable
fun SafeFlowButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: SafeFlowButtonTone = SafeFlowButtonTone.Primary,
    enabled: Boolean = true,
    icon: SafeFlowSymbol? = null,
) {
    val colors = SafeFlowTheme.colors
    val content: @Composable () -> Unit = {
        Row(horizontalArrangement = Arrangement.spacedBy(SafeFlowSpacing.sm), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) SafeFlowIcon(icon, null, tint = androidx.compose.material3.LocalContentColor.current)
            Text(label, style = SafeFlowTextStyles.label)
        }
    }
    val target = modifier.heightIn(min = SafeFlowSizes.touchTarget)
    val shape = RoundedCornerShape(SafeFlowRadius.medium)
    if (tone == SafeFlowButtonTone.Outlined) {
        OutlinedButton(
            onClick = onClick, modifier = target, enabled = enabled, shape = shape,
            border = BorderStroke(1.dp, colors.outline),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.primary, containerColor = colors.surface),
        ) { content() }
    } else {
        val container = when (tone) {
            SafeFlowButtonTone.Danger -> colors.critical
            SafeFlowButtonTone.Secondary -> colors.primaryContainer
            else -> colors.primary
        }
        Button(
            onClick = onClick, modifier = target, enabled = enabled, shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = container,
                contentColor = if (tone == SafeFlowButtonTone.Secondary) colors.primary else colors.inverse,
                disabledContainerColor = colors.outline,
                disabledContentColor = colors.textSecondary,
            ),
        ) { content() }
    }
}

@Composable
fun SafeFlowIconButton(
    symbol: SafeFlowSymbol,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    IconButton(onClick = onClick, enabled = enabled, modifier = modifier.size(SafeFlowSizes.touchTarget)) {
        SafeFlowIcon(symbol, contentDescription, tint = SafeFlowTheme.colors.primary)
    }
}

@Composable
fun SafeFlowChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = SafeFlowTheme.colors
    FilterChip(
        selected = selected, onClick = onClick, enabled = enabled,
        modifier = modifier.heightIn(min = SafeFlowSizes.chipHeight),
        shape = RoundedCornerShape(SafeFlowRadius.medium),
        border = BorderStroke(0.dp, Color.Transparent),
        label = { Text(label, style = SafeFlowTextStyles.label) },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = colors.surface, labelColor = colors.textSecondary,
            selectedContainerColor = colors.primary, selectedLabelColor = colors.inverse,
        ),
    )
}

@Composable
internal fun toneContainer(tone: SafeFlowTone): Color = with(SafeFlowTheme.colors) {
    when (tone) {
        SafeFlowTone.Warning -> warningContainer
        SafeFlowTone.Critical -> criticalContainer
        SafeFlowTone.Muted -> background
        else -> primaryContainer
    }
}

@Composable
internal fun toneContent(tone: SafeFlowTone): Color = with(SafeFlowTheme.colors) {
    when (tone) {
        SafeFlowTone.Warning -> warning
        SafeFlowTone.Critical -> critical
        SafeFlowTone.Muted -> textSecondary
        else -> primary
    }
}
