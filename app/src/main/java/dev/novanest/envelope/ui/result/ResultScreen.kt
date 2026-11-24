package dev.novanest.envelope.ui.result

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.GppMaybe
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.novanest.envelope.ai.Action
import dev.novanest.envelope.ai.Explanation
import dev.novanest.envelope.privacy.TokenEntry
import dev.novanest.envelope.ui.Dates
import dev.novanest.envelope.ui.HelpLinks
import dev.novanest.envelope.ui.LetterDisplay
import dev.novanest.envelope.ui.components.DaysNumber
import dev.novanest.envelope.ui.components.DecodeToReal
import dev.novanest.envelope.ui.components.Dock
import dev.novanest.envelope.ui.components.LineButton
import dev.novanest.envelope.ui.components.LineButtonKind
import dev.novanest.envelope.ui.components.LightStatusBarIcons
import dev.novanest.envelope.ui.components.LineHeight
import dev.novanest.envelope.ui.components.LinePill
import dev.novanest.envelope.ui.components.MarkedLetterText
import dev.novanest.envelope.ui.components.PillKind
import dev.novanest.envelope.ui.components.TheLine
import dev.novanest.envelope.ui.components.TimelineRisk
import dev.novanest.envelope.ui.components.TimelineStep
import dev.novanest.envelope.ui.components.TimelineToday
import dev.novanest.envelope.ui.components.accentGlow
import dev.novanest.envelope.ui.components.realDetailRanges
import dev.novanest.envelope.ui.components.warmColor
import dev.novanest.envelope.ui.theme.BodyTracking
import dev.novanest.envelope.ui.theme.Hairline1
import dev.novanest.envelope.ui.theme.Hairline2
import dev.novanest.envelope.ui.theme.Ink
import dev.novanest.envelope.ui.theme.InkPressed
import dev.novanest.envelope.ui.theme.LetterBody
import dev.novanest.envelope.ui.theme.LocalReducedMotion
import dev.novanest.envelope.ui.theme.Muted
import dev.novanest.envelope.ui.theme.OnSky
import dev.novanest.envelope.ui.theme.OnSkyMuted
import dev.novanest.envelope.ui.theme.PhoneBg
import dev.novanest.envelope.ui.theme.RiskBg
import dev.novanest.envelope.ui.theme.RiskInk
import dev.novanest.envelope.ui.theme.Sky
import dev.novanest.envelope.ui.theme.StandIn
import dev.novanest.envelope.ui.theme.Surface
import dev.novanest.envelope.ui.theme.TitleTracking
import dev.novanest.envelope.ui.theme.UiFamily
import dev.novanest.envelope.ui.theme.UnitText
import dev.novanest.envelope.ui.theme.Typography
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.roundToInt

/** Everything the Result screen shows; also used to reopen a letter from history. */
data class ResultModel(
    val sender: String,
    val explanation: Explanation,
    val tokens: List<TokenEntry>,
    val unknownTokens: List<String>,
    val hiddenCount: Int,
)

/** Where the line sits on the Answer stage, measured from below the status bar (prototype: 300). */
private val AnswerLineY = 300.dp

/** How many put-back details to show before "+ N more". */
private const val PutBackShown = 4

/**
 * Answer: the cloud above the line holds the sender and the days left; below, on this phone,
 * the stand-ins turn back into your real details, then the summary, the dated "What to do" timeline,
 * the reply letter and the help links.
 *
 * [onDone]/[doneLabel] drive the dock: "Saved to my letters" after a fresh explain, "Done" from history.
 */
