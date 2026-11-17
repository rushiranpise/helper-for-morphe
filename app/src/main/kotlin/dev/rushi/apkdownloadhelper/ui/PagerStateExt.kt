package dev.rushi.apkdownloadhelper.ui

import androidx.compose.foundation.pager.PagerState

internal suspend fun PagerState.turnTo(page: MainPage) {
    if (currentPage != page.ordinal) {
        animateScrollToPage(page.ordinal)
    }
}
