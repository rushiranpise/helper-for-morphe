package dev.rushi.apkdownloadhelper.ui.shared.state

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.rushi.apkdownloadhelper.HelperButton
import dev.rushi.apkdownloadhelper.HelperOutlinedButton
import dev.rushi.apkdownloadhelper.ui.shared.card.SurfaceCard
import dev.rushi.apkdownloadhelper.ui.theme.AppDefaults

@Composable
internal fun FailureActions(
    canRetryFailedSources: Boolean,
    onRetryAll: () -> Unit,
    onRetryFailedSources: () -> Unit,
    onCopyDetails: () -> Unit,
    onCancel: () -> Unit
) {
    HelperButton(
        text = "Retry all",
        onClick = onRetryAll,
        modifier = Modifier.fillMaxWidth()
    )
    if (canRetryFailedSources) {
        HelperOutlinedButton(
            text = "Retry failed sources",
            onClick = onRetryFailedSources,
            modifier = Modifier.fillMaxWidth()
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppDefaults.ItemSpacing)
    ) {
        HelperOutlinedButton(
            text = "Copy details",
            onClick = onCopyDetails,
            modifier = Modifier.weight(1f)
        )
        HelperOutlinedButton(
            text = "Cancel",
            onClick = onCancel,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
internal fun ErrorState(
    message: String,
    canRetryFailedSources: Boolean,
    onRetryAll: () -> Unit,
    onRetryFailedSources: () -> Unit,
    onCopyDetails: () -> Unit,
    onCancel: () -> Unit
) {
    SurfaceCard {
        Column(
            modifier = Modifier.padding(AppDefaults.ContentPadding),
            verticalArrangement = Arrangement.spacedBy(AppDefaults.ItemSpacing)
        ) {
            Text(text = message, color = MaterialTheme.colorScheme.error)
            FailureActions(
                canRetryFailedSources = canRetryFailedSources,
                onRetryAll = onRetryAll,
                onRetryFailedSources = onRetryFailedSources,
                onCopyDetails = onCopyDetails,
                onCancel = onCancel
            )
        }
    }
}
