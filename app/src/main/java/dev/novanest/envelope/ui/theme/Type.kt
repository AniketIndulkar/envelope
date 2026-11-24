package dev.novanest.envelope.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.novanest.envelope.R

@OptIn(ExperimentalTextApi::class)
private fun instrument(weight: FontWeight) = Font(
    R.font.instrument_sans, weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight), FontVariation.width(100f)),
)

/** Instrument Sans (variable, bundled so nothing is fetched at runtime): everything you operate. */
val UiFamily = FontFamily(
    instrument(FontWeight.Normal),
    instrument(FontWeight.Medium),
    instrument(FontWeight.SemiBold),
    instrument(FontWeight.Bold),
)

/** Source Serif 4 (variable): letter text, so the letter reads as paper. */
@OptIn(ExperimentalTextApi::class)
val LetterFamily = FontFamily(
    Font(R.font.source_serif, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.source_serif, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
)

/** Tracking from the prototype: titles -0.025em, big numbers -0.05em, body -0.01em. */
val TitleTracking = (-0.025).em
val NumberTracking = (-0.05).em
val BodyTracking = (-0.01).em

private fun ui(size: Int, line: Int, weight: FontWeight = FontWeight.Normal, tracking: TextUnit = 0.em) =
    TextStyle(fontFamily = UiFamily, fontSize = size.sp, lineHeight = line.sp, fontWeight = weight, letterSpacing = tracking)

val Typography = Typography(
    displayLarge = ui(72, 68, FontWeight.Bold, NumberTracking),
    displayMedium = ui(34, 35, FontWeight.Bold, TitleTracking),
    displaySmall = ui(30, 34, FontWeight.Bold, TitleTracking),
    headlineSmall = ui(26, 30, FontWeight.Bold, TitleTracking),
    titleLarge = ui(20, 26, FontWeight.Bold, TitleTracking),
    titleMedium = ui(17, 23, FontWeight.SemiBold, BodyTracking),
    bodyLarge = ui(18, 26, tracking = BodyTracking),
    bodyMedium = ui(16, 23),
    bodySmall = ui(14, 20),
    labelLarge = ui(17, 22, FontWeight.SemiBold, BodyTracking),
    labelMedium = ui(14, 18, FontWeight.SemiBold),
    labelSmall = ui(13, 16, FontWeight.Bold),
)

/** The huge days-left number on the pass and the Answer cloud: 116sp Bold, tight, tabular. */
val BigNumber = TextStyle(
    fontFamily = UiFamily, fontSize = 116.sp, lineHeight = 90.sp, fontWeight = FontWeight.Bold,
    letterSpacing = NumberTracking, fontFeatureSettings = "tnum",
    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Bottom, LineHeightStyle.Trim.Both),
)

/** Unit text next to a big number ("days to pay"): 21sp Bold. */
val UnitText = ui(21, 23, FontWeight.Bold, TitleTracking)

val LetterBody = TextStyle(fontFamily = LetterFamily, fontSize = 16.sp, lineHeight = 27.sp, color = Ink)

/** Scrambling glyphs while a detail resolves into its stand-in. */
val ScrambleText = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold)
