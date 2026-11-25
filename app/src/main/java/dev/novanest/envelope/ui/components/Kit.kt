package dev.novanest.envelope.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.novanest.envelope.ui.theme.Hairline1
import dev.novanest.envelope.ui.theme.Ink
import dev.novanest.envelope.ui.theme.SoftSurface
import dev.novanest.envelope.ui.theme.StandIn
import dev.novanest.envelope.ui.theme.Surface
import dev.novanest.envelope.ui.theme.TitleTracking
import dev.novanest.envelope.ui.theme.UiFamily

// Shared pieces for the plain phone screens (settings, history loading, receipt), in the Line look:
// white sheets with a hairline, ink buttons, Instrument Sans. The stages use their own components.

private val SheetShape = RoundedCornerShape(18.dp)

/** A white sheet with a hairline edge, holding a section of a plain screen. */
@Composable
fun EnvelopeCard(modifier: Modifier = Modifier, padding: Dp = 18.dp, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().clip(SheetShape).background(Surface).border(1.dp, Hairline1, SheetShape).padding(padding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

/** Kept for older call sites; maps onto [LineButton]. */
enum class ButtonKind { Primary, Secondary }

@Composable
fun EnvelopeButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: ButtonKind = ButtonKind.Primary,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
) = LineButton(
    text, onClick, modifier.fillMaxWidth(),
    if (kind == ButtonKind.Primary) LineButtonKind.Primary else LineButtonKind.Secondary,
    icon, enabled = enabled, loading = loading,
)

/** Back arrow and a title, for plain screens. */
@Composable
fun EnvelopeTopBar(title: String, onBack: (() -> Unit)?, actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(start = 8.dp, end = 12.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        onBack?.let { IconButton(onClick = it) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Ink) } }
        Text(
            title, color = Ink, fontFamily = UiFamily, fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = TitleTracking,
            modifier = Modifier.weight(1f).padding(start = if (onBack == null) 8.dp else 0.dp).semantics { heading() },
        )
        actions()
    }
}

/** A tinted notice: icon, optional title, body. [bg]/[fg] come from the Line tokens (Mine/WarnInk, RiskBg/RiskInk, SoftSurface/Ink). */
@Composable
fun Notice(icon: ImageVector, title: String?, body: String, bg: Color = SoftSurface, fg: Color = Ink) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(bg)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(22.dp))
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            title?.let { Text(it, color = fg, fontFamily = UiFamily, fontSize = 17.sp, fontWeight = FontWeight.Bold) }
            Text(body, color = fg, fontFamily = UiFamily, fontSize = 15.sp, lineHeight = 22.sp)
        }
    }
}

/** A spinner and a sentence, while something on this phone is working. */
@Composable
fun LoadingCard(text: String) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp).semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        CircularProgressIndicator(Modifier.size(22.dp), color = StandIn, strokeWidth = 2.5.dp)
        Text(text, color = Ink, fontFamily = UiFamily, fontSize = 17.sp)
    }
}
