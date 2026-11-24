package dev.rushi.apkdownloadhelper.ui.shared.progress

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.fillMaxWidth

/**
 * The manager's wavy progress ring, drawn locally because the helper's Material3 version predates
 * the expressive indicator API. The wave keeps moving while the download runs, and a null
 * [progress] spins a pair of indeterminate arcs for work whose total is not known yet.
 */
@Composable
internal fun WavyProgressRing(
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

/**
 * Material 3 Expressive-style wavy linear progress bar: animated sine wave for the completed
 * portion, clean gap, and straight rounded track for the remaining portion. Smoothly flattens
 * when paused ([active] = false).
 */
@Composable
internal fun WavyLinearProgressBar(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
    strokeWidth: Dp = 4.5.dp,
    amplitude: Dp = 3.2.dp,
    wavelength: Dp = 28.dp,
    gap: Dp = 8.dp,
    active: Boolean = true
) {
    val phase = if (active) {
        val transition = rememberInfiniteTransition(label = "wavy_linear_progress")
        val animatedPhase by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "wavy_linear_phase"
        )
        animatedPhase
    } else {
        0f
    }

    val animatedAmplitude by animateDpAsState(
        targetValue = if (active) amplitude else 0.dp,
        animationSpec = tween(220),
        label = "wavy_linear_amplitude"
    )

    Canvas(modifier = modifier.fillMaxWidth()) {
        val stroke = strokeWidth.toPx()
        val halfStroke = stroke / 2f
        val centerY = size.height / 2f
        val usableWidth = (size.width - stroke).coerceAtLeast(0f)
        if (usableWidth <= 0f) return@Canvas

        val clamped = progress().coerceIn(0f, 1f)
        val activeEnd = halfStroke + usableWidth * clamped
        val gapPx = gap.toPx()

        // Straight remaining track
        val trackStart = if (clamped > 0f) activeEnd + gapPx else halfStroke
        val trackEnd = size.width - halfStroke
        if (trackStart < trackEnd) {
            drawLine(
                color = trackColor,
                start = Offset(trackStart, centerY),
                end = Offset(trackEnd, centerY),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
        }

        // Animated wavy active segment
        if (clamped > 0f) {
            val length = (activeEnd - halfStroke).coerceAtLeast(0f)
            val ampPx = animatedAmplitude.toPx()
            if (ampPx <= 0.05f || length < stroke) {
                drawLine(
                    color = color,
                    start = Offset(halfStroke, centerY),
                    end = Offset(activeEnd, centerY),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
            } else {
                val waveLenPx = wavelength.toPx().coerceAtLeast(1f)
                val phaseRad = phase * 2f * PI.toFloat()
                val steps = max(12, (length / 3f).toInt())
                val path = Path()
                for (index in 0..steps) {
                    val x = halfStroke + length * (index / steps.toFloat())
                    val y = centerY + sin((x / waveLenPx) * 2f * PI.toFloat() - phaseRad) * ampPx
                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(
                        width = stroke,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }
    }
}
