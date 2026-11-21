package dev.rushi.apkdownloadhelper.ui.screen.helper

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.rushi.apkdownloadhelper.storage.HelperSettings
import dev.rushi.apkdownloadhelper.ui.shared.state.EmptyLaunchState
import dev.rushi.apkdownloadhelper.ui.theme.AppDefaults

//==================================================
// Normal App Open
//==================================================

@Composable
internal fun HelperStandaloneContent(
    settings: HelperSettings,
    onSettingsChange: (HelperSettings) -> Unit,
    onOpenMorphe: () -> Unit
) {
    val listState = rememberLazyListState()
    val headerHaze = remember { HazeState() }
    var headerHeightDp by remember { mutableStateOf(0.dp) }
    val isScrolled by remember(listState) {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = headerHaze),
            contentPadding = PaddingValues(
                top = headerHeightDp + 10.dp,
                start = AppDefaults.ContentPadding,
                end = AppDefaults.ContentPadding,
                bottom = 160.dp
            ),
            verticalArrangement = Arrangement.spacedBy(AppDefaults.ItemSpacing)
        ) {
            item {
                EmptyLaunchState(onOpenMorphe = onOpenMorphe)
            }
        }

        HelperFloatingHeader(
            settings = settings,
            onSettingsChange = onSettingsChange,
            isScrolled = isScrolled,
            headerHaze = headerHaze,
            onHeaderHeightChanged = { headerHeightDp = it },
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}