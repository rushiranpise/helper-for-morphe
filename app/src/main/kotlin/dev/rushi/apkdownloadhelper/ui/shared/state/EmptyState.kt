package dev.rushi.apkdownloadhelper.ui.shared.state

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.rushi.apkdownloadhelper.HelperButton
import dev.rushi.apkdownloadhelper.R
import dev.rushi.apkdownloadhelper.ui.shared.card.HelperSetupCard
import dev.rushi.apkdownloadhelper.ui.theme.AppDefaults

@Composable
internal fun EmptyLaunchState(
    onOpenMorphe: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDefaults.ItemSpacing)) {
        EmptyState(
            message = stringResource(R.string.empty_launch_message),
            icon = Icons.Outlined.Download
        )
        HelperSetupCard()
        HelperButton(
            text = "Open Morphe Manager",
            onClick = onOpenMorphe,
            icon = Icons.Outlined.OpenInNew,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** Centred empty state with an oversized icon and optional action. */
@Composable
internal fun EmptyState(
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = Icons.Outlined.FolderOff,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            OutlinedButton(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}
