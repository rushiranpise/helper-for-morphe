package dev.rushi.apkdownloadhelper.ui.screen.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import dev.rushi.apkdownloadhelper.ui.util.turnTo
import kotlinx.coroutines.launch

@Composable
internal fun HomePagerScreen(
    requestResetToken: Int,
    openTrackingToken: Int = 0,
    animatedBackground: Boolean = true,
    findAppsContent: @Composable (bottomBarHeight: Dp) -> Unit,
    trackingContent: @Composable (bottomBarHeight: Dp) -> Unit,
    homeContent: @Composable (bottomBarHeight: Dp) -> Unit,
    requestsContent: @Composable (bottomBarHeight: Dp) -> Unit,
    settingsContent: @Composable (bottomBarHeight: Dp) -> Unit,
    navigationCovered: () -> Boolean,
    overlay: @Composable BoxScope.() -> Unit = {}
) {
    val pagerState = rememberPagerState(initialPage = MainPage.HOME.ordinal) { MainPage.entries.size }
    val scope = rememberCoroutineScope()

    LaunchedEffect(requestResetToken) {
        if (requestResetToken > 0) {
            pagerState.turnTo(MainPage.HOME)
        }
    }

    LaunchedEffect(openTrackingToken) {
        if (openTrackingToken > 0) {
            pagerState.turnTo(MainPage.TRACKING)
        }
    }

    BackHandler(enabled = pagerState.currentPage != MainPage.HOME.ordinal) {
        scope.launch { pagerState.turnTo(MainPage.HOME) }
    }

    MainPagerScaffold(
        pagerState = pagerState,
        animatedBackground = animatedBackground,
        navigationCovered = navigationCovered,
        swipeEnabled = !navigationCovered(),
        overlay = overlay,
        requestsOverlay = requestsContent
    ) { page, bottomBarHeight ->
        when (page) {
            MainPage.FIND_APPS -> findAppsContent(bottomBarHeight)
            MainPage.TRACKING -> trackingContent(bottomBarHeight)
            MainPage.HOME -> homeContent(bottomBarHeight)
            // The board is hosted by MainPagerScaffold above the pager so the
            // navigation bar's blur never captures its WebView.
            MainPage.REQUESTS -> Box(Modifier.fillMaxSize())
            MainPage.SETTINGS -> settingsContent(bottomBarHeight)
        }
    }
}
