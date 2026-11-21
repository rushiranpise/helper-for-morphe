package dev.rushi.apkdownloadhelper.ui.shared.layout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.rushi.apkdownloadhelper.ui.theme.AppDefaults

/** Shared expand/collapse animation for every collapsible section. */
@Composable
internal fun AnimatedExpand(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = expandVertically(
            animationSpec = tween(AppDefaults.ANIMATION_DURATION),
            expandFrom = Alignment.Top
        ) + fadeIn(animationSpec = tween(AppDefaults.ANIMATION_DURATION)),
        exit = shrinkVertically(
            animationSpec = tween(AppDefaults.ANIMATION_DURATION_SHORT),
            shrinkTowards = Alignment.Top
        ) + fadeOut(animationSpec = tween(AppDefaults.ANIMATION_DURATION_SHORT))
    ) {
        content()
    }
}
