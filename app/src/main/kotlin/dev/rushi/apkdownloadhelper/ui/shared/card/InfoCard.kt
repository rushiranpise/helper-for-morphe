package dev.rushi.apkdownloadhelper.ui.shared.card

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * Informational paragraph  the manager's `InfoBox` container, kept as the helper's one-line
 * note component.
 */
@Composable
internal fun InfoCard(text: String) {
    InfoBox(
        title = text,
        titleColor = MaterialTheme.colorScheme.onSurfaceVariant
    ) {}
}
