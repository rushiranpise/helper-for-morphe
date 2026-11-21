package dev.rushi.apkdownloadhelper.ui.screen.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.rushi.apkdownloadhelper.ui.shared.layout.AppBackground
import dev.rushi.apkdownloadhelper.ui.shared.navigation.AppNavigationBar

@Composable
internal fun MainPagerScaffold(
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    animatedBackground: Boolean = true,
    navigationCovered: () -> Boolean = { false },
    swipeEnabled: Boolean = true,
    settingsIcon: ImageVector = MainPage.SETTINGS.icon,
    settingsDescription: String? = null,
    overlay: @Composable BoxScope.() -> Unit = {},
    content: @Composable (page: MainPage, bottomBarHeight: Dp) -> Unit,
) {
    val covered = navigationCovered()
    var barHeight by remember { mutableStateOf(0.dp) }
    val barHaze = remember { HazeState() }
    val navBarHeight = with(LocalDensity.current) {
        WindowInsets.navigationBars.getBottom(this).toDp()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (animatedBackground) {
            AppBackground(modifier = Modifier.matchParentSize())
        }

        // 1. Pager content (marked as haze source so it blurs inside the floating pill)
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = swipeEnabled,
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = barHaze),
        ) { pageIndex ->
            content(MainPage.entries[pageIndex], if (covered) 0.dp else barHeight)
        }

        // 2. Telegram-style bottom dim gradient (sits OVER the scrolling list, BEHIND the floating pill)
        AnimatedVisibility(
            visible = !covered,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(navBarHeight + 96.dp)
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Transparent,
                            0.48f to MaterialTheme.colorScheme.background.copy(alpha = 0.55f),
                            0.80f to MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
                            1.0f to MaterialTheme.colorScheme.background.copy(alpha = 0.96f)
                        )
                    )
            )
        }

        // 3. Floating pill drawn ON TOP of the dim scrim so the pill stays bright & crisp
        AppNavigationBar(
            pagerState = pagerState,
            covered = covered,
            hazeState = barHaze,
            modifier = Modifier.align(Alignment.BottomCenter),
            settingsIcon = settingsIcon,
            settingsDescription = settingsDescription,
            onHeightChanged = { barHeight = it },
        )

        overlay()
    }
}
