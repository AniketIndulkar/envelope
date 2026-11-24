package dev.novanest.envelope.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.novanest.envelope.ui.theme.Accent
import dev.novanest.envelope.ui.theme.AccentRing
import dev.novanest.envelope.ui.theme.Ink
import dev.novanest.envelope.ui.theme.LetterBody
import dev.novanest.envelope.ui.theme.LetterFamily
import dev.novanest.envelope.ui.theme.Mine
import dev.novanest.envelope.ui.theme.NanoMissing
import dev.novanest.envelope.ui.theme.NanoReady
import dev.novanest.envelope.ui.theme.OnSky
import dev.novanest.envelope.ui.theme.SoftSurface
import dev.novanest.envelope.ui.theme.StandIn
import dev.novanest.envelope.ui.theme.StandInOnCloud
import dev.novanest.envelope.ui.theme.StandInPressed
import dev.novanest.envelope.ui.theme.UiFamily

/** Where a stand-in chip is drawn. Flying = mid-crossing in Explain (lighter, with a drop shadow). */
enum class StandInTone { Phone, Cloud, Flying }

private val StandInText = TextStyle(
    fontFamily = UiFamily, fontSize = 14.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.005).em,
)

/**
 * A stand-in such as "Name 1": the only form a personal detail takes when it crosses the line.
 * White on plum (7:1). When [onClick] is set the chip un-hides its detail and gets a 48dp touch target.
 */
@Composable
fun StandInChip(
    label: String,
    modifier: Modifier = Modifier,
    tone: StandInTone = StandInTone.Phone,
    onClick: (() -> Unit)? = null,
    description: String = if (onClick != null) "Hidden: $label. Tap to un-hide." else "Stand-in: $label",
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val bg = when {
        pressed -> StandInPressed
        tone == StandInTone.Phone -> StandIn
        else -> StandInOnCloud
    }
    val shape = CircleShape
    Box(
        modifier
            .then(if (onClick != null) Modifier.minimumInteractiveComponentSize() else Modifier)
            .then(if (tone == StandInTone.Flying) Modifier.shadow(10.dp, shape, ambientColor = Color.Black, spotColor = Color.Black) else Modifier)
            .clip(shape)
            .background(bg)
            .then(
                if (onClick != null) Modifier.clickable(interaction, indication = null, role = Role.Button, onClick = onClick)
                else Modifier,
            )
            .clearAndSetSemantics {
                contentDescription = description
                if (onClick != null) role = Role.Button
            }
            .heightIn(min = 28.dp)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = OnSky, style = StandInText, maxLines = 1)
    }
}

/** How a real detail sits: inline in letter text, or as its own "kept" token (Explain, Answer). */
enum class RealDetailStyle { Inline, Kept }

/**
 * One of your real details, shown only on this phone: peach paper with a 2dp accent underline.
 * Gold in the prototype's words ("Gold = added on this phone").
 */
@Composable
fun RealDetailChip(
    text: String,
    modifier: Modifier = Modifier,
    style: RealDetailStyle = RealDetailStyle.Kept,
    textStyle: TextStyle = if (style == RealDetailStyle.Kept) LetterBody.copy(fontFamily = LetterFamily, lineHeight = 30.sp) else LetterBody,
    onClick: (() -> Unit)? = null,
    description: String = if (onClick != null) "$text. Tap to hide." else "Your detail, kept on this phone: $text",
) {
    val corner = if (style == RealDetailStyle.Kept) 6.dp else 3.dp
    Box(
        modifier
            .then(if (onClick != null) Modifier.minimumInteractiveComponentSize() else Modifier)
            .clip(RoundedCornerShape(corner))
            .background(Mine)
            .drawBehind {
                val h = 2.dp.toPx()
                drawRect(Accent, topLeft = Offset(0f, size.height - h), size = size.copy(height = h))
            }
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .clearAndSetSemantics {
                contentDescription = description
                if (onClick != null) role = Role.Button
            }
            .padding(horizontal = if (style == RealDetailStyle.Kept) 8.dp else 3.dp),
    ) {
        Text(text, style = textStyle, color = Ink)
    }
}

/** Small status pills from the prototype: on the cloud, on the phone, "mine" (gold) and the no-Nano warning. */
enum class PillKind { Cloud, Phone, Mine, Warn }

@Composable
fun LinePill(
    text: String,
    modifier: Modifier = Modifier,
    kind: PillKind = PillKind.Phone,
    icon: ImageVector? = null,
    dot: Boolean = false,
) {
    val (bg, fg) = when (kind) {
        PillKind.Cloud -> Color.White.copy(alpha = 0.12f) to OnSky
        PillKind.Phone -> SoftSurface to Ink
        PillKind.Mine -> Mine to Ink
        PillKind.Warn -> Mine to Color(0xFF6B4100)
    }
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier
            .clip(shape)
            .background(bg)
            .drawBehind {
                val ring = when (kind) {
                    PillKind.Cloud -> Color.White.copy(alpha = 0.14f)
                    PillKind.Mine -> AccentRing
                    else -> null
                }
                ring?.let {
                    val w = 1.dp.toPx()
                    drawRoundRect(it, cornerRadius = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(w))
                }
            }
            .semantics(mergeDescendants = true) {}
            .padding(start = 9.dp, end = 11.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        if (dot) {
            val c = if (kind == PillKind.Warn) NanoMissing else NanoReady
            Box(
                Modifier.size(13.dp).drawBehind {
                    drawCircle(c.copy(alpha = 0.18f), radius = 6.5.dp.toPx())
                    drawCircle(c, radius = 3.5.dp.toPx())
                },
            )
        }
        icon?.let { Icon(it, contentDescription = null, tint = fg, modifier = Modifier.size(14.dp)) }
        Text(text, color = fg, fontFamily = UiFamily, fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.005).em)
    }
}
