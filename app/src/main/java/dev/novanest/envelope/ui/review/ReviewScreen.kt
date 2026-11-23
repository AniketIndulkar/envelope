package dev.novanest.envelope.ui.review

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.CurrencyPound
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.novanest.envelope.ai.Engine
import dev.novanest.envelope.ai.QuickCheck
import dev.novanest.envelope.data.Prefs
import dev.novanest.envelope.privacy.RedactionMode
import dev.novanest.envelope.ui.Busy
import dev.novanest.envelope.ui.Dates
import dev.novanest.envelope.ui.SessionUi
import dev.novanest.envelope.ui.components.EnvelopeButton
import dev.novanest.envelope.ui.components.EnvelopeCard
import dev.novanest.envelope.ui.components.EnvelopeTopBar
import dev.novanest.envelope.ui.components.IconTile
import dev.novanest.envelope.ui.components.LoadingCard
import dev.novanest.envelope.ui.components.Notice
import dev.novanest.envelope.ui.components.Pill
import dev.novanest.envelope.ui.components.RedactedLetter
import dev.novanest.envelope.ui.components.deadlineColors
import dev.novanest.envelope.ui.components.securityHatch
import dev.novanest.envelope.ui.theme.CardWhite
import dev.novanest.envelope.ui.theme.Deep
import dev.novanest.envelope.ui.theme.Ink
import dev.novanest.envelope.ui.theme.InkBody
import dev.novanest.envelope.ui.theme.InkSoft
import dev.novanest.envelope.ui.theme.Neutral
import dev.novanest.envelope.ui.theme.OnDeepSoft
import dev.novanest.envelope.ui.theme.Primary
import dev.novanest.envelope.ui.theme.Rule
import dev.novanest.envelope.ui.theme.SoonBg
import dev.novanest.envelope.ui.theme.SoonInk
import dev.novanest.envelope.ui.theme.SoonTile
import dev.novanest.envelope.ui.theme.Tint
import dev.novanest.envelope.ui.theme.Urgent
import dev.novanest.envelope.ui.theme.UrgentBg

@Composable
fun ReviewScreen(
    ui: SessionUi,
    onBack: () -> Unit,
    onHideWord: (String) -> Unit,
    onShowValue: (String) -> Unit,
    onLanguage: (String) -> Unit,
    onStartAgain: () -> Unit,
    onExplain: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())) {
            EnvelopeTopBar("Check your letter", onBack)
            Stepper(active = if (ui.busy == Busy.READING) 0 else 1)
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 200.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                when (ui.busy) {
                    Busy.READING -> LoadingCard("Reading your letter on this phone…")
                    Busy.FINDING -> LoadingCard("Finding personal details on this phone…")
                    else -> Unit
                }
                ui.error?.let { ErrorNotice(it, ui.leaks) }
                if (ui.busy == null && ui.prepared == null && ui.error == null) {
                    // Android closed the app in the background; the letter only ever lived in memory.
                    Notice(Icons.Rounded.PhoneAndroid, "This letter was closed", "For your privacy, letters aren’t kept while Envelope is closed. Scan it again to carry on.", Tint, Deep, CardWhite, Primary)
                    EnvelopeButton("Back to start", onStartAgain)
                }
                val prepared = ui.prepared
                val display = ui.display
                if (prepared != null && display != null) {
                    QuickCheckCard(ui)
                    val hidden = prepared.outcome.redaction.tokens.size
                    if (prepared.outcome.mode == RedactionMode.NANO_AND_RULES) NanoBanner(hidden) else RulesBanner()
                    if (!prepared.outcome.entityLayerWorked) Text(
                        "Address and phone detection is still getting ready on this phone. Check addresses yourself.",
                        style = MaterialTheme.typography.bodyMedium, color = SoonInk, modifier = Modifier.padding(horizontal = 4.dp),
                    )
                    Text(
                        if (display.suggestions.isNotEmpty()) "Highlighted words look personal. Tap to hide them."
                        else "Tap any word to hide it. Tap a blue block to show it.",
                        style = MaterialTheme.typography.bodyMedium, color = InkSoft, modifier = Modifier.padding(horizontal = 4.dp),
                    )
                    RedactedLetter(display, onHideWord = onHideWord, onShowValue = onShowValue)
                }
            }
        }
        if (ui.prepared != null) {
            ActionBar(ui, onLanguage, onExplain, Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun Stepper(active: Int) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("Scan", "Check", "Explain").forEachIndexed { i, name ->
            val done = i < active
            val on = done || i == active
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(26.dp).clip(CircleShape).background(if (on) Primary else Rule), contentAlignment = Alignment.Center) {
                    if (done) Icon(Icons.Rounded.Check, null, tint = CardWhite, modifier = Modifier.size(14.dp))
                    else Text("${i + 1}", color = if (on) CardWhite else InkSoft, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = if (i == active) FontWeight.Bold else FontWeight.Normal, color = if (on) Ink else InkSoft)
            }
        }
    }
}

