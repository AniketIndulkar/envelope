package dev.novanest.envelope.ui.result

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.GppMaybe
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.ClipEntry
import android.content.ClipData
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import dev.novanest.envelope.ai.Explanation
import dev.novanest.envelope.privacy.TokenEntry
import dev.novanest.envelope.ui.Dates
import dev.novanest.envelope.ui.HelpLinks
import dev.novanest.envelope.ui.components.ButtonKind
import dev.novanest.envelope.ui.components.CardShape
import dev.novanest.envelope.ui.components.CountdownRing
import dev.novanest.envelope.ui.components.EnvelopeButton
import dev.novanest.envelope.ui.components.EnvelopeCard
import dev.novanest.envelope.ui.components.EnvelopeTopBar
import dev.novanest.envelope.ui.components.HeroCard
import dev.novanest.envelope.ui.components.IconTile
import dev.novanest.envelope.ui.components.Notice
import dev.novanest.envelope.ui.components.PaperSheet
import dev.novanest.envelope.ui.components.Pill
import dev.novanest.envelope.ui.components.SectionTitle
import dev.novanest.envelope.ui.components.SquareIconButton
import dev.novanest.envelope.ui.components.securityHatch
import dev.novanest.envelope.ui.theme.CardWhite
import dev.novanest.envelope.ui.theme.Deep
import dev.novanest.envelope.ui.theme.InkBody
import dev.novanest.envelope.ui.theme.InkSoft
import dev.novanest.envelope.ui.theme.LetterBody
import dev.novanest.envelope.ui.theme.Neutral
import dev.novanest.envelope.ui.theme.OnDeepSoft
import dev.novanest.envelope.ui.theme.Primary
import dev.novanest.envelope.ui.theme.SoonBg
import dev.novanest.envelope.ui.theme.SoonInk
import dev.novanest.envelope.ui.theme.SoonTile
import dev.novanest.envelope.ui.theme.Tint
import dev.novanest.envelope.ui.theme.Urgent
import dev.novanest.envelope.ui.theme.UrgentBg

/** Everything the Result screen shows; also used to reopen a letter from history. */
data class ResultModel(
    val sender: String,
    val explanation: Explanation,
    val tokens: List<TokenEntry>,
    val unknownTokens: List<String>,
    val hiddenCount: Int,
)

@Composable
fun ResultScreen(model: ResultModel, onBack: () -> Unit, onReceipt: (() -> Unit)?) {
    val context = LocalContext.current
    val e = model.explanation
    val share = { context.startActivity(shareIntent(e.draftReply)) }
    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())) {
        EnvelopeTopBar("What it means", onBack) { SquareIconButton(Icons.Rounded.Share, "Share reply", share) }
        Column(
            Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 32.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (e.redFlags.isNotEmpty()) ScamWarning(e.redFlags)
            Hero(model)
            if (e.actions.isNotEmpty()) Todo(e)
            if (e.consequences.isNotBlank()) Notice(Icons.Rounded.WarningAmber, "If you do nothing", e.consequences, SoonBg, SoonInk, SoonTile)
            if (e.draftReply.isNotBlank()) Reply(model, onCopyShare = share)
            if (e.helpLinks.any { HelpLinks.isTrusted(it.url) }) Help(e)
            if (model.unknownTokens.isNotEmpty()) Text(
                "Some details in the answer couldn’t be filled in, so they’re shown as placeholders.",
                style = MaterialTheme.typography.bodySmall, color = InkSoft, modifier = Modifier.padding(horizontal = 4.dp),
            )
            onReceipt?.let { ReceiptLink(model.hiddenCount, it) }
            Text(
                "Envelope explains letters in plain English. It isn’t legal or financial advice.",
                style = MaterialTheme.typography.bodySmall, color = InkSoft, modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}

@Composable
private fun ScamWarning(flags: List<String>) = Notice(
    Icons.Rounded.GppMaybe, "This letter may be a scam",
    flags.joinToString("\n") { "• $it" } + "\nDon’t call numbers or pay using details from this letter. Contact the organisation using details from its official website.",
    UrgentBg, Urgent, CardWhite,
)

@Composable
private fun Hero(model: ResultModel) {
    val context = LocalContext.current
    val e = model.explanation
    val days = Dates.daysLeft(e.deadline)
    HeroCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            days?.let { CountdownRing(it) }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(model.sender, style = MaterialTheme.typography.bodySmall, color = OnDeepSoft)
                Text(e.actions.firstOrNull { it.by != null }?.let { a -> headline(a.step, a.by) } ?: e.summary.substringBefore(". "),
                    style = MaterialTheme.typography.headlineSmall, color = CardWhite)
            }
        }
        Text(e.whatItMeans.ifBlank { e.summary }, style = MaterialTheme.typography.bodyLarge, color = Tint)
        Dates.parse(e.deadline)?.let { date ->
            EnvelopeButton("Add to calendar", {
                try { context.startActivity(calendarIntent("${model.sender}: deadline", e.summary, date)) } catch (_: ActivityNotFoundException) {}
            }, kind = ButtonKind.Light, icon = Icons.Rounded.CalendarMonth, height = 50.dp)
        }
    }
}

