package dev.rushi.apkdownloadhelper.ui.shared.dialog

import androidx.compose.runtime.Composable
import dev.rushi.apkdownloadhelper.ui.shared.source.SourcePickerState

@Composable
internal fun SourcePickerDialog(state: SourcePickerState) {
    if (!state.sourcesExpanded || state.groups.isEmpty()) return

    SettingsChoiceDialog(
        title = "Select download source",
        choices = state.groups.map { it.source.toSettingsChoice() },
        selectedIndex = state.currentPage,
        onSelect = { state.onSelectPage(it) },
        onDismiss = { state.onSourcesExpandedChange(false) }
    )
}