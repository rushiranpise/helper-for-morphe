package dev.rushi.apkdownloadhelper.ui.screen.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.Dp
import dev.rushi.apkdownloadhelper.ui.util.turnTo
import kotlinx.coroutines.launch
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

@Composable
internal fun HomePagerScreen(
    requestResetToken: Int,
    hideFindApps: Boolean = false,
    animatedBackground: Boolean = true,
    findAppsContent: @Composable (bottomBarHeight: Dp) -> Unit,
    homeContent: @Composable (bottomBarHeight: Dp) -> Unit,
    settingsContent: @Composable (bottomBarHeight: Dp) -> Unit,
    navigationCovered: () -> Boolean,
    overlay: @Composable BoxScope.() -> Unit = {}
) {
    val visiblePages = remember(hideFindApps) {
        if (hideFindApps) MainPage.entries.filter { it != MainPage.FIND_APPS }
        else MainPage.entries.toList()
    }

    // Remember which page the user was on, independently of its index. When
    // the visible list changes (Find apps hidden/unhidden) the pager snaps
    // back to the SAME page instead of silently shifting to a neighbour.
    var currentMainPage by rememberSaveable { mutableStateOf(MainPage.HOME) }

    val pagerState = rememberPagerState(
        initialPage = visiblePages.indexOf(currentMainPage).coerceAtLeast(0)
    ) { visiblePages.size }
    val scope = rememberCoroutineScope()

    // Pager -> saved identity
    LaunchedEffect(pagerState.currentPage, visiblePages) {
        visiblePages.getOrNull(pagerState.currentPage)?.let { currentMainPage = it }
    }

    // Visible list changed (hideFindApps toggled): snap back to the saved
    // page, or HOME if it disappeared.
    LaunchedEffect(hideFindApps) {
        val idx = visiblePages.indexOf(currentMainPage).let {
            if (it >= 0) it else visiblePages.indexOf(MainPage.HOME).coerceAtLeast(0)
        }
        if (idx >= 0 && pagerState.currentPage != idx) pagerState.scrollToPage(idx)
    }

    LaunchedEffect(requestResetToken) {
        if (requestResetToken > 0) {
            val homeIdx = visiblePages.indexOf(MainPage.HOME)
            if (homeIdx >= 0) pagerState.scrollToPage(homeIdx)
        }
    }

    BackHandler(enabled = pagerState.currentPage != visiblePages.indexOf(MainPage.HOME)) {
        visiblePages.indexOf(MainPage.HOME)
            .takeIf { it >= 0 }
            ?.let { scope.launch { pagerState.scrollToPage(it) } }
    }

    MainPagerScaffold(
        pagerState = pagerState,
        visiblePages = visiblePages,
        animatedBackground = animatedBackground,
        navigationCovered = navigationCovered,
        swipeEnabled = !navigationCovered(),
        overlay = overlay
    ) { page, bottomBarHeight ->
        when (page) {
            MainPage.FIND_APPS -> findAppsContent(bottomBarHeight)
            MainPage.HOME -> homeContent(bottomBarHeight)
            MainPage.SETTINGS -> settingsContent(bottomBarHeight)
        }
    }
}