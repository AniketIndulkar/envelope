package dev.novanest.envelope.ui.review

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.util.lerp
import dev.novanest.envelope.ui.components.LineCrossing
import dev.novanest.envelope.ui.theme.LineMoveEasing
import kotlin.math.roundToInt
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.CurrencyPound
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.novanest.envelope.ai.Engine
import dev.novanest.envelope.data.Prefs
import dev.novanest.envelope.privacy.RedactionMode
import dev.novanest.envelope.ui.Busy
import dev.novanest.envelope.ui.Dates
import dev.novanest.envelope.ui.LetterDisplay
import dev.novanest.envelope.ui.SessionUi
import dev.novanest.envelope.ui.components.CheckLetter
import dev.novanest.envelope.ui.components.CloudBar
import dev.novanest.envelope.ui.components.DecodeSchedule
import dev.novanest.envelope.ui.components.Dock
import dev.novanest.envelope.ui.components.LightStatusBarIcons
import dev.novanest.envelope.ui.components.LineButton
import dev.novanest.envelope.ui.components.LineHeight
import dev.novanest.envelope.ui.components.LinePill
import dev.novanest.envelope.ui.components.PillKind
import dev.novanest.envelope.ui.components.TheLine
import dev.novanest.envelope.ui.components.accentGlow
import dev.novanest.envelope.ui.components.fadeUp
import dev.novanest.envelope.ui.components.rememberStageClock
import dev.novanest.envelope.ui.theme.BigNumber
import dev.novanest.envelope.ui.theme.BodyTracking
import dev.novanest.envelope.ui.theme.DeadlineLabel
import dev.novanest.envelope.ui.theme.Ink
import dev.novanest.envelope.ui.theme.LineMoveMillis
import dev.novanest.envelope.ui.theme.LocalReducedMotion
import dev.novanest.envelope.ui.theme.Mine
import dev.novanest.envelope.ui.theme.Muted
import dev.novanest.envelope.ui.theme.OnSky
import dev.novanest.envelope.ui.theme.OnSkyMuted
import dev.novanest.envelope.ui.theme.PhoneBg
import dev.novanest.envelope.ui.theme.RiskBg
import dev.novanest.envelope.ui.theme.RiskInk
import dev.novanest.envelope.ui.theme.Sky
import dev.novanest.envelope.ui.theme.SoftSurface
import dev.novanest.envelope.ui.theme.StandIn
import dev.novanest.envelope.ui.theme.TitleTracking
import dev.novanest.envelope.ui.theme.UiFamily
import dev.novanest.envelope.ui.theme.WarnInk

/** The line on the Check stage sits at least this far below the status bar (prototype: 150). */
private val CheckLineY = 150.dp

/**
 * Check: the letter as it will cross the line. Gemini Nano (or the rules, without Nano) has swapped each personal
 * detail for a stand-in; you can un-hide any of them, or hide more words, before anything goes up.
 */
