package dev.novanest.envelope.ui.review

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.novanest.envelope.ui.components.CloudBar
import dev.novanest.envelope.ui.components.LineCrossing
import dev.novanest.envelope.ui.components.LinePill
import dev.novanest.envelope.ui.components.PillKind
import dev.novanest.envelope.ui.components.RealDetailChip
import dev.novanest.envelope.ui.components.RealDetailStyle
import dev.novanest.envelope.ui.components.StandInChip
import dev.novanest.envelope.ui.components.StandInTone
import dev.novanest.envelope.ui.components.fadeUp
import dev.novanest.envelope.ui.components.singleLine
import dev.novanest.envelope.ui.theme.Ink
import dev.novanest.envelope.ui.theme.LocalReducedMotion
import dev.novanest.envelope.ui.theme.Muted
import dev.novanest.envelope.ui.theme.OnSky
import dev.novanest.envelope.ui.theme.OnSkyMuted
import dev.novanest.envelope.ui.theme.TitleTracking
import dev.novanest.envelope.ui.theme.UiFamily
import kotlinx.coroutines.delay

/** Where the line sits on the Explain stage, below the status bar (prototype: 520). */
val ExplainLineY = 520.dp

/** Stand-ins shown rising before "+ N more" (the field holds about four rows). */
private const val MaxFlying = 12

/** How many chips rise for [standIns] stand-ins, counting the "+ N more" chip. */
fun flyingCount(standIns: Int): Int = if (standIns > MaxFlying) MaxFlying + 1 else standIns

/**
 * When each stand-in leaves the phone (prototype: 250ms + 260ms apart, 1.15s rise).
 * Many stand-ins squeeze the gap so the crossing finishes in about 3.5 seconds.
 */
class CrossingSchedule(private val count: Int) {
    private val gap: Long = if (count <= 1) 260L else minOf(260L, 2100L / (count - 1))
    fun leaveAt(i: Int): Long = 250L + i * gap
    fun crossAt(i: Int): Long = leaveAt(i) + 30L
    /** When the last stand-in has reached its place in the cloud. */
    val doneMillis: Long get() = if (count == 0) 0L else leaveAt(count - 1) + RiseMillis

    companion object { const val RiseMillis = 1150L }
}

private val RiseEasing = CubicBezierEasing(0.3f, 0.75f, 0.25f, 1f)

