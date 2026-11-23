package dev.novanest.envelope.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val Scheme = lightColorScheme(
    primary = Primary, onPrimary = CardWhite, primaryContainer = Tint, onPrimaryContainer = Deep,
    secondary = Deep, onSecondary = CardWhite,
    background = Canvas, onBackground = Ink, surface = CardWhite, onSurface = Ink,
    surfaceVariant = Neutral, onSurfaceVariant = InkSoft, outline = Rule, outlineVariant = Rule,
    error = Urgent, onError = CardWhite, errorContainer = UrgentBg, onErrorContainer = Urgent,
)

/** One light theme on purpose: the app is about paper letters, and the security-envelope blue is the brand. */
@Composable
fun EnvelopeTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, typography = Typography, content = content)
}