@Composable
fun ResultScreen(
    model: ResultModel,
    onBack: () -> Unit,
    onReceipt: (() -> Unit)?,
    onDone: () -> Unit = onBack,
    doneLabel: String = "Done",
) {
    val context = LocalContext.current
    val e = model.explanation
    // A suspected scam's "deadline" is a pressure tactic: never count it down or put it in the calendar.
    val scam = e.redFlags.isNotEmpty()
    val days = if (scam) null else Dates.daysLeft(e.deadline)
    val share: (() -> Unit)? = if (e.draftReply.isBlank()) null else ({ context.startActivity(shareIntent(e.draftReply)) })

    LightStatusBarIcons()
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val skyHeight = statusTop + AnswerLineY

    Box(Modifier.fillMaxSize().background(PhoneBg)) {
        // This phone: everything below the line scrolls under the cloud.
        Column(
            Modifier
                .fillMaxSize()
                .padding(top = skyHeight + 2.dp)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 120.dp + navBottom),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            if (scam) ScamWarning(e.redFlags)
            if (model.tokens.isNotEmpty()) PutBack(model.tokens)
            Summary(e)
            if (!scam) Dates.parse(e.deadline)?.let { date ->
                LineButton(
                    "Add ${Dates.pretty(e.deadline)} to calendar",
                    { try { context.startActivity(calendarIntent("${model.sender}: deadline", e.summary, date)) } catch (_: ActivityNotFoundException) {} },
                    Modifier.fillMaxWidth(), LineButtonKind.Secondary, Icons.Rounded.CalendarMonth,
                )
            }
            if (e.actions.isNotEmpty() || e.consequences.isNotBlank()) WhatToDo(e)
            if (e.draftReply.isNotBlank()) Reply(model, share ?: {})
            onReceipt?.let { ReceiptRow(model.hiddenCount, it) }
            if (e.helpLinks.any { HelpLinks.isTrusted(it.url) }) Help(e)
            if (model.unknownTokens.isNotEmpty()) SmallPrint("Some details in the answer couldn’t be filled in, so they’re shown as placeholders.")
            SmallPrint("Envelope explains letters in plain English. It isn’t legal or financial advice.")
        }

        // The cloud: Gemini's side of the line.
        Box(
            Modifier
                .fillMaxWidth()
                .height(skyHeight)
                .background(Sky)
                .accentGlow(alpha = { 0.55f })
                .semantics { contentDescription = "The cloud, Gemini" }
                .statusBarsPadding(),
        ) {
            AnswerCloud(model.sender, days, e, scam, onBack, share)
        }

        TheLine(Modifier.offset(y = skyHeight - LineHeight / 2))

        Dock(Modifier.align(Alignment.BottomCenter)) {
            LineButton(doneLabel, onDone, Modifier.fillMaxWidth())
        }
    }
}

// ---------- Above the line ----------

private val DropEasing = CubicBezierEasing(0.34f, 1.3f, 0.5f, 1f)
private val CountEasing = CubicBezierEasing(0.33f, 1f, 0.68f, 1f) // ease-out cubic

@Composable
private fun AnswerCloud(sender: String, days: Int?, e: Explanation, scam: Boolean, onBack: () -> Unit, share: (() -> Unit)?) {
    val reduced = LocalReducedMotion.current
    val drop = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (reduced) return@LaunchedEffect
        delay(300)
        drop.animateTo(1f, tween(900, easing = DropEasing))
    }
    Column {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Rounded.Close, contentDescription = "Close", tint = OnSky) }
            Text(
                sender, color = OnSkyMuted, fontFamily = UiFamily, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(start = 4.dp),
            )
            share?.let { IconButton(onClick = it) { Icon(Icons.Rounded.Share, contentDescription = "Share reply", tint = OnSky) } }
        }
        Column(
            Modifier
                .graphicsLayer {
                    alpha = drop.value.coerceIn(0f, 1f)
                    translationY = (1f - drop.value) * -36.dp.toPx()
                }
                .padding(start = 20.dp, end = 20.dp, top = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (days != null) Countdown(days, Dates.pretty(e.deadline))
            else Text(
                if (scam) "Check before you do anything" else e.summary.substringBefore(". ").trimEnd('.'),
                color = OnSky, style = Typography.displayMedium, maxLines = 4, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() },
            )
        }
    }
}

