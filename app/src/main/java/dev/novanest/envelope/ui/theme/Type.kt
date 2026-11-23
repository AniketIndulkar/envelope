package dev.novanest.envelope.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Design calls for Atkinson Hyperlegible (UI, built for low-vision readers) and Source Serif 4 (letters).
 * Swap these two families for the bundled fonts once they are added to res/font.
 */
val UiFamily: FontFamily = FontFamily.SansSerif
val LetterFamily: FontFamily = FontFamily.Serif

private fun ui(size: Int, line: Int, weight: FontWeight = FontWeight.Normal, tracking: Float = 0f) =
    TextStyle(fontFamily = UiFamily, fontSize = size.sp, lineHeight = line.sp, fontWeight = weight, letterSpacing = tracking.sp)

val Typography = Typography(
    displayLarge = ui(72, 68, FontWeight.Bold, -2f),
    displaySmall = ui(30, 35, FontWeight.Bold, -0.5f),
    headlineSmall = ui(26, 30, FontWeight.Bold, -0.3f),
    titleLarge = ui(20, 26, FontWeight.Bold),
    titleMedium = ui(17, 23, FontWeight.Bold),
    bodyLarge = ui(18, 26),
    bodyMedium = ui(16, 23),
    bodySmall = ui(14, 20),
    labelLarge = ui(18, 22, FontWeight.Bold),
    labelMedium = ui(14, 18, FontWeight.Bold),
)

val LetterBody = TextStyle(fontFamily = LetterFamily, fontSize = 16.sp, lineHeight = 27.sp, color = Ink)
