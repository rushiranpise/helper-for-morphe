package dev.rushi.apkdownloadhelper.ui.util

import androidx.compose.foundation.pager.PagerState
import dev.rushi.apkdownloadhelper.ui.screen.home.MainPage
import kotlin.math.abs

internal suspend fun PagerState.turnTo(page: MainPage) {
    val distance = page.ordinal - currentPage
    if (distance == 0) return
    if (abs(distance) > 1) {
        scrollToPage(page.ordinal)
    } else {
        animateScrollToPage(page.ordinal)
    }
}