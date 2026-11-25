package dev.novanest.envelope.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import dev.novanest.envelope.ui.theme.LocalReducedMotion
import dev.novanest.envelope.ui.theme.ScrambleText
import dev.novanest.envelope.ui.theme.StandIn
import dev.novanest.envelope.ui.theme.StandInSoft
import kotlin.random.Random

private const val Glyphs = "ABCDEFGHJKLMNPQRSTUVWXYZ0123456789#%&"

/**
 * The prototype's scramble: [from] morphs into [to] as [p] goes 0→1.
 * Length eases between the two; the first p·|to| characters are already final, the rest are random glyphs.
 */
fun scramble(from: String, to: String, p: Float, random: Random = Random.Default): String {
    val len = maxOf(1, Math.round(from.length + (to.length - from.length) * p))
    val fixed = (to.length * p).toInt()
    return buildString(len) {
        for (j in 0 until len) append(if (j < fixed && j < to.length) to[j] else Glyphs[random.nextInt(Glyphs.length)])
    }
}

/** 0 before [delayMillis], then 0→1 over [durationMillis]. Jumps straight to 1 with reduced motion. */
@Composable
fun rememberDecodeProgress(key: Any?, delayMillis: Long, durationMillis: Long): Float {
    val reduced = LocalReducedMotion.current
    var p by remember(key) { mutableFloatStateOf(if (reduced) 1f else 0f) }
    LaunchedEffect(key, reduced) {
        if (reduced) { p = 1f; return@LaunchedEffect }
        val start = withFrameMillis { it }
        while (p < 1f) {
            val t = withFrameMillis { it } - start - delayMillis
            p = (t.toFloat() / durationMillis).coerceIn(0f, 1f)
        }
    }
    return p
}

/**
 * A stand-in that decodes back into your real detail on this phone (Answer: "Put back on this phone only").
 * Stand-in chip → scrambling monospace → gold real detail. TalkBack hears only the real detail.
 */
@Composable
fun DecodeToReal(
    standIn: String,
    real: String,
    delayMillis: Long,
    modifier: Modifier = Modifier,
    durationMillis: Long = 700,
) {
    val p = rememberDecodeProgress(standIn to real, delayMillis, durationMillis)
    val a11y = Modifier.clearAndSetSemantics { contentDescription = "$standIn put back as $real" }
    when {
        p <= 0f -> StandInChip(standIn, modifier.then(a11y))
        p >= 1f -> RealDetailChip(real, modifier.then(a11y), RealDetailStyle.Kept)
        else -> ScrambleChip(scramble(standIn, real, p), modifier.then(a11y))
    }
}

/** Mid-scramble look: soft plum with monospace glyphs. */
@Composable
fun ScrambleChip(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier.clip(RoundedCornerShape(6.dp)).background(StandInSoft).heightIn(min = 28.dp).padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = ScrambleText, color = StandIn, maxLines = 1)
    }
}
