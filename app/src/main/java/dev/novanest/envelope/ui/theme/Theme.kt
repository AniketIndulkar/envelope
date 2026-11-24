package dev.novanest.envelope.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private val Scheme = lightColorScheme(
    primary = Ink, onPrimary = Surface, primaryContainer = StandInSoft, onPrimaryContainer = StandIn,
    secondary = StandIn, onSecondary = Surface, secondaryContainer = Mine, onSecondaryContainer = Ink,
    tertiary = Accent, onTertiary = Ink,
    background = PhoneBg, onBackground = Ink, surface = Surface, onSurface = Ink,
    surfaceVariant = SoftSurface, onSurfaceVariant = Muted, outline = Hairline3, outlineVariant = Hairline1,
    inverseSurface = Sky, inverseOnSurface = OnSky,
    error = UrgentRed, onError = Surface, errorContainer = RiskBg, onErrorContainer = RiskInk,
)

/** One light theme on purpose: the phone is paper-light, the cloud above the line is always plum. */
@Composable
fun EnvelopeTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalReducedMotion provides rememberReducedMotion()) {
        MaterialTheme(colorScheme = Scheme, typography = Typography, content = content)
    }
}
