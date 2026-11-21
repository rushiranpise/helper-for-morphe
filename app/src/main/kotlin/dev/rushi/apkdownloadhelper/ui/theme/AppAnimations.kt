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
     * Push transitions: a screen slides up over the one below it and returns by sliding back
     * down, which is how the manager brings Settings forward.
     */
    val pushEnter: EnterTransition = slideInVertically(
        animationSpec = defaultTween(AppDefaults.SCREEN_ENTER_DURATION, FastOutSlowInEasing)
    ) { it } + fadeIn(defaultTween(AppDefaults.SCREEN_ENTER_DURATION))

    val pushExit: ExitTransition = slideOutVertically(
        animationSpec = defaultTween(AppDefaults.SCREEN_ENTER_DURATION, FastOutSlowInEasing)
    ) { it } + fadeOut(tween(AppDefaults.SCREEN_ENTER_DURATION, easing = LinearEasing))

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
