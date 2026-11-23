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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
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
    val text = remember(display, onHideWord != null, onShowValue != null) { annotate(display, onHideWord, onShowValue) }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    PaperSheet(modifier, flat) {
        Text(
            text = text,
            style = LetterBody,
            onTextLayout = { layout = it },
            modifier = Modifier.drawBehind {
                layout?.let { l ->
                    display.suggestions.forEach { drawSuggestion(l, it.first, it.last + 1) }
                    display.chips.forEach { drawChip(l, it.start, it.end) }
                }
            },
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

private fun annotate(d: LetterDisplay, onHideWord: ((String) -> Unit)?, onShowValue: ((String) -> Unit)?): AnnotatedString =
    buildAnnotatedString {
        append(d.text)
        d.chips.forEach { chip ->
            addStyle(chipStyle, chip.start, chip.end)
            if (onShowValue != null) {
                addLink(LinkAnnotation.Clickable("show:${chip.start}", plainLink) { onShowValue(chip.value) }, chip.start, chip.end)
            }
        }
        if (onHideWord != null) d.words.forEach { range ->
            val target = d.tapTarget(range)
            addLink(LinkAnnotation.Clickable("hide:${range.first}", plainLink) { onHideWord(target) }, range.first, range.last + 1)
        }
    }

private fun DrawScope.lineRects(l: TextLayoutResult, start: Int, end: Int, pad: Dp): List<Rect> {
    if (end <= start || end > l.layoutInput.text.length) return emptyList()
    val first = l.getLineForOffset(start)
    val last = l.getLineForOffset(end - 1)
    return (first..last).map { line ->
        val left = if (line == first) l.getHorizontalPosition(start, true) else l.getLineLeft(line)
        val right = if (line == last) l.getHorizontalPosition(end, true) else l.getLineRight(line)
        Rect(left, l.getLineTop(line) + pad.toPx(), right, l.getLineBottom(line) - pad.toPx())
    }
}

private fun DrawScope.drawChip(l: TextLayoutResult, start: Int, end: Int) {
    val radius = CornerRadius(6.dp.toPx())
    lineRects(l, start, end, 2.dp).forEach { rect ->
        drawRoundRect(Primary, rect.topLeft, Size(rect.width, rect.height), radius)
        clipPath(Path().apply { addRoundRect(RoundRect(rect, radius)) }) {
            var x = rect.left - rect.height
            while (x < rect.right) {
                drawLine(Color.White.copy(alpha = 0.16f), Offset(x, rect.bottom), Offset(x + rect.height, rect.top), strokeWidth = 2.dp.toPx())
                x += 6.dp.toPx()
            }
        }
    }
}

private fun DrawScope.drawSuggestion(l: TextLayoutResult, start: Int, end: Int) {
    lineRects(l, start, end, 3.dp).forEach { rect ->
        drawRect(SoonBg, rect.topLeft, Size(rect.width, rect.height))
        drawLine(
            SoonLine, Offset(rect.left, rect.bottom), Offset(rect.right, rect.bottom),
            strokeWidth = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f)),
        )
    }
}