/** Days left counting down from 30 to the real value, warming as it drops. Overdue letters count overdue days. */
@Composable
private fun Countdown(days: Int, duePretty: String?) {
    val reduced = LocalReducedMotion.current
    val animate = !reduced && days in 0 until 30
    val shown = remember(days) { Animatable(if (animate) 30f else days.toFloat()) }
    LaunchedEffect(days, animate) {
        if (!animate) return@LaunchedEffect
        delay(500)
        shown.animateTo(days.toFloat(), tween(1300, easing = CountEasing))
    }
    val n = shown.value.roundToInt()
    val overdue = days < 0
    val one = abs(days) == 1
    val unit = if (overdue) (if (one) "day\noverdue" else "days\noverdue") else (if (one) "day\nleft" else "days\nleft")
    val due = duePretty?.let { if (overdue) "Was due $it" else "Due $it" }
    Column(
        Modifier.clearAndSetSemantics {
            contentDescription = buildString {
                append(if (overdue) "${abs(days)} ${if (one) "day" else "days"} overdue" else "$days ${if (one) "day" else "days"} left")
                due?.let { append(". $it") }
            }
        },
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            DaysNumber(abs(n), color = warmColor(n))
            Text(unit, color = OnSky, style = UnitText.copy(fontSize = 22.sp, lineHeight = 24.sp), modifier = Modifier.padding(bottom = 6.dp))
        }
        due?.let { Text(it, color = OnSkyMuted, fontFamily = UiFamily, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
    }
}

// ---------- Below the line ----------

@Composable
private fun ScamWarning(flags: List<String>) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(RiskBg).semantics(mergeDescendants = true) {}.padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Rounded.GppMaybe, contentDescription = null, tint = RiskInk, modifier = Modifier.size(22.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("This letter may be a scam", color = RiskInk, fontFamily = UiFamily, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text(
                flags.joinToString("\n") { "• $it" } +
                    "\nDon’t call numbers or pay using details from this letter. Contact the organisation using details from its official website.",
                color = RiskInk, fontFamily = UiFamily, fontSize = 15.sp, lineHeight = 22.sp,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PutBack(tokens: List<TokenEntry>) {
    val unique = remember(tokens) { tokens.filter { it.value.isNotBlank() }.distinctBy { it.value } }
    if (unique.isEmpty()) return
    Column(Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LinePill("Put back on this phone only", kind = PillKind.Mine, icon = Icons.Rounded.PhoneAndroid)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            unique.take(PutBackShown).forEachIndexed { i, t ->
                DecodeToReal(LetterDisplay.label(t.token), oneLine(t.value), delayMillis = 900L + i * 420L)
            }
            if (unique.size > PutBackShown) Text(
                "+ ${unique.size - PutBackShown} more", color = Muted, fontFamily = UiFamily, fontSize = 15.sp, lineHeight = 30.sp,
            )
        }
    }
}

/** Addresses come in on several lines; as a chip they read better on one. */
private fun oneLine(value: String) = value.lines().map { it.trim() }.filter { it.isNotEmpty() }.joinToString(", ")

@Composable
private fun Summary(e: Explanation) {
    Column(Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(e.summary, color = Ink, fontFamily = UiFamily, fontSize = 19.sp, lineHeight = 28.sp, letterSpacing = BodyTracking)
        if (e.whatItMeans.isNotBlank() && e.whatItMeans != e.summary) {
            Text(e.whatItMeans, color = InkPressed, fontFamily = UiFamily, fontSize = 16.sp, lineHeight = 24.sp)
        }
    }
}

@Composable
private fun SectionHeading(text: String, modifier: Modifier = Modifier) {
    Text(
        text, color = Ink, fontFamily = UiFamily, fontSize = 19.sp, fontWeight = FontWeight.Bold, letterSpacing = TitleTracking,
        modifier = modifier.semantics { heading() },
    )
}

@Composable
private fun WhatToDo(e: Explanation) {
    // Screen-only for now: survives rotation, resets when you leave the letter.
    var done by rememberSaveable { mutableStateOf(listOf<Int>()) }
    val deadlineStep = remember(e) { deadlineStepIndex(e.actions, e.deadline) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 4.dp), verticalAlignment = Alignment.Bottom) {
            SectionHeading("What to do", Modifier.weight(1f))
            if (e.actions.isNotEmpty()) Text(
                "${done.size} of ${e.actions.size} done", color = Muted, fontFamily = UiFamily, fontSize = 14.sp, fontWeight = FontWeight.Medium,
            )
        }
        Column {
            TimelineToday("Today, ${Dates.pretty(LocalDate.now().toString())}")
            e.actions.forEachIndexed { i, a ->
                TimelineStep(
                    whenLabel = Dates.pretty(a.by)?.let { "By $it" } ?: "Now",
                    title = a.step,
                    note = null,
                    done = i in done,
                    onDoneChange = { on -> done = if (on) done + i else done - i },
                    deadline = i == deadlineStep,
                )
            }
            if (e.consequences.isNotBlank()) {
                val first = e.consequences.substringBefore(". ").trimEnd('.')
                val rest = e.consequences.substringAfter(". ", "").trim()
                TimelineRisk("If you do nothing", first, rest.ifBlank { null })
            }
        }
    }
}

/**
 * Which step carries the letter's deadline, so the timeline can give it the accent dot and outline.
 * The first step due on the letter's deadline date wins. No deadline, or no step on that date: no highlight,
 * rather than guessing, so peach only ever means "this is the date that matters".
 */
internal fun deadlineStepIndex(actions: List<Action>, deadline: String?): Int? {
    val date = Dates.parse(deadline) ?: return null
    return actions.indexOfFirst { Dates.parse(it.by) == date }.takeIf { it >= 0 }
}

@Composable
private fun Sheet(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Column(modifier.fillMaxWidth().clip(shape).background(Surface).border(1.dp, Hairline1, shape), content = content)
}

private fun Modifier.hairlineTop() = drawBehind { drawRect(Hairline2, size = size.copy(height = 1.dp.toPx())) }

@Composable
private fun Reply(model: ResultModel, onShare: () -> Unit) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val reply = model.explanation.draftReply
    val marks = remember(reply, model.tokens) { realDetailRanges(reply, model.tokens) }
    Sheet {
        Row(
            Modifier
                .fillMaxWidth()
                .drawBehind { drawRect(Hairline2, Offset(0f, size.height - 1.dp.toPx()), size.copy(height = 1.dp.toPx())) }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SectionHeading("Reply to send", Modifier.weight(1f))
            if (marks.isNotEmpty()) LinePill("Gold = added on this phone", kind = PillKind.Mine)
        }
        MarkedLetterText(
            reply, marks,
            Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 6.dp),
            style = LetterBody.copy(lineHeight = 28.sp),
        )
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LineButton(
                "Copy", { scope.launch { clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("Reply", reply))) } },
                Modifier.weight(1f), LineButtonKind.Primary, Icons.Rounded.ContentCopy, minHeight = 46.dp,
            )
            LineButton("Share", onShare, Modifier.weight(1f), LineButtonKind.Secondary, Icons.Rounded.Share, minHeight = 46.dp)
        }
    }
}

@Composable
private fun ReceiptRow(hidden: Int, onClick: () -> Unit) {
    Sheet {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("See what left your phone", color = Ink, fontFamily = UiFamily, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "$hidden ${if (hidden == 1) "detail" else "details"} stayed on this phone",
                    color = Muted, fontFamily = UiFamily, fontSize = 14.sp,
                )
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = Ink)
        }
    }
}

@Composable
private fun Help(e: Explanation) {
    val context = LocalContext.current
    Sheet {
        e.helpLinks.filter { HelpLinks.isTrusted(it.url) }.forEachIndexed { i, link ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .then(if (i > 0) Modifier.hairlineTop() else Modifier)
                    .heightIn(min = 52.dp)
                    .clickable(role = Role.Button) {
                        try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link.url))) } catch (_: ActivityNotFoundException) {}
                    }
                    .padding(horizontal = 16.dp, vertical = 15.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(link.label, color = StandIn, fontFamily = UiFamily, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = "Opens in browser", tint = StandIn, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun SmallPrint(text: String) {
    Text(text, color = Muted, fontFamily = UiFamily, fontSize = 13.sp, lineHeight = 19.sp, modifier = Modifier.padding(horizontal = 4.dp))
}
