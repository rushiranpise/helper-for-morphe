package dev.rushi.apkdownloadhelper.ui.shared.dialog

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * @param confirmDestructive true for "Stop" (red icon + red text); false for a plain "Cancel"
 * (default icon colour, normal text).
 */
@Composable
internal fun CancelDownloadDialog(
    title: String = "Stop download?",
    message: String = "The current download progress will be discarded.",
    confirmLabel: String = "Stop",
    confirmDestructive: Boolean = true,
    onKeepDownloading: () -> Unit,
    onConfirmStop: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    AppConfirmDialog(
        icon = Icons.Outlined.Close,
        iconContainerColor = if (confirmDestructive) scheme.errorContainer else AppDialogDefaults.defaultIconContainer(),
        iconTint = if (confirmDestructive) scheme.onErrorContainer else AppDialogDefaults.defaultIconTint(),
        title = title,
        message = message,
        dismissText = "Keep downloading",
        confirmText = confirmLabel,
        destructiveConfirm = confirmDestructive,
        onDismiss = onKeepDownloading,
        onConfirm = onConfirmStop
    )
}