package dev.novanest.envelope.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import dev.novanest.envelope.privacy.TokenEntry
import dev.novanest.envelope.ui.theme.Accent
import dev.novanest.envelope.ui.theme.Ink
import dev.novanest.envelope.ui.theme.LetterBody
import dev.novanest.envelope.ui.theme.Mine

/** Every place a real detail's value appears in [text], longest values first so "Margaret O'Neill" beats "O'Neill". */
fun realDetailRanges(text: String, tokens: List<TokenEntry>): List<IntRange> {
    val taken = BooleanArray(text.length)
    val out = mutableListOf<IntRange>()
    tokens.map { it.value }.filter { it.isNotBlank() }.distinct().sortedByDescending { it.length }.forEach { value ->
        var i = text.indexOf(value)
        while (i >= 0) {
            val end = i + value.length
            if ((i until end).none { taken[it] }) {
                for (k in i until end) taken[k] = true
                out += i until end
            }
            i = text.indexOf(value, end)
        }
    }
    return out.sortedBy { it.first }
}

/**
 * Letter text with your real details marked gold: peach paper behind, a 2dp accent underline below.
 * Marks are drawn per line from the text layout, so a detail that wraps keeps its underline on both lines.
 */
@Composable
fun MarkedLetterText(text: String, marks: List<IntRange>, modifier: Modifier = Modifier, style: TextStyle = LetterBody) {
    var layout by remember(text) { mutableStateOf<TextLayoutResult?>(null) }
    Text(
        text,
        style = style,
        color = Ink,
        onTextLayout = { layout = it },
        modifier = modifier.drawBehind {
            val l = layout ?: return@drawBehind
            val padX = 3.dp.toPx()
            val bar = 2.dp.toPx()
            for (r in marks) {
                if (r.first >= text.length) continue
                val last = minOf(r.last, text.length - 1)
                for (line in l.getLineForOffset(r.first)..l.getLineForOffset(last)) {
                    val s = maxOf(r.first, l.getLineStart(line))
                    val e = minOf(last + 1, l.getLineEnd(line, visibleEnd = true))
                    if (e <= s) continue
                    val x0 = l.getHorizontalPosition(s, usePrimaryDirection = true)
                    val x1 = l.getHorizontalPosition(e, usePrimaryDirection = true)
                    val left = minOf(x0, x1) - padX
                    val width = kotlin.math.abs(x1 - x0) + padX * 2
                    val top = l.getLineTop(line)
                    val bottom = l.getLineBottom(line)
                    drawRect(Mine, Offset(left, top), Size(width, bottom - top))
                    drawRect(Accent, Offset(left, bottom - bar), Size(width, bar))
                }
            }
        },
    )
}
