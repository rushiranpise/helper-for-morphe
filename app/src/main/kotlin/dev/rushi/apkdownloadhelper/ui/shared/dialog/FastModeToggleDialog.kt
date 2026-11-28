package dev.rushi.apkdownloadhelper.ui.shared.dialog

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
internal fun FastModeToggleDialog(
    enable: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    AppConfirmDialog(
        icon = Icons.Outlined.Bolt,
        iconContainerColor = if (enable) scheme.primaryContainer else scheme.surfaceVariant.copy(alpha = 0.65f),
        iconTint = if (enable) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
        title = if (enable) "Switch to Fast Mode?" else "Turn off Fast Mode?",
        message = if (enable) {
            "Current background actions will stop and Fast Mode will immediately search enabled sources for this app."
        } else {
            "Current Fast Mode search or download will stop and switch back to manual source selection."
        },
        dismissText = "Cancel",
        confirmText = if (enable) "Start Fast Mode" else "Turn off Fast Mode",
        onDismiss = onDismiss,
        onConfirm = onConfirm
    )
}