package dev.rushi.apkdownloadhelper.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * The manager's wavy progress ring, drawn locally because the helper's Material3 version predates
 * the expressive indicator API. The wave keeps moving while the download runs, and a null
 * [progress] spins a pair of indeterminate arcs for work whose total is not known yet.
 */
@Composable
internal fun MorpheWavyProgressRing(
    progress: (() -> Float)?,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    strokeWidth: Dp = 8.dp,
    amplitude: Dp = 2.dp,
    active: Boolean = true
) {
    // Leaving the transition out of composition while paused stops its frame clock, so a frozen
    // download does not keep drawing motion that suggests work is still happening.
    val phase = if (active) {
        val transition = rememberInfiniteTransition(label = "wavy_progress")
        val animatedPhase by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1400, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "wavy_progress_phase"
        )
        animatedPhase
    } else {
        0f
    }

    Canvas(modifier = modifier) {
        val stroke = strokeWidth.toPx()
        val wave = amplitude.toPx()
        val radius = (min(size.width, size.height) - stroke) / 2f - wave
        if (radius <= 0f) return@Canvas

        drawCircle(
            color = trackColor,
            radius = radius,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )

        val value = progress?.invoke()?.coerceIn(0f, 1f)
        if (value == null) {
            val start = -90f + phase * 360f
            drawWavyArc(
                baseRadius = radius,
                strokeWidth = stroke,
                color = color,
                startAngle = start,
                sweepAngle = 105f,
                phase = phase,
                waveAmplitude = wave
            )
            drawWavyArc(
                baseRadius = radius,
                strokeWidth = stroke,
                color = color.copy(alpha = 0.55f),
                startAngle = start + 180f,
                sweepAngle = 105f,
                phase = phase,
                waveAmplitude = wave
            )
        } else if (value > 0f) {
            drawWavyArc(
                baseRadius = radius,
                strokeWidth = stroke,
                color = color,
                startAngle = -90f,
                sweepAngle = value * 360f,
                phase = phase,
                waveAmplitude = wave
            )
        }
    }
}

private fun DrawScope.drawWavyArc(
    baseRadius: Float,
    strokeWidth: Float,
    color: Color,
    startAngle: Float,
    sweepAngle: Float,
    phase: Float,
    waveAmplitude: Float
) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val steps = max(24, (sweepAngle / 3f).toInt())
    val wavePhase = phase * 2f * PI.toFloat()
    val path = Path()
    for (index in 0..steps) {
        val fraction = index / steps.toFloat()
        val angle = startAngle + sweepAngle * fraction
        val radians = Math.toRadians(angle.toDouble()).toFloat()
        // A gentle ripple along the ring, sized to stay inside the composable's own bounds
        val ripple = sin(radians * 9f + wavePhase) * waveAmplitude
        val radius = baseRadius + ripple
        val point = Offset(
            x = center.x + cos(radians) * radius,
            y = center.y + sin(radians) * radius
        )
        if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}