fun categoryIcon(category: String?): ImageVector = when (category) {
    "TAX" -> Icons.Rounded.CurrencyPound
    "COUNCIL", "HOUSING" -> Icons.Rounded.Home
    "HEALTH" -> Icons.Rounded.FavoriteBorder
    "BANK" -> Icons.Rounded.AccountBalance
    else -> Icons.Rounded.Description
}

private fun categoryName(category: String?) = when (category) {
    "TAX" -> "Tax"; "COUNCIL" -> "Council"; "HEALTH" -> "Health"; "BANK" -> "Bank"
    "HOUSING" -> "Housing"; "BENEFITS" -> "Benefits"; else -> "Letter"
}

@Composable
private fun QuickCheckCard(ui: SessionUi) {
    val qc: QuickCheck? = ui.quick
    EnvelopeCard {
        when {
            qc != null -> {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconTile(categoryIcon(qc.category), Tint, Primary)
                    Column(Modifier.weight(1f)) {
                        Text(qc.sender, style = MaterialTheme.typography.titleMedium)
                        Text(categoryName(qc.category), style = MaterialTheme.typography.bodySmall, color = InkSoft)
                    }
                    Dates.daysLeft(qc.deadline)?.let { days ->
                        val (bg, fg) = deadlineColors(days)
                        Pill(if (days < 0) "Overdue" else "$days ${if (days == 1) "day" else "days"}", bg, fg, Icons.Rounded.Schedule)
                    }
                }
                Text(qc.oneLine, style = MaterialTheme.typography.bodyLarge)
                if (ui.quickEngine == Engine.ON_DEVICE) Pill("Checked on this phone", Tint, Deep, Icons.Rounded.PhoneAndroid)
                else Pill("Checked online, hidden version", Neutral, InkBody, Icons.Rounded.Cloud)
            }
            ui.quickDeferred -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconTile(Icons.Rounded.PhoneAndroid, Tint, Primary)
                Text("Nothing has left this phone. Hide anything personal, then tap Explain.", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            }
            ui.quickPending -> Text("Quick check running…", style = MaterialTheme.typography.bodyLarge, color = InkSoft)
            else -> Text("Quick check needs Gemini Nano or an internet connection.", style = MaterialTheme.typography.bodyLarge, color = InkSoft)
        }
    }
}

@Composable
private fun NanoBanner(hidden: Int) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Deep).securityHatch(CardWhite.copy(alpha = 0.05f)).padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        IconTile(Icons.Rounded.VerifiedUser, CardWhite.copy(alpha = 0.14f), CardWhite, iconSize = 24.dp)
        Column {
            Text("$hidden ${if (hidden == 1) "detail" else "details"} hidden", color = CardWhite, style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp))
            Text("Found by Gemini Nano, on this phone", color = OnDeepSoft, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun RulesBanner() = Notice(
    Icons.Rounded.WarningAmber, "This phone can’t run Gemini Nano",
    "Numbers are hidden. Names and addresses may still show, so tap them to hide them.",
    SoonBg, SoonInk, SoonTile,
)

@Composable
private fun ErrorNotice(message: String, leaks: List<String>) = Notice(
    Icons.Rounded.ErrorOutline, message,
    if (leaks.isNotEmpty()) "Still visible: ${leaks.joinToString()}. Tap those words to hide them, then try again."
    else "Nothing was sent. Try again in a moment.",
    UrgentBg, Urgent, CardWhite,
)

@Composable
private fun ActionBar(ui: SessionUi, onLanguage: (String) -> Unit, onExplain: () -> Unit, modifier: Modifier) {
    var menu by remember { mutableStateOf(false) }
    Surface(
        modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp),
        shape = RoundedCornerShape(26.dp), color = CardWhite.copy(alpha = 0.97f), shadowElevation = 12.dp,
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Only blue blocks go online", style = MaterialTheme.typography.bodyMedium, color = InkSoft, modifier = Modifier.weight(1f))
                Box {
                    Surface(onClick = { menu = true }, shape = CircleShape, color = Neutral) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Rounded.Translate, null, modifier = Modifier.size(16.dp))
                            Text(ui.language, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        Prefs.LANGUAGES.forEach { lang ->
                            DropdownMenuItem(text = { Text(lang) }, onClick = { menu = false; onLanguage(lang) })
                        }
                    }
                }
            }
            EnvelopeButton(
                text = if (ui.busy == Busy.EXPLAINING) "Explaining…" else "Explain this letter",
                onClick = onExplain,
                icon = Icons.Rounded.AutoAwesome,
                enabled = ui.busy == null && ui.leaks.isEmpty() && !ui.notConfigured,
                loading = ui.busy == Busy.EXPLAINING,
            )
            if (ui.notConfigured) Text("Add your Firebase config to explain letters.", style = MaterialTheme.typography.bodySmall, color = InkSoft)
        }
    }
}
