package dev.novanest.envelope.ui.components

import android.graphics.BlurMaskFilter
import android.graphics.DashPathEffect
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.runtime.State
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import dev.novanest.envelope.ui.theme.Accent
import dev.novanest.envelope.ui.theme.AccentHigh
import dev.novanest.envelope.ui.theme.LocalReducedMotion
import dev.novanest.envelope.ui.theme.SparkEasing
import kotlin.math.exp
import kotlin.math.sin

/** One stand-in crossing the line: where (from the line's left edge) and when (ms after the crossings were set). */
@Immutable
data class LineCrossing(val x: Dp, val atMillis: Long)

/** How long a crossing keeps the line moving, from the prototype's ripple. */
private const val RippleMillis = 1400L
private const val SparkMillis = 750f
private const val RingMillis = 800f

/** Height the line occupies; the stroke sits on its vertical centre so it can be placed on the cloud/phone edge. */
val LineHeight = 40.dp

private const val LineLabel = "The edge of your phone. Above is the cloud, below is this phone."

/**
 * The Line: a glowing peach stroke that is the edge of the phone.
 *
 * - [dashed]: no Gemini Nano on this phone, so the edge is only partly guarded.
 * - [hot]: something is crossing (Explain): the glow pulses faster.
 * - [crossings]: each one ripples the stroke and throws a spark and a ring at its x.
 *
 * With reduced motion the glow is still and crossings draw nothing.
 */
@Composable
fun TheLine(
    modifier: Modifier = Modifier,
    dashed: Boolean = false,
    hot: Boolean = false,
    crossings: List<LineCrossing> = emptyList(),
) {
    val reduced = LocalReducedMotion.current
    val pulse: State<Float> = if (reduced) {
        remember { mutableFloatStateOf(0f) }
    } else {
        rememberInfiniteTransition(label = "line-glow").animateFloat(
            initialValue = 0f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(if (hot) 450 else 1700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "glow",
        )
    }

    // Clock for ripples, sparks and rings. Runs only while a crossing is still moving.
    var elapsed by remember(crossings) { mutableLongStateOf(0L) }
    LaunchedEffect(crossings, reduced) {
        if (reduced || crossings.isEmpty()) return@LaunchedEffect
        val end = crossings.maxOf { it.atMillis } + RippleMillis
        val start = withFrameMillis { it }
        do {
            elapsed = withFrameMillis { it } - start
        } while (elapsed <= end)
    }

    val glowPaint = remember { android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG) }

    Canvas(modifier.fillMaxWidth().height(LineHeight).semantics { contentDescription = LineLabel }) {
        val t = elapsed // read in draw only: ticking redraws, never recomposes
        val midY = size.height / 2f
        val path = linePath(midY, if (reduced) emptyList() else crossings, t)
        val stroke = 3.dp.toPx()
        val dash = if (dashed) floatArrayOf(10.dp.toPx(), 8.dp.toPx()) else null

        // Glow: two blurred copies of the stroke, breathing between the prototype's two drop-shadow sets.
        val p = pulse.value
        drawBlurredPath(glowPaint, path, stroke, lerp(10f, 22f, p).dp.toPx(), Accent.copy(alpha = lerp(0.35f, 0.6f, p)), dash)
        drawBlurredPath(glowPaint, path, stroke, lerp(3f, 5f, p).dp.toPx(), Accent.copy(alpha = lerp(0.7f, 0.9f, p)), dash)
        drawPath(path, Accent, style = Stroke(stroke, cap = StrokeCap.Round, pathEffect = dash?.let { PathEffect.dashPathEffect(it) }))

        if (!reduced) crossings.forEach { drawCrossing(glowPaint, it, t, midY) }
    }
}