/**
 * The cloud during Explain: "Gemini reads stand-ins only", the stand-ins that rose through the line, and a shimmer
 * while Gemini writes. [onCrossings] reports where each stand-in crosses so the line can ripple there.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExplainCloud(
    standIns: List<String>,
    clock: Long,
    schedule: CrossingSchedule,
    onClose: () -> Unit,
    onCrossings: (List<LineCrossing>) -> Unit,
) {
    val reduced = LocalReducedMotion.current
    val density = LocalDensity.current
    val shown = standIns.take(MaxFlying)
    val more = standIns.size - shown.size
    val labels = if (more > 0) shown + "+ $more more" else shown

    // Where each chip sits, relative to the cloud's top; filled in on first layout.
    var cloudTop by remember { mutableStateOf<Float?>(null) }
    val chipTops = remember(labels) { mutableStateListOf<Float?>().apply { repeat(labels.size) { add(null) } } }
    val chipCenters = remember(labels) { mutableStateListOf<Float?>().apply { repeat(labels.size) { add(null) } } }
    val lineY = with(density) { ExplainLineY.toPx() } // measured from below the status bar, like cloudTop

    LaunchedEffect(labels, chipCenters.toList()) {
        if (chipCenters.any { it == null }) return@LaunchedEffect
        onCrossings(chipCenters.mapIndexed { i, x -> LineCrossing(with(density) { x!!.toDp() }, schedule.crossAt(i)) })
    }

    Box(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(ExplainLineY)
            .onGloballyPositioned { cloudTop = it.positionInRoot().y },
    ) {
        Column {
            CloudBar(onClose) {
                Spacer(Modifier.weight(1f))
                LinePill("Gemini", kind = PillKind.Cloud, icon = Icons.Rounded.Cloud)
            }
            Text(
                "Gemini reads\nstand-ins only",
                color = OnSky, fontFamily = UiFamily, fontSize = 34.sp, lineHeight = 35.sp, fontWeight = FontWeight.Bold,
                letterSpacing = TitleTracking,
                modifier = Modifier.fadeUp().padding(start = 20.dp, end = 20.dp, top = 8.dp).semantics { liveRegion = LiveRegionMode.Polite },
            )
        }

        // The field the stand-ins rise into.
        FlowRow(
            Modifier
                .padding(start = 20.dp, end = 20.dp, top = 200.dp)
                .fillMaxWidth()
                .clearAndSetSemantics {
                    contentDescription = "Crossed the line: " + standIns.joinToString(", ") + ". Your real details stayed on this phone."
                },
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            labels.forEachIndexed { i, label ->
                val leave = schedule.leaveAt(i)
                val p = if (reduced || clock == Long.MAX_VALUE) 1f
                else RiseEasing.transform(((clock - leave).toFloat() / CrossingSchedule.RiseMillis).coerceIn(0f, 1f))
                StandInChip(
                    label,
                    Modifier
                        .onGloballyPositioned { c ->
                            val top = cloudTop ?: return@onGloballyPositioned
                            val pos = c.positionInRoot()
                            chipTops[i] = pos.y - top
                            chipCenters[i] = pos.x + c.size.width / 2f
                        }
                        .graphicsLayer {
                            val from = chipTops[i]?.let { lineY - it } ?: 0f
                            translationY = (1f - p) * from
                            // Hidden below the line until it leaves; the cloud clips it as it rises through.
                            alpha = if (clock != Long.MAX_VALUE && clock < leave && !reduced) 0f else 1f
                        },
                    tone = StandInTone.Flying,
                    description = label,
                )
            }
        }

        Column(
            Modifier.align(androidx.compose.ui.Alignment.BottomStart).padding(start = 20.dp, end = 20.dp, bottom = 34.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SlowNotice()
            Shimmer(0.86f)
            Shimmer(0.64f)
        }
    }
}

/** After 10 seconds of waiting, say so; nothing else crosses while we wait. */
@Composable
private fun SlowNotice() {
    var slow by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(10_000); slow = true }
    if (slow) Text(
        "Still waiting for Gemini. Nothing else has left your phone.",
        color = OnSkyMuted, fontFamily = UiFamily, fontSize = 15.sp, lineHeight = 21.sp,
        modifier = Modifier.fadeUp(delayMillis = 0).semantics { liveRegion = LiveRegionMode.Polite },
    )
}

/** Placeholder lines for the answer Gemini is writing: a soft sweep, still with reduced motion. */
@Composable
private fun Shimmer(widthFraction: Float) {
    val reduced = LocalReducedMotion.current
    val shift = if (reduced) remember { mutableStateOf(0.5f) } else rememberInfiniteTransition(label = "shim")
        .animateFloat(1f, -1f, infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart), label = "shim-x")
    Box(
        Modifier
            .fillMaxWidth(widthFraction)
            .height(10.dp)
            .fadeUp(delayMillis = 2000, durationMillis = 400)
            .clip(CircleShape)
            .drawBehind {
                val w = size.width
                val x = shift.value * w
                drawRect(
                    Brush.horizontalGradient(
                        listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.38f), Color.White.copy(alpha = 0.10f)),
                        startX = x - w, endX = x + w,
                    ),
                )
            },
    )
}

/** Below the line during Explain: the real details that are staying put. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExplainPhone(realValues: List<String>) {
    val shown = realValues.take(4)
    val more = realValues.size - shown.size
    Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        LinePill("This phone", kind = PillKind.Phone, icon = Icons.Rounded.PhoneAndroid)
        Text("Your details stay here", color = Ink, fontFamily = UiFamily, fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = TitleTracking)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            shown.forEach { RealDetailChip(singleLine(it), style = RealDetailStyle.Kept) }
            if (more > 0) Text("+ $more more", color = Muted, fontFamily = UiFamily, fontSize = 15.sp, lineHeight = 30.sp)
        }
    }
}
