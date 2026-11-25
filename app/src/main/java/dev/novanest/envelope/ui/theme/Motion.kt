package dev.novanest.envelope.ui.theme

import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

/**
 * True when the system animator scale is 0 ("Remove animations" in Accessibility sets it too).
 * Every Line animation checks this and jumps to its end state instead.
 */
val LocalReducedMotion = staticCompositionLocalOf { false }

@Composable
fun rememberReducedMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    fun read() = Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    var reduced by remember(resolver) { mutableStateOf(read()) }
    DisposableEffect(resolver) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) { reduced = read() }
        }
        resolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, observer)
        onDispose { resolver.unregisterContentObserver(observer) }
    }
    return reduced
}

// Curves from the prototype.
/** The line and the cloud moving between stages: 950ms. */
val LineMoveEasing = CubicBezierEasing(0.65f, 0f, 0.2f, 1f)
const val LineMoveMillis = 950
/** Sparks and rings thrown where a stand-in crosses. */
val SparkEasing = CubicBezierEasing(0.2f, 0.8f, 0.3f, 1f)
/** Gentle overshoot for presses, chips and toggles. */
val SpringyEasing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)
/** Ease-out used for fades and the pass flip. */
val SettleEasing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)
