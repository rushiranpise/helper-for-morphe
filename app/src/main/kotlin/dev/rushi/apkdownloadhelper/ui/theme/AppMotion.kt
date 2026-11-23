package dev.rushi.apkdownloadhelper.ui.theme

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ==== ANIMATIONS ====
object AppSprings {
    fun <T> listSpring(dampingRatio: Float = 0.8f): androidx.compose.animation.core.SpringSpec<T> =
        spring(stiffness = 400f, dampingRatio = dampingRatio)
}

// ==== PRESS FEEDBACK (CORNER MORPH ONLY, NO SIZE SHRINK) ====
const val PressedScale = 1f

@Composable
fun Modifier.pressScale(
    interactionSource: InteractionSource,
    enabled: Boolean = true,
    pressedScale: Float = PressedScale,
    restCorner: Dp = 28.dp,
    pressedCorner: Dp = 12.dp,
): Modifier = this

@Composable
fun rememberMorphePillShape(
    interactionSource: InteractionSource,
    enabled: Boolean = true,
    pressedCorner: Dp = 12.dp,
): CornerBasedShape {
    val isPressed by interactionSource.collectIsPressedAsState()
    val corner by animateDpAsState(
        targetValue = if (enabled && isPressed) pressedCorner else 28.dp,
        animationSpec = spring(
            dampingRatio = 0.7f,
            stiffness = 800f
        ),
        label = "morphe_pill_corner"
    )
    return RoundedCornerShape(corner)
}