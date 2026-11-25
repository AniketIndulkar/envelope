package dev.novanest.envelope.ui.components

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.RowScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import dev.novanest.envelope.ui.theme.LocalReducedMotion
import dev.novanest.envelope.ui.theme.OnSky
import dev.novanest.envelope.ui.theme.SettleEasing
import kotlinx.coroutines.delay

/**
 * Status bar icon colour for the screen being shown: white over the plum cloud, dark over the light phone screens.
 * Each screen states its own, so whichever screen is entering wins; nothing is restored on leave.
 */
@Composable
fun StatusBarIcons(darkIcons: Boolean) {
    val activity = LocalActivity.current ?: return
    SideEffect {
        WindowCompat.getInsetsController(activity.window, activity.window.decorView).isAppearanceLightStatusBars = darkIcons
    }
}

/** Stages with the plum cloud at the top need white status bar icons. */
@Composable
fun LightStatusBarIcons() = StatusBarIcons(darkIcons = false)

/** Light screens (wallet, settings, receipt) need dark status bar icons. */
@Composable
fun DarkStatusBarIcons() = StatusBarIcons(darkIcons = true)

/** The bar at the top of a cloud stage: a Close button, then whatever the stage puts in [content]. */
@Composable
fun CloudBar(onClose: () -> Unit, modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 56.dp).padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, contentDescription = "Close", tint = OnSky) }
        content()
    }
}

/** The prototype's `.fade`: rise 8dp and fade in after [delayMillis]. Shown at once with reduced motion. */
@Composable
fun Modifier.fadeUp(delayMillis: Long = 300, durationMillis: Int = 550): Modifier {
    val reduced = LocalReducedMotion.current
    val p = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (reduced) return@LaunchedEffect
        delay(delayMillis)
        p.animateTo(1f, tween(durationMillis, easing = SettleEasing))
    }
    return graphicsLayer {
        alpha = p.value
        translationY = (1f - p.value) * 8.dp.toPx()
    }
}
