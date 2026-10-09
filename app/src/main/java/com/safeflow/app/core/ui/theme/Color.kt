package com.safeflow.app.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Semantic Light tokens from SafeFlow/Color in Figma. */
@Immutable
data class SafeFlowColors(
    val background: Color = Color(0xFFF8FAF9),
    val surface: Color = Color.White,
    val textPrimary: Color = Color(0xFF17221B),
    val textSecondary: Color = Color(0xFF536158),
    val primary: Color = Color(0xFF15803D),
    val primaryContainer: Color = Color(0xFFEAF5EE),
    val outline: Color = Color(0xFFDCE5DF),
    val warning: Color = Color(0xFFB45309),
    val warningContainer: Color = Color(0xFFFFF5E6),
    val critical: Color = Color(0xFFB91C1C),
    val criticalContainer: Color = Color(0xFFFDECEC),
    val inverse: Color = Color.White,
)

internal val LocalSafeFlowColors = staticCompositionLocalOf { SafeFlowColors() }
