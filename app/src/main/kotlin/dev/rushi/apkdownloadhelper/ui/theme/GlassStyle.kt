package dev.rushi.apkdownloadhelper.ui.theme

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

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
