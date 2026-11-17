package dev.rushi.apkdownloadhelper.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.launch

@Composable
internal fun HomePagerScreen(
    findAppsContent: @Composable (bottomBarHeight: Dp) -> Unit,
    homeContent: @Composable (bottomBarHeight: Dp, onFindApps: () -> Unit) -> Unit,
    settingsContent: @Composable (bottomBarHeight: Dp) -> Unit,
    navigationCovered: () -> Boolean,
    overlay: @Composable BoxScope.() -> Unit = {}
) {
    val pagerState = rememberPagerState(initialPage = MainPage.HOME.ordinal) { MainPage.entries.size }
    val scope = rememberCoroutineScope()

    BackHandler(enabled = pagerState.currentPage != MainPage.HOME.ordinal) {
        scope.launch { pagerState.turnTo(MainPage.HOME) }
    }

    MainPagerScaffold(
        pagerState = pagerState,
        navigationCovered = navigationCovered,
        overlay = overlay
    ) { page, bottomBarHeight ->
        when (page) {
            MainPage.FIND_APPS -> findAppsContent(bottomBarHeight)
            MainPage.HOME -> homeContent(bottomBarHeight) {
                scope.launch { pagerState.animateScrollToPage(MainPage.FIND_APPS.ordinal) }
            }
            MainPage.SETTINGS -> settingsContent(bottomBarHeight)
        }
    }
}
