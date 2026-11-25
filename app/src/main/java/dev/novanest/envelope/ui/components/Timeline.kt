package dev.novanest.envelope.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.novanest.envelope.ui.theme.Accent
import dev.novanest.envelope.ui.theme.DeadlineLabel
import dev.novanest.envelope.ui.theme.Hairline1
import dev.novanest.envelope.ui.theme.Hairline4
import dev.novanest.envelope.ui.theme.Ink
import dev.novanest.envelope.ui.theme.LocalReducedMotion
import dev.novanest.envelope.ui.theme.Muted
import dev.novanest.envelope.ui.theme.OnSky
import dev.novanest.envelope.ui.theme.Rail
import dev.novanest.envelope.ui.theme.RailRisk
import dev.novanest.envelope.ui.theme.RiskBg
import dev.novanest.envelope.ui.theme.RiskInk
import dev.novanest.envelope.ui.theme.Sky
import dev.novanest.envelope.ui.theme.SoftSurface
import dev.novanest.envelope.ui.theme.Surface
import dev.novanest.envelope.ui.theme.UiFamily
import dev.novanest.envelope.ui.theme.UrgentRed

/** Where a row sits on the rail: the rail starts at the "Today" dot and ends in a dashed stub at the risk card. */
private enum class RailPart { First, Middle, Last }

private val RailWidth = 28.dp
private val TimelineCardShape = RoundedCornerShape(16.dp)

/** Rail column for one row; draws its piece of the 2dp rail behind the node. */
@Composable
private fun RailCell(part: RailPart, node: @Composable () -> Unit) {
    Box(
        Modifier
            .width(RailWidth)
            .fillMaxHeight()
            .drawBehind {
                val x = 14.dp.toPx()
                val w = 2.dp.toPx()
                when (part) {
                    RailPart.First -> drawLine(Rail, Offset(x, 12.dp.toPx()), Offset(x, size.height), w)
                    RailPart.Middle -> drawLine(Rail, Offset(x, 0f), Offset(x, size.height), w)
                    RailPart.Last -> drawLine(
                        RailRisk, Offset(x, 0f), Offset(x, 22.dp.toPx()), w,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())),
                    )
                }
            },
        contentAlignment = Alignment.TopCenter,
    ) { node() }
}

/** First row of "What to do": a plum dot and "Today, 24 November". */
@Composable
fun TimelineToday(label: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        RailCell(RailPart.First) {
            Box(
                Modifier.padding(top = 5.dp).size(18.dp).drawBehind {
                    drawCircle(Sky.copy(alpha = 0.16f), radius = 9.dp.toPx())
                    drawCircle(Sky, radius = 5.dp.toPx())
                },
            )
        }
        Text(label, color = Muted, fontFamily = UiFamily, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.semantics { heading() })
    }
}

