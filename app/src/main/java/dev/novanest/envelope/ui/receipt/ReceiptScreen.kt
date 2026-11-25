package dev.novanest.envelope.ui.receipt

import dev.novanest.envelope.ui.components.DarkStatusBarIcons
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.novanest.envelope.ai.Engine
import dev.novanest.envelope.pipeline.Step
import dev.novanest.envelope.pipeline.StepRecord
import dev.novanest.envelope.privacy.PiiType
import dev.novanest.envelope.privacy.Redaction
import dev.novanest.envelope.ui.LetterDisplay
import dev.novanest.envelope.ui.components.EnvelopeCard
import dev.novanest.envelope.ui.components.EnvelopeTopBar
import dev.novanest.envelope.ui.components.HeroCard
import dev.novanest.envelope.ui.components.IconTile
import dev.novanest.envelope.ui.components.Notice
import dev.novanest.envelope.ui.components.OriginalLetter
import dev.novanest.envelope.ui.components.Pill
import dev.novanest.envelope.ui.components.RedactedLetter
import dev.novanest.envelope.ui.theme.CardWhite
import dev.novanest.envelope.ui.theme.Deep
import dev.novanest.envelope.ui.theme.Ink
import dev.novanest.envelope.ui.theme.InkSoft
import dev.novanest.envelope.ui.theme.Neutral
import dev.novanest.envelope.ui.theme.OnDeepSoft
import dev.novanest.envelope.ui.theme.Primary
import dev.novanest.envelope.ui.theme.Tint

@Composable
fun ReceiptScreen(redaction: Redaction, steps: List<StepRecord>, onBack: () -> Unit) {
    DarkStatusBarIcons()
    var sentView by rememberSaveable { mutableStateOf(true) }
    val hidden = redaction.tokens.size
    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())) {
        EnvelopeTopBar("Privacy receipt", onBack)
        Column(
            Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 32.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Hero(hidden, redaction.countsByType())
            EnvelopeCard(padding = 16.dp) {
                Toggle(sentView) { sentView = it }
                if (sentView) RedactedLetter(LetterDisplay.of(redaction), flat = true)
                else OriginalLetter(redaction, flat = true)
            }
            EnvelopeCard(padding = 18.dp) {
                Text("Where each step ran", style = MaterialTheme.typography.titleLarge)
                Timeline(steps)
            }
            Notice(
                Icons.Rounded.VerifiedUser, null,
                "Your history online keeps only the hidden version. Your real details stay on this phone and are never backed up.",
                Tint, Deep, CardWhite, Primary,
            )
        }
    }
}

private val plural = mapOf(
    PiiType.NAME to ("name" to "names"), PiiType.ADDRESS to ("address" to "addresses"),
    PiiType.POSTCODE to ("postcode" to "postcodes"), PiiType.NI_NUMBER to ("NI number" to "NI numbers"),
    PiiType.ACCOUNT to ("bank detail" to "bank details"), PiiType.REFERENCE to ("reference" to "references"),
    PiiType.PHONE to ("phone number" to "phone numbers"), PiiType.EMAIL to ("email" to "emails"),
    PiiType.DOB to ("date of birth" to "dates of birth"),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Hero(hidden: Int, counts: Map<PiiType, Int>) {
    HeroCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("$hidden", style = MaterialTheme.typography.displayLarge, color = CardWhite)
            Text(
                if (hidden == 1) "personal detail never left this phone" else "personal details never left this phone",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = CardWhite,
            )
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            counts.entries.sortedBy { it.key.ordinal }.forEach { (type, n) ->
                val (one, many) = plural.getValue(type)
                Pill("$n ${if (n == 1) one else many}", CardWhite.copy(alpha = 0.14f), CardWhite)
            }
        }
        Text("Dates, amounts and who sent the letter were kept, so the explanation is right.", style = MaterialTheme.typography.bodyMedium, color = OnDeepSoft)
    }
}

@Composable
private fun Toggle(sent: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Neutral).padding(4.dp).selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Segment("On your phone", Icons.Rounded.PhoneAndroid, !sent, Modifier.weight(1f)) { onChange(false) }
        Segment("Sent online", Icons.Rounded.Cloud, sent, Modifier.weight(1f)) { onChange(true) }
    }
}

@Composable
private fun Segment(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        modifier.height(46.dp).selectable(selected, role = Role.Tab, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) CardWhite else Neutral,
        shadowElevation = if (selected) 2.dp else 0.dp,
    ) {
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = if (selected) Ink else InkSoft, modifier = Modifier.size(16.dp))
            Text("  $label", style = MaterialTheme.typography.labelMedium, color = if (selected) Ink else InkSoft)
        }
    }
}

private fun stepTitle(step: Step) = when (step) {
    Step.READ -> "Read the letter"
    Step.FIND_DETAILS -> "Find personal details"
    Step.QUICK_CHECK -> "Quick check"
    Step.EXPLAIN -> "Explain and draft a reply"
}

private fun where(r: StepRecord) = when (r.engine) {
    Engine.IN_CLOUD -> "Online, hidden version only"
    Engine.RULES -> "On this phone, rules only"
    Engine.ON_DEVICE -> if (r.step == Step.READ) "On this phone" else "On this phone, Gemini Nano"
}

@Composable
private fun Timeline(steps: List<StepRecord>) {
    val all = steps + if (steps.any { it.step == Step.EXPLAIN }) listOf(null) else emptyList() // null = "put details back"
    Column {
        all.forEachIndexed { i, r ->
            val online = r?.engine == Engine.IN_CLOUD
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconTile(
                        if (online) Icons.Rounded.Cloud else Icons.Rounded.PhoneAndroid,
                        if (online) Ink else Tint, if (online) CardWhite else Primary, iconSize = 20.dp,
                    )
                    if (i < all.lastIndex) Box(Modifier.width(2.dp).fillMaxHeight().background(Tint))
                }
                Column(Modifier.padding(top = 2.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("${i + 1}. ${r?.let { stepTitle(it.step) } ?: "Put your details back"}", style = MaterialTheme.typography.titleMedium)
                    Text(r?.let(::where) ?: "On this phone", style = MaterialTheme.typography.bodyMedium, color = if (online) Ink else Primary)
                }
            }
        }
    }
}
