package dev.rushi.apkdownloadhelper.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
internal fun MainPagerScaffold(
    pagerState: PagerState,
    navigationCovered: () -> Boolean,
    swipeEnabled: Boolean = true,
    overlay: @Composable BoxScope.() -> Unit = {},
    pageContent: @Composable (page: MainPage, bottomBarHeight: Dp) -> Unit
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    var measuredBarHeight by remember { mutableStateOf(80.dp) }
    val isCovered = navigationCovered()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            AnimatedVisibility(
                visible = !isCovered,
                enter = slideInVertically { it },
                exit = slideOutVertically { it }
            ) {
                NavigationBar(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .onSizeChanged { size ->
                            measuredBarHeight = with(density) { size.height.toDp() }
                        },
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 3.dp
                ) {
                    MainPage.entries.forEach { page ->
                        val selected = pagerState.currentPage == page.ordinal
                        NavigationBarItem(
                            selected = selected,
                            onClick = { scope.launch { pagerState.turnTo(page) } },
                            icon = { Icon(page.icon, contentDescription = page.label) },
                            label = { Text(page.label) },
                            alwaysShowLabel = true
                        )
                    }
                }
            }
        }
    ) { _ ->
        Box(modifier = Modifier.fillMaxSize()) {
            val effectiveBottomBar = if (isCovered) 0.dp else measuredBarHeight
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = swipeEnabled,
                modifier = Modifier.fillMaxSize()
            ) { index ->
                pageContent(MainPage.entries[index], effectiveBottomBar)
            }
            overlay()
        }
    }
}
