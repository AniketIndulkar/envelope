package dev.novanest.envelope.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.novanest.envelope.ui.theme.CardWhite
import dev.novanest.envelope.ui.theme.Deep
import dev.novanest.envelope.ui.theme.Ink
import dev.novanest.envelope.ui.theme.InkSoft
import dev.novanest.envelope.ui.theme.OnDeepSoft
import dev.novanest.envelope.ui.theme.Primary
import dev.novanest.envelope.ui.theme.Tint
import dev.novanest.envelope.ui.theme.Urgent
import dev.novanest.envelope.ui.theme.UrgentBg
import dev.novanest.envelope.ui.theme.Soon
import dev.novanest.envelope.ui.theme.SoonBg

val CardShape = RoundedCornerShape(22.dp)
val HeroShape = RoundedCornerShape(28.dp)

/** Diagonal hairlines, like the tint printed inside a security envelope. */
fun Modifier.securityHatch(line: Color, spacing: Dp = 10.dp, width: Dp = 2.dp): Modifier = drawBehind {
    val step = spacing.toPx()
    var x = -size.height
    while (x < size.width) {
        drawLine(line, Offset(x, size.height), Offset(x + size.height, 0f), strokeWidth = width.toPx())
        x += step
    }
}

@Composable
fun EnvelopeCard(modifier: Modifier = Modifier, padding: Dp = 18.dp, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier = modifier.fillMaxWidth(), shape = CardShape, color = CardWhite, shadowElevation = 2.dp) {
        Column(Modifier.padding(padding), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

/** Deep blue hero with the security texture and, optionally, the envelope flap across the top. */
@Composable
fun HeroCard(modifier: Modifier = Modifier, flap: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier
            .fillMaxWidth()
            .clip(HeroShape)
            .background(Deep)
            .securityHatch(Color.White.copy(alpha = 0.05f))
            .drawBehind {
                if (flap) {
                    val flapPath = Path().apply {
                        moveTo(0f, 0f); lineTo(size.width, 0f); lineTo(size.width / 2f, 120.dp.toPx()); close()
                    }
                    drawPath(flapPath, Primary.copy(alpha = 0.9f))
                }
            },
    ) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
    }
}

@Composable
fun IconTile(icon: ImageVector, bg: Color, fg: Color, size: Dp = 44.dp, iconSize: Dp = 22.dp, corner: Dp = 14.dp) {
    Box(Modifier.size(size).clip(RoundedCornerShape(corner)).background(bg), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(iconSize))
    }
}

@Composable
fun Pill(text: String, bg: Color, fg: Color, icon: ImageVector? = null, modifier: Modifier = Modifier, dot: Color? = null) {
    Row(
        modifier.clip(CircleShape).background(bg).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        dot?.let { Box(Modifier.size(8.dp).clip(CircleShape).background(it)) }
        icon?.let { Icon(it, null, tint = fg, modifier = Modifier.size(15.dp)) }
        Text(text, color = fg, style = MaterialTheme.typography.labelMedium)
    }
}

enum class ButtonKind { Primary, Light, Ghost, Outline }

@Composable
fun EnvelopeButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: ButtonKind = ButtonKind.Primary,
    icon: ImageVector? = null,
    height: Dp = 56.dp,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val (bg, fg) = when (kind) {
        ButtonKind.Primary -> Primary to CardWhite
        ButtonKind.Light -> CardWhite to Deep
        ButtonKind.Ghost -> Color.White.copy(alpha = 0.12f) to CardWhite
        ButtonKind.Outline -> CardWhite to Deep
    }
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier
            .fillMaxWidth()
            .height(height)
            .then(if (kind == ButtonKind.Primary) Modifier.shadow(10.dp, shape, ambientColor = Primary, spotColor = Primary) else Modifier)
            .clip(shape)
            .background(if (enabled) bg else if (kind == ButtonKind.Primary) Color(0xFFA9B6DD) else Color(0xFFF2F4F7))
            .then(if (kind == ButtonKind.Outline) Modifier.border(1.5.dp, Color(0xFFD0D8F0), shape) else Modifier)
            .clickable(enabled = enabled && !loading, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(20.dp), color = fg, strokeWidth = 2.5.dp)
            Spacer(Modifier.width(10.dp))
        } else icon?.let {
            Icon(it, null, tint = fg, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(text, color = fg, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun EnvelopeTopBar(title: String, onBack: (() -> Unit)?, actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        onBack?.let { SquareIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", it) }
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f).semantics { heading() })
        actions()
    }
}

@Composable
fun SquareIconButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(14.dp), color = CardWhite, shadowElevation = 1.dp, modifier = Modifier.size(44.dp)) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = label, tint = Ink, modifier = Modifier.size(22.dp)) }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.titleLarge, modifier = modifier.padding(horizontal = 4.dp).semantics { heading() })
}

/** Day-count tile used in lists: red under a week, amber under a fortnight, blue otherwise. */
@Composable
fun DeadlineTile(days: Int?) {
    val (bg, fg) = deadlineColors(days)
    Column(
        Modifier.size(width = 60.dp, height = 64.dp).clip(RoundedCornerShape(16.dp)).background(bg)
            .semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(days?.coerceAtLeast(0)?.toString() ?: "–", color = fg, fontSize = 26.sp, fontWeight = FontWeight.Bold, lineHeight = 26.sp)
        Text(if (days == 1) "day" else "days", color = fg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

fun deadlineColors(days: Int?): Pair<Color, Color> = when {
    days == null -> Color(0xFFF2F4F7) to InkSoft
    days <= 7 -> UrgentBg to Urgent
    days <= 14 -> SoonBg to Soon
    else -> Tint to Primary
}

/** "25 days left" ring for the result hero; the arc shows how much of a 30-day window remains. */
@Composable
fun CountdownRing(days: Int, modifier: Modifier = Modifier) {
    Box(modifier.size(96.dp).semantics { contentDescription = "$days days left" }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(96.dp)) {
            val stroke = 8.dp.toPx()
            val inset = stroke / 2
            val arcSize = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
            drawArc(Color.White.copy(alpha = 0.18f), 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            val sweep = 360f * (days.coerceIn(0, 30) / 30f).coerceAtLeast(0.04f)
            drawArc(Color.White, -90f, sweep, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(days.coerceAtLeast(0).toString(), color = CardWhite, fontSize = 28.sp, fontWeight = FontWeight.Bold, lineHeight = 28.sp)
            Text("days left", color = OnDeepSoft, fontSize = 12.sp, textAlign = TextAlign.Center)
        }
    }
}

/** Tinted notice row: icon tile + title + body (amber warnings, blue privacy notes). */
@Composable
fun Notice(icon: ImageVector, title: String?, body: String, bg: Color, fg: Color, tile: Color, tileFg: Color = fg) {
    Row(
        Modifier.fillMaxWidth().clip(CardShape).background(bg).padding(18.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        IconTile(icon, tile, tileFg, size = 40.dp, iconSize = 20.dp, corner = 12.dp)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            title?.let { Text(it, color = fg, style = MaterialTheme.typography.titleMedium) }
            Text(body, color = fg, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun LoadingCard(text: String) {
    EnvelopeCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            CircularProgressIndicator(Modifier.size(24.dp), color = Primary, strokeWidth = 3.dp)
            Text(text, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

val ScreenPadding = PaddingValues(horizontal = 16.dp)

@Composable
fun Centered(content: @Composable BoxScope.() -> Unit) = Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center, content = content)
