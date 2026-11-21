package dev.rushi.apkdownloadhelper.ui.util

import androidx.compose.foundation.pager.PagerState
import dev.rushi.apkdownloadhelper.ui.screen.home.MainPage

internal suspend fun PagerState.turnTo(page: MainPage) {
    if (currentPage != page.ordinal) {
        animateScrollToPage(page.ordinal)
    }
}
