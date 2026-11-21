package dev.rushi.apkdownloadhelper.ui.shared.source

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.rushi.apkdownloadhelper.R
import dev.rushi.apkdownloadhelper.ui.shared.button.SourceSquareButton
import dev.rushi.apkdownloadhelper.ui.shared.card.SurfaceCard
import dev.rushi.apkdownloadhelper.ui.shared.icon.ThemedIcon
import dev.rushi.apkdownloadhelper.ui.shared.layout.AppDivider
import dev.rushi.apkdownloadhelper.ui.model.DownloadSource
import dev.rushi.apkdownloadhelper.ui.theme.AppDefaults
import dev.rushi.apkdownloadhelper.ui.theme.SemanticTone
import dev.rushi.apkdownloadhelper.ui.theme.pressScale

@Composable
internal fun SelectedSourceBar(source: DownloadSource) {
    SurfaceCard(
        cornerRadius = AppDefaults.CardCornerRadius,
        borderWidth = 1.5.dp,
        borderColor = SemanticTone.Primary.accent.copy(alpha = 0.65f),
        color = SemanticTone.Primary.container.copy(alpha = 0.4f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SourceAvatar(source = source, size = 32.dp)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = stringResource(R.string.selected_source),
                    color = SemanticTone.Primary.content.copy(alpha = 0.75f),
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = source.label,
                    fontWeight = FontWeight.Bold,
                    color = SemanticTone.Primary.content,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            ThemedIcon(
                icon = Icons.Outlined.CheckCircle,
                contentDescription = stringResource(R.string.selected),
                size = 22.dp
            )
        }
    }
}

@Composable
internal fun SourceBottomBar(
    action: PrimaryAction,
    onRefresh: () -> Unit,
    onCancel: () -> Unit
) {
    Column {
        AppDivider(fullWidth = true)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = AppDefaults.ContentPadding, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SourceSquareButton(
                icon = Icons.Outlined.Refresh,
                contentDescription = stringResource(R.string.refresh),
                onClick = onRefresh
            )
            SourceSquareButton(
                icon = Icons.Outlined.Close,
                contentDescription = stringResource(R.string.cancel),
                onClick = onCancel
            )
            val primaryInteractionSource = remember { MutableInteractionSource() }
            Button(
                onClick = action.run,
                enabled = action.enabled,
                modifier = Modifier
                    .weight(1f)
                    .height(AppDefaults.DialogButtonHeight)
                    .pressScale(primaryInteractionSource, enabled = action.enabled),
                interactionSource = primaryInteractionSource,
                shape = RoundedCornerShape(AppDefaults.CardCornerRadius),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.48f)),
                contentPadding = PaddingValues(horizontal = AppDefaults.ContentPadding)
            ) {
                if (action.loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = action.icon,
                        contentDescription = null,
                        modifier = Modifier.size(AppDefaults.IconSizeSmall)
                    )
                }
                Spacer(Modifier.width(AppDefaults.ContentPaddingSmall))
                Text(
                    action.label,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