@Composable
fun ReviewScreen(
    ui: SessionUi,
    onBack: () -> Unit,
    onHideWord: (String) -> Unit,
    onShowValue: (String) -> Unit,
    onLanguage: (String) -> Unit,
    onStartAgain: () -> Unit,
    /** Start explaining; the callback fires once the answer is ready. */
    onExplain: (onReady: () -> Unit) -> Unit,
    /** Go to the Answer. Called once the answer is ready and the stand-ins have finished crossing. */
    onAnswer: () -> Unit,
) {
    LightStatusBarIcons()
    val reduced = LocalReducedMotion.current
    val density = LocalDensity.current
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val prepared = ui.prepared
    val display = ui.display
    val nano = prepared?.outcome?.mode == RedactionMode.NANO_AND_RULES
    val hidden = prepared?.outcome?.redaction?.tokens?.size ?: 0

    // The decode plays once per letter, the first time it appears; not again after rotation or un-hiding.
    var played by rememberSaveable { mutableStateOf(false) }
    val play = remember(display != null) { display != null && !played && !reduced }
    LaunchedEffect(play) { if (play) played = true }
    val schedule = remember(display?.chips?.size) { DecodeSchedule(display?.chips?.size ?: 0) }
    val clock = rememberStageClock(play, schedule.endMillis)

    // ---- Explain stage: the same screen, with the line sliding down and the stand-ins rising through it ----
    var explaining by rememberSaveable { mutableStateOf(false) }
    var answerReady by remember { mutableStateOf(false) }
    val stage = remember { Animatable(if (explaining) 1f else 0f) } // 0 = Check, 1 = Explain
    val tokens = prepared?.outcome?.redaction?.tokens.orEmpty()
    val standIns = remember(tokens) { tokens.map { LetterDisplay.label(it.token) }.distinct() }
    val realValues = remember(tokens) { tokens.map { it.value }.filter { it.isNotBlank() }.distinct() }
    val crossing = remember(standIns) { CrossingSchedule(flyingCount(standIns.size)) }
    val explainClock = rememberStageClock(explaining && !reduced, crossing.doneMillis + 200)
    var crossings by remember { mutableStateOf(emptyList<LineCrossing>()) }

    // Back from the Answer (or the app was recreated) with nothing in flight: show Check again, without animating.
    LaunchedEffect(Unit) {
        if (explaining && ui.busy != Busy.EXPLAINING) { explaining = false; stage.snapTo(0f) }
    }
    LaunchedEffect(explaining) {
        val target = if (explaining) 1f else 0f
        if (!explaining) crossings = emptyList()
        if (reduced) stage.snapTo(target) else stage.animateTo(target, tween(LineMoveMillis, easing = LineMoveEasing))
    }
    // Explaining failed: the error shows on Check, so slide back.
    LaunchedEffect(ui.busy, ui.error) {
        if (explaining && ui.busy == null && ui.error != null) explaining = false
    }
    val crossingDone = explainClock == Long.MAX_VALUE || explainClock >= crossing.doneMillis
    LaunchedEffect(explaining, answerReady, crossingDone) {
        if (explaining && answerReady && crossingDone) onAnswer()
    }
    val startExplain = {
        answerReady = false
        explaining = true
        onExplain { answerReady = true }
    }
    val crossed = if (!explaining) 0 else (0 until flyingCount(standIns.size)).count { explainClock >= crossing.crossAt(it) }
    val glow = if (explaining) 0.25f + 0.75f * (if (standIns.isEmpty()) 1f else crossed / flyingCount(standIns.size).toFloat()) else 0.3f

    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val explainPx = with(density) { (statusTop + ExplainLineY).roundToPx() }
    val checkPx = remember { intArrayOf(0) }
    var skyPx by remember { mutableIntStateOf(0) }
    Box(Modifier.fillMaxSize().background(PhoneBg)) {
        Column(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Sky)
                    .accentGlow(alpha = { glow })
                    .clipToBounds()
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))
                        if (!explaining) checkPx[0] = placeable.height
                        val h = lerp(checkPx[0].toFloat(), explainPx.toFloat(), stage.value).roundToInt()
                        layout(constraints.maxWidth, h) { placeable.place(0, 0) }
                    }
                    .onSizeChanged { skyPx = it.height }
                    .semantics { contentDescription = "The cloud, Gemini" },
            ) {
                if (explaining) ExplainCloud(standIns, explainClock, crossing, onBack) { crossings = it }
                else CheckCloud(ui, onBack, if (reduced) Modifier else Modifier.animateContentSize(tween(LineMoveMillis, easing = LineMoveEasing)))
            }
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(top = 2.dp)
                    .verticalScroll(rememberScrollState())
                    .then(if (explaining) Modifier else Modifier.padding(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 190.dp + navBottom)),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (explaining) ExplainPhone(realValues)
                else {
                    when (ui.busy) {
                        Busy.READING -> Working("Reading your letter on this phone…")
                        Busy.FINDING -> Working("Finding personal details on this phone…")
                        else -> Unit
                    }
                    ui.error?.let { ErrorCard(it, ui.leaks) }
                    if (ui.busy == null && prepared == null && ui.error == null) LetterClosed(onStartAgain)
                    if (prepared != null && display != null) {
                        Counter(lockedCount(display, schedule, clock, hidden), nano)
                        if (!nano) NoNanoBanner(ui.nanoOnPhone, display.suggestions.isNotEmpty())
                        if (!prepared.outcome.entityLayerWorked) Text(
                            "Address and phone detection is still getting ready on this phone. Check addresses yourself.",
                            color = DeadlineLabel, fontFamily = UiFamily, fontSize = 15.sp, lineHeight = 22.sp,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                        CheckLetter(display, clock, schedule, onHideWord, onShowValue)
                    }
                }
            }
        }

        // The line sits on the bottom edge of the cloud, wherever that is.
        val half = with(density) { (LineHeight / 2).roundToPx() }
        TheLine(
            Modifier.offset { IntOffset(0, skyPx - half) },
            dashed = !explaining && prepared != null && !nano,
            hot = explaining,
            crossings = crossings,
        )

        if (!explaining && prepared != null && display != null) {
            CheckDock(ui, display, hidden, onLanguage, startExplain, Modifier.align(Alignment.BottomCenter))
        }
    }
}

