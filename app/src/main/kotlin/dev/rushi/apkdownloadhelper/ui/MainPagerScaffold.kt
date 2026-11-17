package dev.rushi.apkdownloadhelper.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.HazeState

@Composable
internal fun MainPagerScaffold(
    pagerState: PagerState,
    modifier: Modifier = Modifier,
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        MorpheBackground(modifier = Modifier.matchParentSize())

        // Mark the pager as the haze source so scrolling content blurs under the pill.
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = swipeEnabled,
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = barHaze),
        ) { pageIndex ->
            content(MainPage.entries[pageIndex], if (covered) 0.dp else barHeight)
        }

        // Pass barHaze into MorpheNavigationBar so glassFill blurs the pager behind it.
        MorpheNavigationBar(
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