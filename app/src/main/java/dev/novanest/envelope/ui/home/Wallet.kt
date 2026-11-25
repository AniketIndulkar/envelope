package dev.novanest.envelope.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.novanest.envelope.ui.components.DueSoonBadge
import dev.novanest.envelope.ui.components.LineButton
import dev.novanest.envelope.ui.components.LineButtonKind
import dev.novanest.envelope.ui.components.LinePill
import dev.novanest.envelope.ui.components.Pass
import dev.novanest.envelope.ui.components.PassFront
import dev.novanest.envelope.ui.components.PillKind
import dev.novanest.envelope.ui.components.StandInChip
import dev.novanest.envelope.ui.components.StandInTone
import dev.novanest.envelope.ui.components.warmColor
import dev.novanest.envelope.ui.theme.Accent
import dev.novanest.envelope.ui.theme.BigNumber
import dev.novanest.envelope.ui.theme.Hairline2
import dev.novanest.envelope.ui.theme.Hairline4
import dev.novanest.envelope.ui.theme.Ink
import dev.novanest.envelope.ui.theme.LocalReducedMotion
import dev.novanest.envelope.ui.theme.OnSky
import dev.novanest.envelope.ui.theme.OnSkyMuted
import dev.novanest.envelope.ui.theme.SettleEasing
import dev.novanest.envelope.ui.theme.SkyStripA
import dev.novanest.envelope.ui.theme.SkyStripB
import dev.novanest.envelope.ui.theme.SpringyEasing
import dev.novanest.envelope.ui.theme.TitleTracking
import dev.novanest.envelope.ui.theme.UiFamily
import dev.novanest.envelope.ui.theme.UrgentRed
import kotlinx.coroutines.delay

private val StripShape = RoundedCornerShape(22.dp)
private val LandEasing = CubicBezierEasing(0.34f, 1.4f, 0.5f, 1f)

/** Strips shown above the front pass while stacked; "Show all" fans out the rest. */
const val StackedStrips = 4

/**
 * The wallet: the other letters as strips stacked above the front pass. [fanned] spreads them out with a spring.
 * A strip tapped comes to the front. [landedId] is a letter that just arrived: its pass drops in and the strips
 * shuffle down to make room.
 */
@Composable
fun WalletStack(
    letters: List<WalletLetter>,
    front: WalletLetter,
    fanned: Boolean,
    flipped: Boolean,
    landedId: String?,
    onPick: (WalletLetter) -> Unit,
    onOpen: (WalletLetter) -> Unit,
    onFlip: () -> Unit,
) {
    val others = letters.filter { it.id != front.id }
    // Furthest deadline at the top, so the strip touching the pass is the next one due.
    val strips = (if (fanned) others else others.take(StackedStrips)).reversed()
    Column(Modifier.fillMaxWidth().padding(top = 4.dp)) {
        strips.forEachIndexed { i, letter ->
            Strip(letter, i, fanned, shuffleDelay = if (landedId == front.id) 720L + i * 90L else null) { onPick(letter) }
        }
        FlippablePass(front, flipped, landed = landedId == front.id, onOpen = { onOpen(front) }, onFlip = onFlip)
    }
}