/** Distinct details whose stand-in has locked so far; all of them once the decode has finished. */
private fun lockedCount(display: LetterDisplay, schedule: DecodeSchedule, clock: Long, all: Int): Int {
    if (clock == Long.MAX_VALUE || clock >= schedule.endMillis) return all
    return display.chips.filterIndexed { i, _ -> clock >= schedule.lockAt(i) }.distinctBy { it.token }.size
}

// ---------- Above the line ----------

@Composable
private fun CheckCloud(ui: SessionUi, onBack: () -> Unit, modifier: Modifier) {
    val qc = ui.quick
    val wentUp = ui.quickEngine == Engine.IN_CLOUD
    Column(modifier.fillMaxWidth().statusBarsPadding().heightIn(min = CheckLineY)) {
        CloudBar(onBack) {
            Text("Check", color = OnSky, fontFamily = UiFamily, fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = TitleTracking, modifier = Modifier.weight(1f))
            LinePill("Gemini", kind = PillKind.Cloud, icon = Icons.Rounded.Cloud)
        }
        Column(
            Modifier.fadeUp().padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                if (wentUp) "Only stand-ins have gone up so far." else "Nothing has left your phone yet.",
                color = OnSkyMuted, fontFamily = UiFamily, fontSize = 17.sp, lineHeight = 23.sp,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            when {
                qc != null -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val days = Dates.daysLeft(qc.deadline)
                    val left = days?.let { if (it < 0) "Overdue" else "$it ${if (it == 1) "day" else "days"} left" }
                    Text(
                        listOfNotNull(qc.sender, left).joinToString(" · "),
                        color = OnSky, fontFamily = UiFamily, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Text(qc.oneLine, color = OnSkyMuted, fontFamily = UiFamily, fontSize = 15.sp, lineHeight = 21.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
                ui.quickPending -> Text("Quick check running…", color = OnSkyMuted, fontFamily = UiFamily, fontSize = 15.sp)
                else -> Unit
            }
        }
    }
}

// ---------- Below the line ----------

@Composable
private fun Counter(count: Int, nano: Boolean) {
    Row(
        Modifier.padding(horizontal = 4.dp).clearAndSetSemantics {
            contentDescription = "$count ${if (count == 1) "detail" else "details"} hidden${if (nano) " by Gemini Nano" else ""}. Tap a highlighted word to un-hide it."
        },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("$count", style = BigNumber.copy(fontSize = 64.sp, lineHeight = 52.sp), color = StandIn)
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                (if (count == 1) "detail hidden" else "details hidden") + if (nano) " by Gemini Nano" else "",
                color = Ink, fontFamily = UiFamily, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, letterSpacing = BodyTracking,
            )
            Text("Tap a highlighted word to un-hide it.", color = Muted, fontFamily = UiFamily, fontSize = 15.sp)
        }
    }
}

