package dev.novanest.envelope.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.novanest.envelope.ui.theme.Hairline1
import dev.novanest.envelope.ui.theme.Hairline3
import dev.novanest.envelope.ui.theme.Ink
import dev.novanest.envelope.ui.theme.InkPressed
import dev.novanest.envelope.ui.theme.LocalReducedMotion
import dev.novanest.envelope.ui.theme.Muted
import dev.novanest.envelope.ui.theme.PhoneBg
import dev.novanest.envelope.ui.theme.SoftSurface
import dev.novanest.envelope.ui.theme.SpringyEasing
import dev.novanest.envelope.ui.theme.Surface
import dev.novanest.envelope.ui.theme.UiFamily

/**
 * The bottom dock: one main action (plus an optional second) pinned over the phone area.
 * Lay out a row of buttons inside with Row(Modifier.fillMaxWidth(), spacedBy(10.dp)).
 */
@Composable
fun Dock(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            // Opaque: the prototype's 94% dock relies on backdrop blur, which Android views don't have,
            // and even a nearly opaque dock left the letter readable through it on a real phone.
            .background(PhoneBg)
            .drawBehind { drawRect(Hairline1, size = size.copy(height = 1.dp.toPx())) }
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

/** Primary = ink, Secondary = white with a hairline, Lite = phone background (on the dark scan screen). */
enum class LineButtonKind { Primary, Secondary, Lite }

/**
 * The redesign's button: 14dp corners, 17sp semibold, presses down to 97% with a small spring.
 * [compact] is the 40dp row button used inside timeline cards and the reply card; it still gets a 48dp touch target.
 */
@Composable
fun LineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: LineButtonKind = LineButtonKind.Primary,
    icon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    compact: Boolean = false,
    minHeight: Dp = if (compact) 40.dp else 54.dp,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val reduced = LocalReducedMotion.current
    val scale by animateFloatAsState(
        if (pressed && !reduced) 0.97f else 1f,
        if (reduced) snap() else tween(200, easing = SpringyEasing),
        label = "press",
    )
    val (bg, fg) = when (kind) {
        LineButtonKind.Primary -> (if (pressed) InkPressed else Ink) to Surface
        LineButtonKind.Secondary -> Surface to Ink
        LineButtonKind.Lite -> (if (pressed) Surface else PhoneBg) to Ink
    }
    val shape = RoundedCornerShape(if (compact) 10.dp else 14.dp)
    val outline = when {
        kind != LineButtonKind.Secondary -> null
        pressed -> Ink
        else -> Hairline3
    }
    Row(
        modifier
            .minimumInteractiveComponentSize()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .heightIn(min = minHeight)
            .clip(shape)
            .background(if (enabled) bg else SoftSurface)
            .then(if (outline != null) Modifier.border(1.dp, outline, shape) else Modifier)
            .clickable(interaction, indication = null, enabled = enabled && !loading, role = Role.Button, onClick = onClick)
            .padding(horizontal = if (compact) 13.dp else 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val tint = if (enabled) fg else Muted
        val iconSize = if (compact) 16.dp else 20.dp
        if (loading) {
            CircularProgressIndicator(Modifier.size(iconSize), color = tint, strokeWidth = 2.5.dp)
            Spacer(Modifier.width(9.dp))
        } else icon?.let {
            Icon(it, contentDescription = null, tint = tint, modifier = Modifier.size(iconSize))
            Spacer(Modifier.width(9.dp))
        }
        Text(
            text, color = tint, fontFamily = UiFamily, fontWeight = FontWeight.SemiBold,
            fontSize = if (compact) 14.sp else 17.sp, letterSpacing = (-0.01).em, maxLines = 1,
        )
        trailingIcon?.let {
            Spacer(Modifier.width(6.dp))
            Icon(it, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
        }
    }
}