/**
 * One dated step in "What to do".
 * [deadline] marks the step that carries the letter's deadline (accent dot, accent outline, rust date label).
 * [action] is the step's own link button (for example "Pay on GOV.UK"); "Mark done" is always added after it.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TimelineStep(
    whenLabel: String,
    title: String,
    note: String?,
    done: Boolean,
    onDoneChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    deadline: Boolean = false,
    action: (@Composable FlowRowScope.() -> Unit)? = null,
) {
    val reduced = LocalReducedMotion.current
    val cardAlpha by animateFloatAsState(if (done) 0.62f else 1f, if (reduced) snap() else tween(300), label = "done-fade")
    Row(modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        RailCell(RailPart.Middle) { StepNode(done = done, deadline = deadline) }
        Column(
            Modifier
                .weight(1f)
                .graphicsLayer { alpha = cardAlpha }
                .then(if (deadline && !done) Modifier.shadow(10.dp, TimelineCardShape, ambientColor = Accent, spotColor = Accent) else Modifier)
                .clip(TimelineCardShape)
                .background(Surface)
                .border(if (deadline && !done) 1.5.dp else 1.dp, if (deadline && !done) Accent else Hairline1, TimelineCardShape)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(whenLabel, color = if (deadline) DeadlineLabel else Muted, fontFamily = UiFamily, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(
                title, color = Ink, fontFamily = UiFamily, fontSize = 17.sp, lineHeight = 23.sp, fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.01).em, textDecoration = if (done) TextDecoration.LineThrough else null,
            )
            note?.let { Text(it, color = Muted, fontFamily = UiFamily, fontSize = 15.sp, lineHeight = 22.sp) }
            FlowRow(
                Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                action?.invoke(this)
                MarkDoneToggle(done, onDoneChange)
            }
        }
    }
}

@Composable
private fun StepNode(done: Boolean, deadline: Boolean) {
    val reduced = LocalReducedMotion.current
    val big = done || deadline
    val size by animateDpAsState(if (big) 20.dp else 16.dp, if (reduced) snap() else tween(400, easing = dev.novanest.envelope.ui.theme.SpringyEasing), label = "node")
    val fill by animateColorAsState(
        when { done -> Sky; deadline -> Accent; else -> Surface }, if (reduced) snap() else tween(300), label = "node-fill",
    )
    val top: Dp = if (big) 16.dp else 18.dp
    Box(
        Modifier
            .padding(top = top)
            .size(size)
            .drawBehind {
                val r = this.size.minDimension / 2f
                when {
                    deadline && !done -> drawCircle(Accent.copy(alpha = 0.22f), radius = r + 5.dp.toPx())
                    !done -> drawCircle(Hairline4, radius = r)
                    else -> {}
                }
                drawCircle(fill, radius = if (!done && !deadline) r - 2.dp.toPx() else r)
            },
        contentAlignment = Alignment.Center,
    ) {
        if (done) Icon(Icons.Rounded.Check, contentDescription = null, tint = OnSky, modifier = Modifier.size(13.dp))
    }
}

/** "Mark done" / "Done" toggle: 40dp chip, 48dp touch target, announced as a switch-like toggle. */
@Composable
private fun MarkDoneToggle(done: Boolean, onDoneChange: (Boolean) -> Unit) {
    val reduced = LocalReducedMotion.current
    val bg by animateColorAsState(if (done) Sky else SoftSurface, if (reduced) snap() else tween(250), label = "chk-bg")
    val fg by animateColorAsState(if (done) OnSky else Ink, if (reduced) snap() else tween(250), label = "chk-fg")
    Row(
        Modifier
            .minimumInteractiveComponentSize()
            .heightIn(min = 40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .toggleable(value = done, role = Role.Checkbox, onValueChange = onDoneChange)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(Icons.Rounded.Check, contentDescription = null, tint = fg, modifier = Modifier.size(14.dp))
        Text(if (done) "Done" else "Mark done", color = fg, fontFamily = UiFamily, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Last row: red diamond and the "If you do nothing" card. */
@Composable
fun TimelineRisk(whenLabel: String, title: String, note: String?, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        RailCell(RailPart.Last) {
            Box(
                Modifier
                    .padding(top = 20.dp)
                    .size(13.dp)
                    .rotate(45f)
                    .drawBehind { drawRect(UrgentRed.copy(alpha = 0.14f), topLeft = Offset(-4.dp.toPx(), -4.dp.toPx()),
                        size = androidx.compose.ui.geometry.Size(this.size.width + 8.dp.toPx(), this.size.height + 8.dp.toPx())) }
                    .clip(RoundedCornerShape(3.dp))
                    .background(UrgentRed),
            )
        }
        Column(
            Modifier
                .weight(1f)
                .clip(TimelineCardShape)
                .background(RiskBg)
                .semantics(mergeDescendants = true) {}
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(whenLabel, color = RiskInk, fontFamily = UiFamily, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(title, color = RiskInk, fontFamily = UiFamily, fontSize = 17.sp, lineHeight = 23.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.01).em)
            note?.let { Text(it, color = RiskInk, fontFamily = UiFamily, fontSize = 15.sp, lineHeight = 22.sp) }
        }
    }
}

