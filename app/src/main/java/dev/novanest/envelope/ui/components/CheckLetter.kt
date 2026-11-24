package dev.novanest.envelope.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.CubicBezierEasing
import dev.novanest.envelope.ui.LetterDisplay
import dev.novanest.envelope.ui.theme.Hairline1
import dev.novanest.envelope.ui.theme.Ink
import dev.novanest.envelope.ui.theme.LetterBody
import dev.novanest.envelope.ui.theme.Mine
import dev.novanest.envelope.ui.theme.NanoMissing
import dev.novanest.envelope.ui.theme.OnSky
import dev.novanest.envelope.ui.theme.StandIn
import dev.novanest.envelope.ui.theme.StandInSoft
import dev.novanest.envelope.ui.theme.Surface
import dev.novanest.envelope.ui.theme.UiFamily

/**
 * When each detail in the Check letter scrambles into its stand-in (prototype: 450ms + 340ms apart, 620ms each).
 * Long letters squeeze the gap so the whole decode stays under about three seconds.
 */
class DecodeSchedule(private val count: Int) {
    private val gap: Long = if (count <= 1) 340L else minOf(340L, 2400L / (count - 1))
    fun startAt(i: Int): Long = 450L + i * gap
    fun lockAt(i: Int): Long = startAt(i) + DecodeMillis
    val endMillis: Long get() = if (count == 0) BeamEnd else maxOf(BeamEnd, lockAt(count - 1))

    companion object {
        const val DecodeMillis = 620L
        const val BeamDelay = 300L
        const val BeamMillis = 3200L
        const val BeamEnd = BeamDelay + BeamMillis
    }
}

/** Elapsed ms since [play] turned on, ticking each frame until [endMillis]; Long.MAX_VALUE when not playing or done. */
@Composable
fun rememberStageClock(play: Boolean, endMillis: Long): Long {
    val end by rememberUpdatedState(endMillis)
    var t by remember(play) { mutableLongStateOf(if (play) 0L else Long.MAX_VALUE) }
    LaunchedEffect(play) {
        if (!play) return@LaunchedEffect
        val start = withFrameMillis { it }
        while (t <= end) t = withFrameMillis { it } - start
        t = Long.MAX_VALUE
    }
    return t
}

private enum class SegKind { Raw, Scramble, StandIn }
private class Seg(val start: Int, val end: Int, val kind: SegKind)
private class Frame(val text: AnnotatedString, val segs: List<Seg>, val settled: Boolean)

private val StandInSpan = SpanStyle(fontFamily = UiFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = OnSky, letterSpacing = (-0.005).em)
private val ScrambleSpan = SpanStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = StandIn)
private val PlainLink = TextLinkStyles(SpanStyle())
private val CheckLetterStyle = LetterBody.copy(lineHeight = 30.sp)
private val BeamEasing = CubicBezierEasing(0.45f, 0f, 0.4f, 1f)
private val PaperShape = RoundedCornerShape(10.dp)

/**
 * The letter as it will cross the line (Check).
 *
 * At [clock] (from [rememberStageClock]) each detail is shown raw, then scrambling, then locked as its stand-in pill,
 * while a plum beam sweeps the page. Once settled the letter is interactive: tap a pill to un-hide it,
 * tap a word to hide it. Amber-underlined words are suggestions on phones without Nano.
 */
