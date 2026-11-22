package dev.rushi.apkdownloadhelper.ui.shared.state

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.rushi.apkdownloadhelper.R
import dev.rushi.apkdownloadhelper.ui.shared.card.HelperSetupCard
import dev.rushi.apkdownloadhelper.ui.theme.AppDefaults
import dev.rushi.apkdownloadhelper.ui.theme.pressScale
import androidx.compose.material3.Button

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
        val openMorpheInteraction = remember { MutableInteractionSource() }
        Button(
            onClick = onOpenMorphe,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .pressScale(openMorpheInteraction),
            interactionSource = openMorpheInteraction,
            shape = CircleShape,
            border = null,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.OpenInBrowser,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Open Morphe Manager",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
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
