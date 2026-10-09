package com.safeflow.app.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

object SafeFlowTheme {
    val colors: SafeFlowColors
        @Composable @ReadOnlyComposable get() = LocalSafeFlowColors.current
}

/** Only the approved Light mode is implemented. Wireframe is a design artifact. */
@Composable
fun SafeFlowTheme(content: @Composable () -> Unit) {
    val colors = SafeFlowColors()
    CompositionLocalProvider(LocalSafeFlowColors provides colors) {
        MaterialTheme(
            colorScheme = lightColorScheme(
                primary = colors.primary, onPrimary = colors.inverse,
                primaryContainer = colors.primaryContainer, onPrimaryContainer = colors.primary,
                secondary = colors.primary, onSecondary = colors.inverse,
                secondaryContainer = colors.primaryContainer, onSecondaryContainer = colors.primary,
                tertiary = colors.warning, onTertiary = colors.inverse,
                tertiaryContainer = colors.warningContainer, onTertiaryContainer = colors.warning,
                background = colors.background, onBackground = colors.textPrimary,
                surface = colors.surface, onSurface = colors.textPrimary,
                surfaceVariant = colors.background, onSurfaceVariant = colors.textSecondary,
                surfaceDim = colors.outline, surfaceBright = colors.surface,
                surfaceContainerLowest = colors.surface, surfaceContainerLow = colors.background,
                surfaceContainer = colors.background, surfaceContainerHigh = colors.background,
                surfaceContainerHighest = colors.outline,
                inverseSurface = colors.textPrimary, inverseOnSurface = colors.inverse,
                inversePrimary = colors.primaryContainer,
                surfaceTint = colors.primary, outline = colors.outline,
                outlineVariant = colors.outline,
                error = colors.critical, onError = colors.inverse,
                errorContainer = colors.criticalContainer, onErrorContainer = colors.critical,
            ),
            typography = SafeFlowTypography,
            shapes = Shapes(
                extraSmall = RoundedCornerShape(SafeFlowRadius.small),
                small = RoundedCornerShape(SafeFlowRadius.small),
                medium = RoundedCornerShape(SafeFlowRadius.medium),
                large = RoundedCornerShape(SafeFlowRadius.large),
                extraLarge = RoundedCornerShape(SafeFlowRadius.large),
            ),
            content = content,
        )
    }
}