/** [nanoOnPhone]: Nano is installed but couldn't check this letter, which needs different words than "can't run". */
@Composable
private fun NoNanoBanner(nanoOnPhone: Boolean, suggestions: Boolean) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Mine)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = WarnInk, modifier = Modifier.size(22.dp))
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                if (nanoOnPhone) "Gemini Nano couldn’t check this letter" else "No Gemini Nano on this phone",
                color = WarnInk, fontFamily = UiFamily, fontSize = 16.sp, fontWeight = FontWeight.Bold,
            )
            Text(
                if (suggestions) "Numbers are hidden. Underlined words look personal: tap to hide them."
                else "Numbers are hidden. Names and addresses may still show: tap them to hide them.",
                color = WarnInk, fontFamily = UiFamily, fontSize = 15.sp, lineHeight = 22.sp,
            )
        }
    }
}

@Composable
private fun Working(text: String) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp).semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        CircularProgressIndicator(Modifier.size(22.dp), color = StandIn, strokeWidth = 2.5.dp)
        Text(text, color = Ink, fontFamily = UiFamily, fontSize = 17.sp)
    }
}

@Composable
private fun ErrorCard(message: String, leaks: List<String>) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(RiskBg)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Assertive }
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = RiskInk, modifier = Modifier.size(22.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(message, color = RiskInk, fontFamily = UiFamily, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text(
                if (leaks.isNotEmpty()) "Still visible: ${leaks.joinToString()}. Tap those words to hide them, then try again."
                else "Nothing was sent. Try again in a moment.",
                color = RiskInk, fontFamily = UiFamily, fontSize = 15.sp, lineHeight = 22.sp,
            )
        }
    }
}

/** Android closed the app in the background; the letter only ever lived in memory. */
@Composable
private fun LetterClosed(onStartAgain: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(SoftSurface).semantics(mergeDescendants = true) {}.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Rounded.PhoneAndroid, contentDescription = null, tint = Ink, modifier = Modifier.size(22.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("This letter was closed", color = Ink, fontFamily = UiFamily, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text(
                    "For your privacy, letters aren’t kept while Envelope is closed. Scan it again to carry on.",
                    color = Ink, fontFamily = UiFamily, fontSize = 15.sp, lineHeight = 22.sp,
                )
            }
        }
        LineButton("Back to start", onStartAgain, Modifier.fillMaxWidth())
    }
}

@Composable
private fun CheckDock(
    ui: SessionUi, display: LetterDisplay, hidden: Int, onLanguage: (String) -> Unit, onExplain: () -> Unit, modifier: Modifier,
) {
    val warns = display.suggestions.size
    val cross = when {
        warns > 0 -> "$warns underlined ${if (warns == 1) "word" else "words"} will go up"
        hidden == 0 -> "Nothing hidden yet"
        else -> "$hidden ${if (hidden == 1) "stand-in" else "stand-ins"} will go up"
    }
    Dock(modifier) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(cross, color = Ink, fontFamily = UiFamily, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            LanguagePicker(ui.language, onLanguage)
        }
        LineButton(
            text = if (ui.busy == Busy.EXPLAINING) "Explaining…" else "Explain this letter",
            onClick = onExplain,
            modifier = Modifier.fillMaxWidth(),
            icon = Icons.Rounded.ArrowUpward,
            enabled = ui.busy == null && ui.leaks.isEmpty() && !ui.notConfigured,
            loading = ui.busy == Busy.EXPLAINING,
        )
        if (ui.notConfigured) Text("Add your Firebase config to explain letters.", color = Muted, fontFamily = UiFamily, fontSize = 13.sp)
    }
}

@Composable
private fun LanguagePicker(language: String, onLanguage: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier
                .minimumInteractiveComponentSize()
                .heightIn(min = 36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SoftSurface)
                .clickable(role = Role.Button, onClickLabel = "Change answer language") { open = true }
                .padding(start = 9.dp, end = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Icon(Icons.Rounded.Translate, contentDescription = null, tint = Ink, modifier = Modifier.size(14.dp))
            Text(language, color = Ink, fontFamily = UiFamily, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            Prefs.LANGUAGES.forEach { lang ->
                DropdownMenuItem(text = { Text(lang, fontFamily = UiFamily) }, onClick = { open = false; onLanguage(lang) })
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