@Composable
private fun Strip(letter: WalletLetter, index: Int, fanned: Boolean, shuffleDelay: Long?, onClick: () -> Unit) {
    val reduced = LocalReducedMotion.current
    val gap by animateDpAsState(if (fanned) 10.dp else (-36).dp, if (reduced) snap() else tween(600, easing = SpringyEasing), label = "strip-gap")
    val shuffle = remember { Animatable(if (shuffleDelay != null && !reduced) 1f else 0f) }
    LaunchedEffect(shuffleDelay) {
        if (shuffleDelay == null || reduced) return@LaunchedEffect
        delay(shuffleDelay)
        shuffle.animateTo(0f, tween(700, easing = SpringyEasing))
    }
    val bg = if (index % 2 == 0) SkyStripA else SkyStripB
    Row(
        Modifier
            .fillMaxWidth()
            // Report a shorter (stacked) or taller (fanned) height so the next card overlaps or sits below.
            .layout { m, c ->
                val p = m.measure(c)
                val h = (p.height + gap.roundToPx()).coerceAtLeast(0)
                layout(p.width, h) { p.place(0, 0) }
            }
            .graphicsLayer { translationY = -shuffle.value * 34.dp.toPx() }
            .height(88.dp)
            .shadow(14.dp, StripShape, ambientColor = Ink, spotColor = Ink)
            .clip(StripShape)
            .background(bg)
            .drawBehind { drawRect(Color.White.copy(alpha = 0.12f), size = size.copy(height = 1.dp.toPx())) }
            .clickable(role = Role.Button, onClick = onClick)
            .clearAndSetSemantics {
                role = Role.Button
                contentDescription = "Show the letter from ${letter.from}, ${letter.stripLabel.replace("–", "no deadline")}"
            }
            .padding(horizontal = 20.dp, vertical = 15.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            letter.from, color = OnSky, fontFamily = UiFamily, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
        )
        Text(
            letter.stripLabel,
            color = if (letter.scam) Accent else warmColor(letter.days),
            fontFamily = UiFamily, fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.02).em,
        )
    }
}

/** Front or back of the front letter, with the flip, the drop-in for a new letter, and the touch tilt. */
@Composable
private fun FlippablePass(letter: WalletLetter, flipped: Boolean, landed: Boolean, onOpen: () -> Unit, onFlip: () -> Unit) {
    val reduced = LocalReducedMotion.current
    // Flip: the new face swings in from -75° (prototype .flipin).
    val flip = remember(letter.id) { Animatable(1f) }
    var lastFlipped by remember(letter.id) { mutableStateOf(flipped) }
    LaunchedEffect(flipped) {
        if (flipped == lastFlipped) return@LaunchedEffect
        lastFlipped = flipped
        if (reduced) return@LaunchedEffect
        flip.snapTo(0f)
        flip.animateTo(1f, tween(600, easing = SettleEasing))
    }
    // A newly saved letter drops into the wallet (prototype .land).
    val land = remember(letter.id) { Animatable(if (landed && !reduced) 0f else 1f) }
    LaunchedEffect(letter.id, landed) {
        if (!landed || reduced) return@LaunchedEffect
        delay(300) // let the navigation fade finish, so the drop is seen whole
        land.animateTo(1f, tween(1000, easing = LandEasing))
    }
    // Tilt toward the finger while it rests on the pass; scrolling still works because nothing is consumed.
    var tilt by remember { mutableStateOf(Offset.Zero) }
    var touching by remember { mutableStateOf(false) }
    val tx by animateFloatAsState(if (touching) tilt.x else 0f, tween(250, easing = SettleEasing), label = "tx")
    val ty by animateFloatAsState(if (touching) tilt.y else 0f, tween(250, easing = SettleEasing), label = "ty")

    Box(
        Modifier
            .fillMaxWidth()
            .then(
                if (reduced) Modifier else Modifier.pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val e = awaitPointerEvent()
                            val c = e.changes.firstOrNull() ?: continue
                            when (e.type) {
                                PointerEventType.Press, PointerEventType.Move -> {
                                    touching = c.pressed
                                    tilt = Offset(c.position.x / size.width - 0.5f, c.position.y / size.height - 0.5f)
                                }
                                else -> if (!c.pressed) touching = false
                            }
                        }
                    }
                },
            )
            .graphicsLayer {
                cameraDistance = 12f * density
                rotationX = -ty * 7f
                rotationY = tx * 9f + (1f - flip.value) * -75f
                alpha = flip.value.coerceIn(0f, 1f) * (if (land.value < 0.6f) land.value / 0.6f else 1f)
                translationY = (1f - land.value) * -260.dp.toPx()
                rotationZ = (1f - land.value) * -4f
                val s = 0.94f + 0.06f * land.value
                scaleX = s; scaleY = s
            },
    ) {
        if (flipped) PassBack(letter, onFlip) else PassFrontFor(letter, onOpen, onFlip)
        // Sheen that follows the finger.
        if (tx != 0f || ty != 0f) Box(
            Modifier
                .matchParentSize()
                .clip(dev.novanest.envelope.ui.components.PassShape)
                .drawBehind {
                    drawRect(
                        Brush.radialGradient(
                            listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0f)),
                            center = Offset((tx + 0.5f) * size.width, (ty + 0.5f) * size.height), radius = 240.dp.toPx(),
                        ),
                    )
                },
        )
    }
}

