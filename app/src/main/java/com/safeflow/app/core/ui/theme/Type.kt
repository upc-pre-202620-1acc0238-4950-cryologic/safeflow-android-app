package com.safeflow.app.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.safeflow.app.R

val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

private fun style(size: Int, line: Int, weight: FontWeight) = TextStyle(
    fontFamily = Inter, fontWeight = weight, fontSize = size.sp,
    lineHeight = line.sp, letterSpacing = 0.sp,
    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
)

object SafeFlowTextStyles {
    val display = style(32, 40, FontWeight.Bold)
    val headline = style(24, 32, FontWeight.SemiBold)
    val title = style(18, 24, FontWeight.SemiBold)
    val bodyLarge = style(16, 24, FontWeight.Normal)
    val bodyMedium = style(14, 20, FontWeight.Normal)
    val label = style(14, 20, FontWeight.SemiBold)
    val caption = style(12, 16, FontWeight.Medium)
    val overline = style(12, 16, FontWeight.SemiBold)
}

val SafeFlowTypography = Typography(
    displayLarge = SafeFlowTextStyles.display,
    displayMedium = SafeFlowTextStyles.display,
    displaySmall = SafeFlowTextStyles.display,
    headlineLarge = SafeFlowTextStyles.headline,
    headlineMedium = SafeFlowTextStyles.headline,
    headlineSmall = SafeFlowTextStyles.headline,
    titleLarge = SafeFlowTextStyles.title,
    titleMedium = SafeFlowTextStyles.title,
    titleSmall = SafeFlowTextStyles.label,
    bodyLarge = SafeFlowTextStyles.bodyLarge,
    bodyMedium = SafeFlowTextStyles.bodyMedium,
    bodySmall = SafeFlowTextStyles.caption,
    labelLarge = SafeFlowTextStyles.label,
    labelMedium = SafeFlowTextStyles.label,
    labelSmall = SafeFlowTextStyles.overline,
)
