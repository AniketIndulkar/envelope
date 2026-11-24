package dev.novanest.envelope.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.novanest.envelope.ui.theme.Accent
import dev.novanest.envelope.ui.theme.BigNumber
import dev.novanest.envelope.ui.theme.Hairline1
import dev.novanest.envelope.ui.theme.Ink
import dev.novanest.envelope.ui.theme.OnSky
import dev.novanest.envelope.ui.theme.OnSkyMuted
import dev.novanest.envelope.ui.theme.Sky
import dev.novanest.envelope.ui.theme.Surface
import dev.novanest.envelope.ui.theme.UiFamily
import dev.novanest.envelope.ui.theme.UnitText

val PassShape = RoundedCornerShape(24.dp)

/**
 * Days-left colour: white with time to spare, warming to peach as the deadline nears.
 * Starts warming at 14 days and is fully peach at 2 days or fewer (the prototype's warm()).
 */
fun warmColor(days: Int?): Color {
    if (days == null) return OnSky
    val k = ((14f - days) / 12f).coerceIn(0f, 1f)
    return lerp(OnSky, Accent, k)
}

/** The huge days-left number, coloured by [warmColor]. Shows an en dash when the letter has no deadline. */
@Composable
fun DaysNumber(days: Int?, modifier: Modifier = Modifier, color: Color = warmColor(days)) {
    Text(days?.coerceAtLeast(0)?.toString() ?: "–", modifier = modifier, style = BigNumber, color = color, maxLines = 1)
}

/** The 3dp glowing accent line across a pass: the Line in miniature. */
@Composable
fun PassLine(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(3.dp)
            .drawBehind {
                // Soft glow above and below, like the prototype's 14px box-shadow.
                val spread = 8.dp.toPx()
                drawRect(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        0f to Accent.copy(alpha = 0f), 0.5f to Accent.copy(alpha = 0.6f), 1f to Accent.copy(alpha = 0f),
                        startY = -spread, endY = size.height + spread,
                    ),
                    topLeft = androidx.compose.ui.geometry.Offset(0f, -spread),
                    size = size.copy(height = size.height + spread * 2),
                )
            }
            .background(Accent),
    )
}

/**
 * A pass: plum cloud top, the accent line, white phone bottom. Front and back (privacy receipt) both use it.
 * Tilt, flip and stacking are applied by the wallet with graphicsLayer on [modifier].
 */
@Composable
fun Pass(
    modifier: Modifier = Modifier,
    top: @Composable ColumnScope.() -> Unit,
    bottom: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .shadow(18.dp, PassShape, ambientColor = Ink, spotColor = Ink)
            .clip(PassShape)
            .background(Surface)
            .border(1.dp, Hairline1, PassShape),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Sky)
                .accentGlow(alpha = { 1f }, radiusX = 170.dp, radiusY = 100.dp, belowEdge = 30.dp, peak = 0.38f)
                .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = top,
        )
        PassLine()
        Column(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = bottom,
        )
    }
}

/**
 * Front of a letter's pass: sender, optional "Due soon", the big days number with its unit, and the due date.
 * [bottom] holds the next step, Open / Receipt and the "details kept" pill (built in the wallet phase).
 */
@Composable
fun PassFront(
    from: String,
    days: Int?,
    unit: String,
    due: String?,
    modifier: Modifier = Modifier,
    dueSoon: Boolean = days != null && days <= 7,
    bottom: @Composable ColumnScope.() -> Unit,
) {
    Pass(
        modifier,
        top = {
            Column(
                Modifier.clearAndSetSemantics {
                    contentDescription = buildString {
                        append("Letter from $from")
                        if (days != null) append(", $days ${if (days == 1) "day" else "days"} left")
                        due?.let { append(". $it") }
                    }
                },
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        from, color = OnSkyMuted, fontFamily = UiFamily, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                    )
                    if (dueSoon) DueSoonBadge()
                }
                Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Bottom) {
                    DaysNumber(days)
                    Text(unit, color = OnSky, style = UnitText, modifier = Modifier.padding(bottom = 6.dp))
                }
                due?.let { Text(it, color = OnSkyMuted, fontFamily = UiFamily, fontSize = 15.sp) }
            }
        },
        bottom = bottom,
    )
}

@Composable
private fun DueSoonBadge() {
    Text(
        "Due soon",
        color = Ink, fontFamily = UiFamily, fontSize = 13.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Accent).padding(horizontal = 9.dp, vertical = 4.dp),
    )
}
