package dev.novanest.envelope.ui.receipt

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.novanest.envelope.ai.Engine
import dev.novanest.envelope.pipeline.Step
import dev.novanest.envelope.pipeline.StepRecord
import dev.novanest.envelope.privacy.Redaction
import dev.novanest.envelope.ui.LetterDisplay
import dev.novanest.envelope.ui.components.CheckLetter
import dev.novanest.envelope.ui.components.DarkStatusBarIcons
import dev.novanest.envelope.ui.components.DecodeSchedule
import dev.novanest.envelope.ui.components.EnvelopeTopBar
import dev.novanest.envelope.ui.components.MarkedLetterText
import dev.novanest.envelope.ui.components.ReceiptPass
import dev.novanest.envelope.ui.home.WalletLetter
import dev.novanest.envelope.ui.theme.Hairline1
import dev.novanest.envelope.ui.theme.Ink
import dev.novanest.envelope.ui.theme.LetterBody
import dev.novanest.envelope.ui.theme.Muted
import dev.novanest.envelope.ui.theme.OnSky
import dev.novanest.envelope.ui.theme.PhoneBg
import dev.novanest.envelope.ui.theme.Rail
import dev.novanest.envelope.ui.theme.Sky
import dev.novanest.envelope.ui.theme.SoftSurface
import dev.novanest.envelope.ui.theme.StandIn
import dev.novanest.envelope.ui.theme.StandInSoft
import dev.novanest.envelope.ui.theme.Surface
import dev.novanest.envelope.ui.theme.TitleTracking
import dev.novanest.envelope.ui.theme.UiFamily

/**
 * Privacy receipt for the letter just explained: the same pass as the back of a wallet letter, then the letter
 * itself (as sent, or as it is on this phone) and where each step ran.
 */
@Composable
fun ReceiptScreen(redaction: Redaction, steps: List<StepRecord>, onBack: () -> Unit) {
    DarkStatusBarIcons()
    var sentView by rememberSaveable { mutableStateOf(true) }
    val tokens = remember(redaction) { redaction.tokens.map { it.token }.distinct() }
    val standIns = remember(tokens) { tokens.map { LetterDisplay.label(it) } }
    val ledger = remember(tokens) { WalletLetter.ledgerOf(tokens) }
    val display = remember(redaction) { LetterDisplay.of(redaction) }
    val originalMarks = remember(redaction) { redaction.placements.map { it.originalStart until it.originalEnd } }

    Column(Modifier.fillMaxSize().background(PhoneBg).statusBarsPadding().verticalScroll(rememberScrollState())) {
        EnvelopeTopBar("Privacy receipt", onBack)
        Column(
            Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 32.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            ReceiptPass(standIns, ledger)
            Text(
                "Dates, amounts and who sent the letter were kept, so the explanation is right.",
                color = Muted, fontFamily = UiFamily, fontSize = 15.sp, lineHeight = 22.sp, modifier = Modifier.padding(horizontal = 4.dp),
            )

            Heading("The letter")
            Toggle(sentView) { sentView = it }
            if (sentView) CheckLetter(
                display, clock = Long.MAX_VALUE, schedule = DecodeSchedule(display.chips.size),
                onHideWord = {}, onShowValue = {}, interactive = false, description = "The letter as it went to Gemini",
            ) else Paper { MarkedLetterText(redaction.original, originalMarks, style = LetterBody.copy(lineHeight = 30.sp)) }

            Heading("Where each step ran")
            StepRail(steps)

            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(SoftSurface).semantics(mergeDescendants = true) {}
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Rounded.PhoneAndroid, contentDescription = null, tint = Ink, modifier = Modifier.size(20.dp))
                Text(
                    "Your history online keeps only the hidden version. Your real details stay on this phone and are never backed up.",
                    color = Ink, fontFamily = UiFamily, fontSize = 15.sp, lineHeight = 22.sp,
                )
            }
        }
    }
}

@Composable
private fun Heading(text: String) = Text(
    text, color = Ink, fontFamily = UiFamily, fontSize = 19.sp, fontWeight = FontWeight.Bold, letterSpacing = TitleTracking,
    modifier = Modifier.padding(horizontal = 4.dp, vertical = 0.dp).semantics { heading() },
)

@Composable
private fun Paper(content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        Modifier.fillMaxWidth().shadow(12.dp, shape, ambientColor = Ink, spotColor = Ink).clip(shape).background(Surface)
            .border(1.dp, Hairline1, shape).padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 24.dp),
    ) { content() }
}

@Composable
private fun Toggle(sent: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(SoftSurface).padding(4.dp).selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Segment("Sent to Gemini", Icons.Rounded.Cloud, sent, Modifier.weight(1f)) { onChange(true) }
        Segment("On this phone", Icons.Rounded.PhoneAndroid, !sent, Modifier.weight(1f)) { onChange(false) }
    }
}

@Composable
private fun Segment(label: String, icon: ImageVector, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier
            .heightIn(min = 48.dp)
            .clip(shape)
            .background(if (selected) Surface else SoftSurface)
            .then(if (selected) Modifier.border(1.dp, Hairline1, shape) else Modifier)
            .selectable(selected, role = Role.Tab, onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = if (selected) Ink else Muted, modifier = Modifier.size(16.dp))
        Text(label, color = if (selected) Ink else Muted, fontFamily = UiFamily, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun stepTitle(step: Step) = when (step) {
    Step.READ -> "Read the letter"
    Step.FIND_DETAILS -> "Find personal details"
    Step.QUICK_CHECK -> "Quick check"
    Step.EXPLAIN -> "Explain and draft a reply"
}

private fun where(r: StepRecord) = when (r.engine) {
    Engine.IN_CLOUD -> "Gemini, stand-ins only"
    Engine.RULES -> "This phone, rules only"
    Engine.ON_DEVICE -> if (r.step == Step.READ) "This phone" else "This phone, Gemini Nano"
}

/** Each step on a rail: plum cloud dot for Gemini, soft plum phone dot for this phone. */
@Composable
private fun StepRail(steps: List<StepRecord>) {
    val all = steps + if (steps.any { it.step == Step.EXPLAIN }) listOf(null) else emptyList() // null = "put details back"
    Column {
        all.forEachIndexed { i, r ->
            val cloud = r?.engine == Engine.IN_CLOUD
            Row(
                Modifier.fillMaxWidth().height(IntrinsicSize.Min).semantics(mergeDescendants = true) {},
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(Modifier.width(32.dp).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.size(32.dp).clip(CircleShape).background(if (cloud) Sky else StandInSoft),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            if (cloud) Icons.Rounded.Cloud else Icons.Rounded.PhoneAndroid, contentDescription = null,
                            tint = if (cloud) OnSky else StandIn, modifier = Modifier.size(16.dp),
                        )
                    }
                    if (i < all.lastIndex) Box(Modifier.width(2.dp).weight(1f).background(Rail))
                }
                Column(Modifier.padding(top = 5.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(r?.let { stepTitle(it.step) } ?: "Put your details back", color = Ink, fontFamily = UiFamily, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text(r?.let(::where) ?: "This phone", color = if (cloud) Ink else StandIn, fontFamily = UiFamily, fontSize = 15.sp)
                }
            }
        }
    }
}