/** Flat line, plus a damped wave around each crossing that started in the last 1.4s. */
private fun DrawScope.linePath(midY: Float, crossings: List<LineCrossing>, t: Long): Path {
    val path = Path()
    val step = 4.dp.toPx()
    val amp = 9.dp.toPx()
    val spread = 46.dp.toPx()
    var x = 0f
    path.moveTo(0f, midY)
    while (x <= size.width) {
        var y = midY
        for (c in crossings) {
            val dt = (t - c.atMillis).toFloat()
            if (dt <= 0f || dt >= RippleMillis) continue
            val d = (x - c.x.toPx()) / spread
            y += amp * exp(-dt / 260f) * sin(dt / 38f) * exp(-d * d)
        }
        path.lineTo(x, y)
        x += step
    }
    path.lineTo(size.width, midY)
    return path
}

private fun DrawScope.drawCrossing(paint: android.graphics.Paint, c: LineCrossing, t: Long, midY: Float) {
    val dt = (t - c.atMillis).toFloat()
    if (dt < 0f) return
    val center = Offset(c.x.toPx(), midY)
    if (dt < RingMillis) {
        val p = SparkEasing.transform(dt / RingMillis)
        drawCircle(Accent.copy(alpha = 1f - p), radius = 5.dp.toPx() * lerp(0.3f, 5f, p), center = center, style = Stroke(2.dp.toPx()))
    }
    if (dt < SparkMillis) {
        val p = SparkEasing.transform(dt / SparkMillis)
        val r = 4.dp.toPx() * lerp(0.2f, 2.6f, p)
        val a = 1f - p
        drawIntoCanvas { canvas ->
            paint.style = android.graphics.Paint.Style.FILL
            paint.pathEffect = null
            paint.maskFilter = BlurMaskFilter(10.dp.toPx(), BlurMaskFilter.Blur.NORMAL)
            paint.color = Accent.copy(alpha = 0.9f * a).toArgb()
            canvas.nativeCanvas.drawCircle(center.x, center.y, r + 4.dp.toPx(), paint)
        }
        drawCircle(AccentHigh.copy(alpha = a), radius = r, center = center)
    }
}

/** BlurMaskFilter is drawn by hardware from API 28; older phones just get the plain stroke. */
private fun DrawScope.drawBlurredPath(
    paint: android.graphics.Paint, path: Path, width: Float, blur: Float, color: Color, dash: FloatArray?,
) = drawIntoCanvas { canvas ->
    paint.style = android.graphics.Paint.Style.STROKE
    paint.strokeWidth = width
    paint.strokeCap = android.graphics.Paint.Cap.ROUND
    paint.pathEffect = dash?.let { DashPathEffect(it, 0f) }
    paint.maskFilter = BlurMaskFilter(blur, BlurMaskFilter.Blur.NORMAL)
    paint.color = color.toArgb()
    canvas.nativeCanvas.drawPath(path.asAndroidPath(), paint)
}

/**
 * Peach light rising from the bottom edge of a cloud area (the sky above the line, or a pass's top).
 * [alpha] is the overall strength; the Explain stage raises it as stand-ins cross.
 */
fun Modifier.accentGlow(alpha: () -> Float, radiusX: Dp = 180.dp, radiusY: Dp = 180.dp, belowEdge: Dp = 40.dp, peak: Float = 0.55f): Modifier =
    drawBehind {
        val a = alpha().coerceIn(0f, 1f)
        if (a == 0f) return@drawBehind
        val rx = radiusX.toPx()
        val ry = radiusY.toPx()
        val center = Offset(size.width / 2f, size.height + belowEdge.toPx())
        // Draw a circle of radius ry, stretched horizontally to rx; clipped so the light stays above the line.
        clipRect {
            scale(scaleX = rx / ry, scaleY = 1f, pivot = center) {
                drawCircle(
                    Brush.radialGradient(listOf(Accent.copy(alpha = peak * a), Accent.copy(alpha = 0f)), center = center, radius = ry),
                    radius = ry, center = center,
                )
            }
        }
    }
