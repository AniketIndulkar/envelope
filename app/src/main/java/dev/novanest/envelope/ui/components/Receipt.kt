package dev.novanest.envelope.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.novanest.envelope.ui.theme.BigNumber
import dev.novanest.envelope.ui.theme.Hairline2
import dev.novanest.envelope.ui.theme.Hairline4
import dev.novanest.envelope.ui.theme.Ink
import dev.novanest.envelope.ui.theme.OnSky
import dev.novanest.envelope.ui.theme.TitleTracking
import dev.novanest.envelope.ui.theme.UiFamily

/**
 * The privacy receipt as a pass: above the line, the stand-ins that went up to Gemini; below it, how many real
 * details never left and what kinds they were. Used on the back of a wallet pass and on the receipt screen.
 * [onTurnBack] adds the "Turn back" button (wallet only).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReceiptPass(
    standIns: List<String>,
    ledger: List<Pair<String, Int>>,
    modifier: Modifier = Modifier,
    a11yLabel: String = "Privacy receipt",
    onTurnBack: (() -> Unit)? = null,
) {
    val kept = standIns.size
    Pass(
        modifier.semantics(mergeDescendants = false) { contentDescription = a11yLabel },
        top = {
            LinePill("Went up to Gemini", kind = PillKind.Cloud, icon = Icons.Rounded.Cloud)
            Text(
                if (kept == 0) "Nothing personal was found to hide"
                else "Only $kept ${if (kept == 1) "stand-in" else "stand-ins"} crossed the line",
                color = OnSky, fontFamily = UiFamily, fontSize = 24.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold,
                letterSpacing = TitleTracking, modifier = Modifier.padding(top = 4.dp),
            )
            if (standIns.isNotEmpty()) FlowRow(
                Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                standIns.forEach { StandInChip(it, tone = StandInTone.Cloud) }
            }
        },
        bottom = {
            LinePill("Stayed on this phone", kind = PillKind.Phone, icon = Icons.Rounded.PhoneAndroid)
            Row(
                Modifier.clearAndSetSemantics { contentDescription = "$kept real ${if (kept == 1) "detail" else "details"} never left this phone" },
                verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("$kept", style = BigNumber.copy(fontSize = 72.sp, lineHeight = 58.sp), color = Ink)
                Text(
                    if (kept == 1) "real detail\nnever left" else "real details\nnever left",
                    color = Ink, fontFamily = UiFamily, fontSize = 20.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = TitleTracking,
                )
            }
            if (ledger.isNotEmpty()) Column {
                ledger.forEachIndexed { i, (kind, n) -> LedgerRow(kind, n, divider = i > 0) }
            }
            onTurnBack?.let {
                LineButton("Turn back", it, Modifier.fillMaxWidth(), LineButtonKind.Secondary, Icons.Rounded.Sync, minHeight = 50.dp)
            }
        },
    )
}

/** "Names ........ 2" with a dotted leader. */
@Composable
private fun LedgerRow(kind: String, count: Int, divider: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (divider) Modifier.drawBehind { drawRect(Hairline2, size = size.copy(height = 1.dp.toPx())) } else Modifier)
            .semantics(mergeDescendants = true) {}
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(kind, color = Ink, fontFamily = UiFamily, fontSize = 15.sp)
        Box(
            Modifier.weight(1f).height(14.dp).drawBehind {
                val y = size.height - 4.dp.toPx()
                drawLine(
                    Hairline4, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(1.5.dp.toPx(), 3.dp.toPx())),
                )
            },
        )
        Text("$count", color = Ink, fontFamily = UiFamily, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}