@Composable
fun CheckLetter(
    display: LetterDisplay,
    clock: Long,
    schedule: DecodeSchedule,
    onHideWord: (String) -> Unit,
    onShowValue: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val frame = if (clock == Long.MAX_VALUE || clock >= schedule.endMillis) {
        remember(display) { settledFrame(display, onShowValue) }
    } else {
        decodingFrame(display, schedule, clock)
    }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val hideWord by rememberUpdatedState(onHideWord)

    Box(
        modifier
            .fillMaxWidth()
            .shadow(12.dp, PaperShape, ambientColor = Ink, spotColor = Ink)
            .clip(PaperShape)
            .background(Surface)
            .border(1.dp, Hairline1, PaperShape)
            .semantics { contentDescription = "Your letter, as it will cross the line" }
            .drawWithContent {
                drawContent()
                val p = ((clock - DecodeSchedule.BeamDelay).toFloat() / DecodeSchedule.BeamMillis)
                if (clock == Long.MAX_VALUE || p !in 0f..1f) return@drawWithContent
                val beamH = 90.dp.toPx()
                val bottom = BeamEasing.transform(p) * (size.height + beamH)
                val alpha = if (p < 0.88f) 1f else 1f - (p - 0.88f) / 0.12f
                drawRect(
                    Brush.verticalGradient(
                        0f to StandIn.copy(alpha = 0f), 0.75f to StandIn.copy(alpha = 0.12f), 1f to StandIn.copy(alpha = 0.4f),
                        startY = bottom - beamH, endY = bottom,
                    ),
                    topLeft = Offset(0f, bottom - beamH), size = Size(size.width, beamH), alpha = alpha,
                )
                drawRect(StandIn, Offset(0f, bottom - 2.dp.toPx()), Size(size.width, 2.dp.toPx()), alpha = alpha)
            },
    ) {
        Text(
            frame.text,
            style = CheckLetterStyle,
            onTextLayout = { layout = it },
            modifier = Modifier
                .padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 24.dp)
                .drawWithCache {
                    val l = layout
                    val inset = 1.dp.toPx()
                    val grow = 3.dp.toPx()
                    fun rects(s: Int, e: Int) = l?.let { lineRects(it, s, e, inset) }.orEmpty()
                    val pills = frame.segs.filter { it.kind == SegKind.StandIn }.flatMap { rects(it.start, it.end) }
                        .map { Rect(it.left - grow, it.top, it.right + grow, it.bottom) }
                    val raws = frame.segs.filter { it.kind == SegKind.Raw }.flatMap { rects(it.start, it.end) }
                    val scrs = frame.segs.filter { it.kind == SegKind.Scramble }.flatMap { rects(it.start, it.end) }
                        .map { Rect(it.left - grow, it.top, it.right + grow, it.bottom) }
                    val warns = if (frame.settled) display.suggestions.flatMap { rects(it.first, it.last + 1) } else emptyList()
                    val bar = 2.dp.toPx()
                    onDrawBehind {
                        warns.forEach {
                            drawRect(Mine, it.topLeft, it.size)
                            drawRect(NanoMissing, Offset(it.left, it.bottom - bar), Size(it.width, bar))
                        }
                        raws.forEach { drawRoundRect(StandIn.copy(alpha = 0.10f), it.topLeft, it.size, CornerRadius(3.dp.toPx())) }
                        scrs.forEach { drawRoundRect(StandInSoft, it.topLeft, it.size, CornerRadius(6.dp.toPx())) }
                        pills.forEach { drawRoundRect(StandIn, it.topLeft, it.size, CornerRadius(it.height / 2f)) }
                    }
                }
                .then(
                    if (!frame.settled) Modifier else Modifier.pointerInput(display) {
                        detectTapGestures { pos ->
                            val l = layout ?: return@detectTapGestures
                            val offset = l.getOffsetForPosition(pos)
                            if (display.chips.any { offset in it.start until it.end }) return@detectTapGestures
                            display.words.firstOrNull { offset in it }?.let { hideWord(display.tapTarget(it)) }
                        }
                    },
                ),
        )
    }
}

/** Final letter: stand-in pills are real links so TalkBack users can un-hide them too. */
private fun settledFrame(d: LetterDisplay, onShowValue: (String) -> Unit): Frame {
    val text = buildAnnotatedString {
        append(d.text)
        d.chips.forEach { chip ->
            addStyle(StandInSpan, chip.start, chip.end)
            addLink(LinkAnnotation.Clickable("show:${chip.start}", PlainLink) { onShowValue(chip.value) }, chip.start, chip.end)
        }
    }
    return Frame(text, d.chips.map { Seg(it.start, it.end, SegKind.StandIn) }, settled = true)
}

/** Mid-decode letter: details before their start are raw, then scramble, then become the stand-in from [LetterDisplay.text]. */
private fun decodingFrame(d: LetterDisplay, schedule: DecodeSchedule, t: Long): Frame {
    val segs = mutableListOf<Seg>()
    val text = buildAnnotatedString {
        var cursor = 0
        d.chips.forEachIndexed { i, chip ->
            append(d.text, cursor, chip.start)
            val standIn = d.text.substring(chip.start, chip.end)
            val start = length
            val kind = when {
                t < schedule.startAt(i) -> { append(chip.value); SegKind.Raw }
                t < schedule.lockAt(i) -> {
                    val p = (t - schedule.startAt(i)).toFloat() / DecodeSchedule.DecodeMillis
                    pushStyle(ScrambleSpan); append(scramble(chip.value, LetterDisplay.label(chip.token), p)); pop()
                    SegKind.Scramble
                }
                else -> { pushStyle(StandInSpan); append(standIn); pop(); SegKind.StandIn }
            }
            segs += Seg(start, length, kind)
            cursor = chip.end
        }
        append(d.text, cursor, d.text.length)
    }
    return Frame(text, segs, settled = false)
}
