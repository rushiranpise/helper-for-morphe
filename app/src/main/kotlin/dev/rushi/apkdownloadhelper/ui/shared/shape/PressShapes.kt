package dev.rushi.apkdownloadhelper.ui.shared.shape

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.util.lerp
import dev.rushi.apkdownloadhelper.ui.theme.PillInner
import dev.rushi.apkdownloadhelper.ui.theme.PillOuter
import dev.rushi.apkdownloadhelper.ui.theme.PressedCornerScale

/** Shapes of the members of a connected row of buttons: round at the ends, nearly square between. */
internal object PillGroupShapes {
    private val single = RoundedCornerShape(PillOuter)
    private val leading = RoundedCornerShape(
        topStart = PillOuter, bottomStart = PillOuter, topEnd = PillInner, bottomEnd = PillInner
    )
    private val middle = RoundedCornerShape(PillInner)
    private val trailing = RoundedCornerShape(
        topStart = PillInner, bottomStart = PillInner, topEnd = PillOuter, bottomEnd = PillOuter
    )

    fun inRow(index: Int, count: Int): RoundedCornerShape = when {
        count == 1 -> single
        index == 0 -> leading
        index == count - 1 -> trailing
        else -> middle
    }
}

/** How far into a press [interactionSource] is, from 0 at rest to 1 held. */
@Composable
internal fun rememberPressProgress(
    interactionSource: InteractionSource,
    enabled: Boolean = true,
    label: String = "press_progress"
): State<Float> {
    val isPressed by interactionSource.collectIsPressedAsState()
    return animateFloatAsState(
        targetValue = if (enabled && isPressed) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium),
        label = label
    )
}

/** [shape] with its corners rounding down while pressed. */
@Composable
internal fun rememberPressShape(
    interactionSource: InteractionSource,
    shape: Shape,
    enabled: Boolean = true
): Shape {
    val progress by rememberPressProgress(interactionSource, enabled, label = "press_shape")
    return remember(shape, progress) { shape.pressed(progress) }
}

private fun Shape.pressed(progress: Float): Shape {
    if (this !is CornerBasedShape || progress == 0f) return this
    val scale = lerp(1f, PressedCornerScale, progress)
    return copy(
        topStart = ScaledCornerSize(topStart, scale),
        topEnd = ScaledCornerSize(topEnd, scale),
        bottomEnd = ScaledCornerSize(bottomEnd, scale),
        bottomStart = ScaledCornerSize(bottomStart, scale)
    )
}

private data class ScaledCornerSize(val base: CornerSize, val scale: Float) : CornerSize {
    override fun toPx(shapeSize: Size, density: Density): Float = base.toPx(shapeSize, density) * scale
}

/**
 * [target] with its corners easing over from the shape it had before, so a picked button
 * rounds out rather than jumping. The first shape is taken at once.
 */
@Composable
internal fun animateCornersAsState(target: CornerBasedShape): CornerBasedShape {
    val corners = remember { AnimatedCorners(target) }
    LaunchedEffect(target) { corners.animateTo(target) }
    return corners.shape
}

@Stable
private class AnimatedCorners(initial: CornerBasedShape) {
    private class Leg(val from: CornerBasedShape, val to: CornerBasedShape)

    private var leg by mutableStateOf(Leg(initial, initial))
    private var progress by mutableFloatStateOf(1f)

    /** The same shape throughout; its corners are resolved where it is drawn. */
    val shape: CornerBasedShape = initial.copy(
        topStart = Corner(CornerBasedShape::topStart),
        topEnd = Corner(CornerBasedShape::topEnd),
        bottomEnd = Corner(CornerBasedShape::bottomEnd),
        bottomStart = Corner(CornerBasedShape::bottomStart)
    )

    suspend fun animateTo(target: CornerBasedShape) {
        val current = leg
        if (target == current.to) return
        leg = Leg(current.at(progress), target)
        progress = 0f
        animate(0f, 1f, animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium)) { value, _ ->
            progress = value
        }
    }

    private fun Leg.at(fraction: Float): CornerBasedShape = when (fraction) {
        0f -> from
        1f -> to
        else -> to.copy(
            topStart = LerpCornerSize(from.topStart, to.topStart, fraction),
            topEnd = LerpCornerSize(from.topEnd, to.topEnd, fraction),
            bottomEnd = LerpCornerSize(from.bottomEnd, to.bottomEnd, fraction),
            bottomStart = LerpCornerSize(from.bottomStart, to.bottomStart, fraction)
        )
    }

    private inner class Corner(private val of: (CornerBasedShape) -> CornerSize) : CornerSize {
        override fun toPx(shapeSize: Size, density: Density): Float {
            val leg = leg
            val end = of(leg.to).toPx(shapeSize, density)
            val fraction = progress
            return if (fraction == 1f) end else lerp(of(leg.from).toPx(shapeSize, density), end, fraction)
        }
    }
}

private data class LerpCornerSize(val start: CornerSize, val stop: CornerSize, val fraction: Float) : CornerSize {
    override fun toPx(shapeSize: Size, density: Density): Float =
        lerp(start.toPx(shapeSize, density), stop.toPx(shapeSize, density), fraction)
}
