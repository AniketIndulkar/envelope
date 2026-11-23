package dev.novanest.envelope.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import dev.novanest.envelope.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Atkinson Hyperlegible: designed by the Braille Institute for low-vision readers. Used for everything you operate. */
val UiFamily = FontFamily(
    Font(R.font.atkinson_regular, FontWeight.Normal),
    Font(R.font.atkinson_bold, FontWeight.Bold),
)

/** Source Serif 4 (variable): letter text, so the letter reads as paper. */
@OptIn(ExperimentalTextApi::class)
val LetterFamily = FontFamily(
    Font(R.font.source_serif, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.source_serif, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
)

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
