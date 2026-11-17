package dev.rushi.apkdownloadhelper.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlin.math.PI
import kotlin.math.sin

/**
 * The manager's default Circles background: a handful of large, very soft circles
 * drifting behind the content. Kept subtle so cards and text stay readable.
 */
@Composable
internal fun MorpheBackground(modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val circles = remember(primary, secondary, tertiary) {
        listOf(
            BackgroundCircle(0.20f, 0.225f, 133f, primary, 0.05f, 0f, 0.8f, 0.05f, 0.025f, 4f, 3f),
            BackgroundCircle(0.85f, 0.185f, 93f, tertiary, 0.035f, 1.4f, 2.2f, 0.03f, 0.035f, 3f, 4f),
            BackgroundCircle(0.715f, 0.44f, 67f, tertiary, 0.04f, 2.7f, 0.4f, 0.035f, 0.04f, 4f, 3f),
            BackgroundCircle(0.815f, 0.785f, 107f, secondary, 0.035f, 0.6f, 3.1f, 0.035f, 0.035f, 3f, 4f),
            BackgroundCircle(0.24f, 0.765f, 60f, primary, 0.04f, 3.4f, 1.7f, 0.04f, 0.035f, 4f, 3f),
            BackgroundCircle(0.525f, 0.895f, 73f, secondary, 0.04f, 2.1f, 4.3f, 0.025f, 0.025f, 3f, 4f),
        )
    }
    // Keep the circles on screen under reduced motion, but do not start an infinite
    // transition that would keep requesting frames on a low-power device.
    val motionAllowed = rememberMotionAllowed()
    val progress = if (motionAllowed) {
        val transition = rememberInfiniteTransition(label = "morphe_background")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 32_000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "morphe_background_progress"
        )
    } else {
        null
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val twoPi = 2f * PI.toFloat()
        val time = (progress?.value ?: 0f) * twoPi
        circles.forEach { circle ->
            val center = Offset(
                x = size.width * (circle.x + circle.xAmplitude * sin(time * circle.xCycles + circle.xPhase)),
                y = size.height * (circle.y + circle.yAmplitude * sin(time * circle.yCycles + circle.yPhase))
            )
            drawCircle(
                color = circle.color.copy(alpha = circle.alpha),
                radius = circle.radius.dp.toPx(),
                center = center
            )
        }
    }
}

@Composable
private fun rememberMotionAllowed(): Boolean {
    val context = LocalContext.current
    val powerManager = remember(context) {
        context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    }
    val animatorScaleEnabled = remember(context) {
        mutableStateOf(context.animatorScaleEnabled())
    }
    val batterySaver = remember(powerManager) {
        mutableStateOf(powerManager?.isPowerSaveMode == true)
    }

    DisposableEffect(context, powerManager) {
        val scaleObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                animatorScaleEnabled.value = context.animatorScaleEnabled()
            }
        }
        context.contentResolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
            false,
            scaleObserver
        )

        val batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context?, intent: Intent?) {
                batterySaver.value = powerManager?.isPowerSaveMode == true
            }
        }
        if (powerManager != null) {
            ContextCompat.registerReceiver(
                context,
                batteryReceiver,
                IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        }

        onDispose {
            context.contentResolver.unregisterContentObserver(scaleObserver)
            if (powerManager != null) {
                context.unregisterReceiver(batteryReceiver)
            }
        }
    }

    return animatorScaleEnabled.value && !batterySaver.value
}

private fun Context.animatorScaleEnabled(): Boolean =
    Settings.Global.getFloat(
        contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1f
    ) > 0f

@Immutable
private data class BackgroundCircle(
    val x: Float,
    val y: Float,
    val radius: Float,
    val color: Color,
    val alpha: Float,
    val xPhase: Float,
    val yPhase: Float,
    val xAmplitude: Float,
    val yAmplitude: Float,
    val xCycles: Float,
    val yCycles: Float
)
