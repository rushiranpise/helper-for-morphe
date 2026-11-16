package dev.rushi.apkdownloadhelper.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import androidx.compose.ui.unit.dp

// ==== ANIMATIONS ====
object MorpheSprings {
    fun <T> listSpring(dampingRatio: Float = 0.8f): androidx.compose.animation.core.SpringSpec<T> =
        spring(stiffness = 400f, dampingRatio = dampingRatio)
}

// ==== PRESS FEEDBACK ====
const val PressedScale = 0.97f

@Composable
fun Modifier.pressScale(
    interactionSource: InteractionSource,
    enabled: Boolean = true,
    pressedScale: Float = PressedScale
): Modifier {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (enabled && isPressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "press_scale"
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

// ==== GLASS BLUR ====
fun glassBlurStyle(tint: Color, background: Color): HazeStyle {
    val solid = tint.compositeOver(background)
    return HazeStyle(
        backgroundColor = background,
        blurRadius = 20.dp,
        tints = listOf(HazeTint(solid.copy(alpha = 0.55f)))
    )
}

@Composable
fun Modifier.glassFill(
    glass: Color,
    shape: Shape,
    hazeState: HazeState?
): Modifier {
    val screen = MaterialTheme.colorScheme.background
    if (hazeState == null) {
        return this.background(glass.compositeOver(screen), shape)
    }
    val style = remember(glass, screen) { glassBlurStyle(glass, screen) }
    return this.clip(shape).hazeEffect(state = hazeState, style = style)
}

// ==== COLORS & BORDERS ====
object MorpheColors {
    @Composable
    fun glassContainerColor(selected: Boolean = false): Color {
        val base = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        val alpha = if (selected) 0.55f else 0.35f
        return base.copy(alpha = alpha)
    }

    @Composable
    fun glassBorderColor(selected: Boolean = false): Color {
        return if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
        } else {
            MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        }
    }

    @Composable
    fun glassContentColor(selected: Boolean = false): Color {
        return if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    }
}