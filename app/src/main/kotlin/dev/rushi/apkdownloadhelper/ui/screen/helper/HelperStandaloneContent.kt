package dev.rushi.apkdownloadhelper.ui.screen.helper

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.rushi.apkdownloadhelper.storage.HelperSettings
import dev.rushi.apkdownloadhelper.ui.shared.scroll.verticalScrollFade
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
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .verticalScrollFade(listState)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = AppDefaults.ContentPadding, vertical = AppDefaults.ContentPadding),
        verticalArrangement = Arrangement.spacedBy(AppDefaults.ItemSpacing)
    ) {
        item {
            HelperTopHeader(
                settings = settings,
                onSettingsChange = onSettingsChange
            )
        }
        item {
            EmptyLaunchState(onOpenMorphe = onOpenMorphe)
        }
    }
}