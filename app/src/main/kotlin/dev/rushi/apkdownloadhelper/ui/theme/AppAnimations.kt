package dev.rushi.apkdownloadhelper.ui.theme

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** True inside a [SettingsGroup], where rows share the group's single card surface. */
// ---------------------------------------------------------------------------
// Motion  ported from morphe-manager's ui/screen/shared/Animations.kt
// ---------------------------------------------------------------------------
/**
 * Motion set shared by every transition in the helper, so dialogs, pushed screens, overlays
 * and the floating button all move like the manager's. Durations and scales come from
 * [AppDefaults], so retuning a token retimes the whole app at once.
 */
internal object AppAnimations {
    /** Shared easing helper, mirroring the manager's `defaultTween`. */
    private fun <T> defaultTween(
        duration: Int = AppDefaults.ANIMATION_DURATION,
        easing: Easing = LinearOutSlowInEasing
    ) = tween<T>(duration, easing = easing)

    val fade: EnterTransition = fadeIn(animationSpec = defaultTween())
    val fadeOut: ExitTransition = fadeOut(animationSpec = defaultTween())

    /** Dialog scale-fade, used by [AppDialog]. */
    val dialogEnter: EnterTransition = fadeIn(animationSpec = defaultTween()) +
        scaleIn(
            initialScale = AppDefaults.DIALOG_SCALE,
            animationSpec = defaultTween(easing = FastOutSlowInEasing)
        )
    val dialogExit: ExitTransition = fadeOut(animationSpec = defaultTween()) +
        scaleOut(
            targetScale = AppDefaults.DIALOG_SCALE,
            animationSpec = defaultTween()
        )

    /** Overlays fade without scaling. */
    val overlayEnter: EnterTransition = fadeIn(animationSpec = defaultTween())
    val overlayExit: ExitTransition = fadeOut(animationSpec = defaultTween())

    /** Screen swap that scales in place, for content replacing content. */
    val screenEnter: EnterTransition = fadeIn(defaultTween(AppDefaults.SCREEN_ENTER_DURATION)) +
        scaleIn(
            initialScale = AppDefaults.DIALOG_SCALE,
            animationSpec = defaultTween(AppDefaults.SCREEN_ENTER_DURATION, FastOutSlowInEasing)
        )
    val screenExit: ExitTransition = dialogExit

    /**
     * Popup scale-fade transitions for Settings subpages and overlays (replaces bottom slide-up).
     */
    val pushEnter: EnterTransition = fadeIn(
        animationSpec = tween(220, easing = LinearOutSlowInEasing)
    ) + scaleIn(
        initialScale = 0.92f,
        animationSpec = tween(240, easing = FastOutSlowInEasing)
    )

    val pushExit: ExitTransition = fadeOut(
        animationSpec = tween(160, easing = LinearEasing)
    ) + scaleOut(
        targetScale = 0.95f,
        animationSpec = tween(180, easing = FastOutSlowInEasing)
    )

    /** Vertical expand/collapse paired with a fade, for collapsible sections. */
    val expandFadeEnter: EnterTransition = expandVertically(defaultTween()) + fadeIn(defaultTween())
    val shrinkFadeExit: ExitTransition = shrinkVertically(defaultTween()) + fadeOut(defaultTween())

    /**
     * Floating button (scroll-to-top): pops in from below with a stronger scale than a dialog,
     * so it reads as arriving rather than appearing.
     */
    val fabEnter: EnterTransition = fadeIn(defaultTween()) +
        scaleIn(defaultTween(), initialScale = 0.85f) +
        slideInVertically(defaultTween()) { it / 2 }
    val fabExit: ExitTransition = fadeOut(defaultTween()) +
        scaleOut(defaultTween(), targetScale = 0.85f) +
        slideOutVertically(defaultTween()) { it / 2 }

    /** Spring-driven slide-up for bars dropping in from the top edge. */
    val springSlideUpEnter: EnterTransition = slideInVertically(
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        initialOffsetY = { it }
    ) + fadeIn(tween(AppDefaults.ANIMATION_DURATION_SHORT))

    /**
     * Slide-fade content swap for [androidx.compose.animation.AnimatedContent]: counters,
     * labels and status text. Asymmetric durations read as snappier than a symmetric crossfade.
     */
    fun slideTransitionSpec(
        enterDuration: Int = 200,
        exitDuration: Int = 150,
        offset: (Int) -> Int = { -it / 2 }
    ): AnimatedContentTransitionScope<*>.() -> ContentTransform = {
        (fadeIn(tween(enterDuration)) + slideInVertically(tween(enterDuration)) { offset(it) })
            .togetherWith(fadeOut(tween(exitDuration)) + slideOutVertically(tween(exitDuration)) { -offset(it) })
    }

    /** Plain crossfade, for content that swaps in step with a state change. */
    fun fadeCrossfade(
        duration: Int = AppDefaults.ANIMATION_DURATION
    ): AnimatedContentTransitionScope<*>.() -> ContentTransform = {
        fadeIn(tween(duration)) togetherWith fadeOut(tween(duration))
    }
}

/**
 * Placement and fade animation for a lazy list row, so a list that filters, folds or reorders
 * settles instead of jumping. Kept next to [AppAnimations] so list and dialog motion stay
 * in step.
 */
@Composable
internal fun Modifier.animatedListItem(itemScope: LazyItemScope): Modifier = with(itemScope) {
    this@animatedListItem.animateItem(
        fadeInSpec = tween(AppDefaults.ANIMATION_DURATION),
        fadeOutSpec = tween(AppDefaults.ANIMATION_DURATION_SHORT),
        placementSpec = spring(stiffness = 400f, dampingRatio = 0.8f)
    )
}

/**
 * Morphe Manager-style expressive tap animation:
 * compresses scale and morphs corner radius from pill/circle to a rounded squircle on press.
 */
fun Modifier.morphePressShape(
    interactionSource: MutableInteractionSource,
    restCorner: Dp = 28.dp,
    pressedCorner: Dp = 12.dp,
    pressedScale: Float = 0.92f,
): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 900f),
        label = "morphePressScale",
    )
    val corner by animateDpAsState(
        targetValue = if (pressed) pressedCorner else restCorner,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 900f),
        label = "morphePressCorner",
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clip(RoundedCornerShape(corner))
}