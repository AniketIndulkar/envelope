package dev.novanest.envelope.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.novanest.envelope.privacy.Redaction
import dev.novanest.envelope.ui.LetterDisplay
import dev.novanest.envelope.ui.theme.CardWhite
import dev.novanest.envelope.ui.theme.LetterBody
import dev.novanest.envelope.ui.theme.Primary
import dev.novanest.envelope.ui.theme.RealMark
import dev.novanest.envelope.ui.theme.Rule
import dev.novanest.envelope.ui.theme.SoonBg
import dev.novanest.envelope.ui.theme.SoonLine
import dev.novanest.envelope.ui.theme.UiFamily

/**
 * The letter as it will be sent. Each hidden detail is a solid blue block with security hatching and a readable label.
 * With [onHideWord]/[onShowValue], words and blocks are tappable; amber highlights are suggestions (no-Nano phones).
 */
@Composable
fun RedactedLetter(
    display: LetterDisplay,
    modifier: Modifier = Modifier,
    flat: Boolean = false,
    onHideWord: ((String) -> Unit)? = null,
    onShowValue: ((String) -> Unit)? = null,
) {
    val text = remember(display, onShowValue != null) { annotate(display, onShowValue) }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val hideWord by rememberUpdatedState(onHideWord)
    PaperSheet(modifier, flat) {
        Text(
            text = text,
            style = LetterBody,
            onTextLayout = { layout = it },
            modifier = Modifier
                // Hatch and suggestion shapes are measured once per layout and reused for every frame (scrolling).
                .drawWithCache {
                    val l = layout
                    val chipRects = l?.let { display.chips.flatMap { c -> lineRects(it, c.start, c.end, 2.dp.toPx()) } }.orEmpty()
                    val hintRects = l?.let { display.suggestions.flatMap { r -> lineRects(it, r.first, r.last + 1, 3.dp.toPx()) } }.orEmpty()
                    val radius = CornerRadius(6.dp.toPx())
                    val chipPaths = chipRects.map { Path().apply { addRoundRect(RoundRect(it, radius)) } }
                    val hatchStep = 6.dp.toPx(); val hatchWidth = 2.dp.toPx(); val dashWidth = 2.dp.toPx()
                    val dash = PathEffect.dashPathEffect(floatArrayOf(6f, 4f))
                    onDrawBehind {
                        hintRects.forEach { r ->
                            drawRect(SoonBg, r.topLeft, Size(r.width, r.height))
                            drawLine(SoonLine, Offset(r.left, r.bottom), Offset(r.right, r.bottom), strokeWidth = dashWidth, pathEffect = dash)
                        }
                        chipRects.forEachIndexed { i, r ->
                            drawRoundRect(Primary, r.topLeft, Size(r.width, r.height), radius)
                            clipPath(chipPaths[i]) {
                                var x = r.left - r.height
                                while (x < r.right) {
                                    drawLine(HatchLine, Offset(x, r.bottom), Offset(x + r.height, r.top), strokeWidth = hatchWidth)
                                    x += hatchStep
                                }
                            }
                        }
                    }
                }
                // One tap handler for the whole letter instead of hundreds of per-word links: cheaper to lay out and scroll.
                .then(
                    if (onHideWord == null) Modifier else Modifier.pointerInput(display) {
                        detectTapGestures { pos ->
                            val l = layout ?: return@detectTapGestures
                            val offset = l.getOffsetForPosition(pos)
                            if (display.chips.any { offset in it.start until it.end }) return@detectTapGestures
                            display.words.firstOrNull { offset in it }?.let { hideWord?.invoke(display.tapTarget(it)) }
                        }
                    }
                ),
        )
    }
}

/** The letter as it is on the phone, real details marked — the "On your phone" side of the privacy receipt. */
@Composable
fun OriginalLetter(redaction: Redaction, modifier: Modifier = Modifier, flat: Boolean = false) {
    val text = remember(redaction) {
        buildAnnotatedString {
            append(redaction.original)
            redaction.placements.forEach { addStyle(SpanStyle(background = RealMark), it.originalStart, it.originalEnd) }
        }
    }
    PaperSheet(modifier, flat) { Text(text, style = LetterBody) }
}

@Composable
fun PaperSheet(modifier: Modifier = Modifier, flat: Boolean = false, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Surface(
        modifier = modifier.fillMaxWidth().then(if (flat) Modifier.border(1.dp, Rule, shape) else Modifier),
        color = CardWhite,
        shape = shape,
        shadowElevation = if (flat) 0.dp else 4.dp,
    ) {
        Box(Modifier.padding(horizontal = 20.dp, vertical = 22.dp)) { content() }
    }
}

private val chipStyle = SpanStyle(fontFamily = UiFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = CardWhite)
private val plainLink = TextLinkStyles(SpanStyle())

private val HatchLine = Color.White.copy(alpha = 0.16f)

/** Blocks stay real links (few per letter) so TalkBack users can reveal them; plain words are handled by one tap detector. */
private fun annotate(d: LetterDisplay, onShowValue: ((String) -> Unit)?): AnnotatedString =
    buildAnnotatedString {
        append(d.text)
        d.chips.forEach { chip ->
            addStyle(chipStyle, chip.start, chip.end)
            if (onShowValue != null) {
                addLink(LinkAnnotation.Clickable("show:${chip.start}", plainLink) { onShowValue(chip.value) }, chip.start, chip.end)
            }
        }
    }

internal fun lineRects(l: TextLayoutResult, start: Int, end: Int, pad: Float): List<Rect> {
    if (end <= start || end > l.layoutInput.text.length) return emptyList()
    val first = l.getLineForOffset(start)
    val last = l.getLineForOffset(end - 1)
    return (first..last).map { line ->
        val left = if (line == first) l.getHorizontalPosition(start, true) else l.getLineLeft(line)
        val right = if (line == last) l.getHorizontalPosition(end, true) else l.getLineRight(line)
        Rect(left, l.getLineTop(line) + pad, right, l.getLineBottom(line) - pad)
    }
}
