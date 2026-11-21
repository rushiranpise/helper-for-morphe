package dev.rushi.apkdownloadhelper.ui.shared.state

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.rushi.apkdownloadhelper.ui.shared.loading.ShimmerListRow
import dev.rushi.apkdownloadhelper.ui.theme.AppDefaults

@Composable
internal fun LoadingState() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppDefaults.ItemSpacing)
    ) {
        repeat(3) { ShimmerListRow() }
    }
}
