package dev.rushi.apkdownloadhelper.ui.shared.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.rushi.apkdownloadhelper.ui.shared.icon.ThemedIcon
import dev.rushi.apkdownloadhelper.ui.shared.selection.RadioDot
import dev.rushi.apkdownloadhelper.ui.shared.source.SourceAvatar
import dev.rushi.apkdownloadhelper.ui.model.DownloadSource
import dev.rushi.apkdownloadhelper.ui.theme.AppDefaults
import dev.rushi.apkdownloadhelper.ui.theme.pressScale

@Composable
internal fun SettingsOptionCard(
    icon: ImageVector?,
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
    source: DownloadSource? = null
) {
    val colors = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(interactionSource, enabled = enabled, pressedScale = 0.97f, restCorner = 16.dp, pressedCorner = 10.dp)
            .clip(RoundedCornerShape(AppDefaults.SettingsCornerRadius))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        shape = RoundedCornerShape(AppDefaults.SettingsCornerRadius),
        color = if (selected) colors.surfaceVariant else colors.surfaceColorAtElevation(3.dp),
        contentColor = colors.onSurface,
        tonalElevation = if (selected) 0.dp else 1.dp,
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(AppDefaults.ContentPadding), horizontalArrangement = Arrangement.spacedBy(AppDefaults.ItemSpacing), verticalAlignment = Alignment.CenterVertically) {
            if (source != null) SourceAvatar(source = source, size = AppDefaults.IconSize)
            else if (icon != null) ThemedIcon(icon = icon, size = AppDefaults.IconSize, tint = if (selected) colors.primary else colors.onSurfaceVariant)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = colors.onSurface)
                Text(description, color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            }
            RadioDot(selected = selected)
        }
    }
}