private fun headline(step: String, by: String?): String {
    val date = Dates.pretty(by) ?: return step
    val short = step.substringBefore(" using ").substringBefore(" on ").substringBefore(" at ").trimEnd('.')
    return if (short.length <= 36) "$short by $date" else "Act by $date"
}

@Composable
private fun Todo(e: Explanation) {
    val done = remember { mutableStateMapOf<Int, Boolean>() }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle("What to do", Modifier.padding(top = 8.dp))
        e.actions.forEachIndexed { i, a ->
            Surface(shape = CardShape, color = CardWhite, shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                Row(
                    Modifier.clickable(role = Role.Checkbox) { done[i] = !(done[i] ?: false) }.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Checkbox(checked = done[i] ?: false, onCheckedChange = null, colors = CheckboxDefaults.colors(checkedColor = Primary), modifier = Modifier.size(26.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(a.step, style = MaterialTheme.typography.bodyLarge)
                        Pill(Dates.pretty(a.by)?.let { "By $it" } ?: "As soon as you can", Neutral, InkBody, Icons.Rounded.Schedule)
                    }
                }
            }
        }
    }
}

@Composable
private fun Reply(model: ResultModel, onCopyShare: () -> Unit) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val reply = model.explanation.draftReply
    val marked = remember(reply, model.tokens) { markReal(reply, model.tokens) }
    EnvelopeCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Reply to send", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            Pill("Details added on phone", Tint, Deep, Icons.Rounded.PhoneAndroid)
        }
        PaperSheet(flat = true) { Text(marked, style = LetterBody) }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            EnvelopeButton("Copy", { scope.launch { clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("Reply", reply))) } }, Modifier.weight(1f), ButtonKind.Outline, Icons.Rounded.ContentCopy, 48.dp)
            EnvelopeButton("Share", onCopyShare, Modifier.weight(1f), ButtonKind.Outline, Icons.Rounded.Share, 48.dp)
        }
    }
}

/** Highlights the real details that were filled back in on the phone. */
private fun markReal(text: String, tokens: List<TokenEntry>) = buildAnnotatedString {
    append(text)
    tokens.map { it.value }.filter { it.isNotBlank() }.forEach { value ->
        var i = text.indexOf(value)
        while (i >= 0) { addStyle(SpanStyle(background = Tint), i, i + value.length); i = text.indexOf(value, i + value.length) }
    }
}

@Composable
private fun Help(e: Explanation) {
    val context = LocalContext.current
    Surface(shape = CardShape, color = CardWhite, shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 6.dp)) {
            e.helpLinks.filter { HelpLinks.isTrusted(it.url) }.forEachIndexed { i, link ->
                if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                Row(
                    Modifier.fillMaxWidth().clickable(role = Role.Button) {
                        try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link.url))) } catch (_: ActivityNotFoundException) {}
                    }.padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(link.label, style = MaterialTheme.typography.titleMedium, color = Primary, modifier = Modifier.weight(1f))
                    Icon(Icons.AutoMirrored.Rounded.OpenInNew, null, tint = Primary, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun ReceiptLink(hidden: Int, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Primary).securityHatch(CardWhite.copy(alpha = 0.08f))
            .clickable(role = Role.Button, onClick = onClick).padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        IconTile(Icons.Rounded.VerifiedUser, CardWhite.copy(alpha = 0.16f), CardWhite, iconSize = 24.dp)
        Column(Modifier.weight(1f)) {
            Text("See what left your phone", style = MaterialTheme.typography.titleMedium.copy(fontSize = MaterialTheme.typography.labelLarge.fontSize), color = CardWhite)
            Text("$hidden ${if (hidden == 1) "detail" else "details"} stayed here", style = MaterialTheme.typography.bodySmall, color = OnDeepSoft)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = CardWhite)
    }
}