@Composable
private fun PassFrontFor(letter: WalletLetter, onOpen: () -> Unit, onFlip: () -> Unit) {
    PassFront(
        from = letter.from,
        days = letter.days,
        unit = letter.unit,
        due = letter.due,
        noNumberText = if (letter.scam) "Check first" else "No deadline",
        badge = when {
            letter.scam -> ({ DueSoonBadge("Possible scam", bg = UrgentRed, fg = OnSky) })
            letter.days != null && letter.days <= 7 -> ({ DueSoonBadge() })
            else -> null
        },
        a11yLabel = letter.frontA11y,
    ) {
        Text(
            letter.next, color = Ink, fontFamily = UiFamily, fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.01).em, maxLines = 3, overflow = TextOverflow.Ellipsis,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LineButton("Open", onOpen, Modifier.weight(1f), minHeight = 50.dp)
            LineButton("Receipt", onFlip, Modifier.weight(1f), LineButtonKind.Secondary, Icons.Rounded.Sync, minHeight = 50.dp)
        }
        if (letter.kept > 0) LinePill(
            "${letter.kept} ${if (letter.kept == 1) "detail" else "details"} kept on this phone",
            kind = PillKind.Mine, icon = Icons.Rounded.PhoneAndroid,
        )
    }
}

/** Back of the pass: the privacy receipt. Above the line what went to Gemini, below what never left. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PassBack(letter: WalletLetter, onFlip: () -> Unit) {
    Pass(
        Modifier.semantics(mergeDescendants = false) { contentDescription = "Privacy receipt for the letter from ${letter.from}" },
        top = {
            LinePill("Went up to Gemini", kind = PillKind.Cloud, icon = Icons.Rounded.Cloud)
            Text(
                if (letter.kept == 0) "Nothing personal was found to hide"
                else "Only ${letter.kept} ${if (letter.kept == 1) "stand-in" else "stand-ins"} crossed the line",
                color = OnSky, fontFamily = UiFamily, fontSize = 24.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold,
                letterSpacing = TitleTracking, modifier = Modifier.padding(top = 4.dp),
            )
            if (letter.standIns.isNotEmpty()) FlowRow(
                Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                letter.standIns.forEach { StandInChip(it, tone = StandInTone.Cloud) }
            }
        },
        bottom = {
            LinePill("Stayed on this phone", kind = PillKind.Phone, icon = Icons.Rounded.PhoneAndroid)
            Row(
                Modifier.clearAndSetSemantics { contentDescription = "${letter.kept} real details never left this phone" },
                verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("${letter.kept}", style = BigNumber.copy(fontSize = 72.sp, lineHeight = 58.sp), color = Ink)
                Text("real details\nnever left", color = Ink, fontFamily = UiFamily, fontSize = 20.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = TitleTracking)
            }
            Column {
                letter.ledger.forEachIndexed { i, (kind, n) -> LedgerRow(kind, n, divider = i > 0) }
            }
            LineButton("Turn back", onFlip, Modifier.fillMaxWidth(), LineButtonKind.Secondary, Icons.Rounded.Sync, minHeight = 50.dp)
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

/** No letters yet: an empty pass explaining what will live here. */
@Composable
fun EmptyPass() {
    Pass(
        top = {
            Text(
                "Your letters live here", color = OnSky, fontFamily = UiFamily, fontSize = 26.sp, lineHeight = 28.sp,
                fontWeight = FontWeight.Bold, letterSpacing = TitleTracking,
            )
            Text("Letters you scan show up here, soonest deadline first.", color = OnSkyMuted, fontFamily = UiFamily, fontSize = 15.sp, lineHeight = 21.sp)
        },
        bottom = {
            Text(
                "Scan a letter. Your name, address and numbers stay on this phone; only stand-ins go to Gemini.",
                color = Ink, fontFamily = UiFamily, fontSize = 17.sp, lineHeight = 24.sp,
            )
        },
    )
}
