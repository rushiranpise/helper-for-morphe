package dev.rushi.apkdownloadhelper.ui.shared.button

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.rushi.apkdownloadhelper.ui.theme.AppAnimations

/**
 * Floating button that pops in and out with the manager's floating-button motion, so it
 * reads as arriving rather than appearing.
 */
@Composable
internal fun AnimatedFab(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = AppAnimations.fabEnter,
        exit = AppAnimations.fabExit
    ) {
        content()
    }
}
