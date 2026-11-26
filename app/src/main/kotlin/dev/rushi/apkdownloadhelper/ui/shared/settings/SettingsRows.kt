package dev.rushi.apkdownloadhelper.ui.shared.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.alpha

@Composable
internal fun SettingsSection(
    title: String,
    icon: ImageVector? = null,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, bottom = 2.dp).semantics { heading() },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (icon != null) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
        }
        content()
    }
}

@Composable
internal fun SettingsSwitchItem(
    title: String,
    checked: Boolean,
    onToggle: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    subtitle: String? = null
) {
    val matches = LocalSettingsSearch.current?.matches(listOf(title, subtitle)) ?: true
    AnimatedVisibility(visible = matches) {
        SettingsItem(
            onClick = { if (enabled) onToggle() },
            title = title,
            // 1. Fades out the entire row (icon, text, and switch) when disabled
            modifier = modifier.alpha(if (enabled) 1f else 0.35f),
            icon = icon,
            leadingContent = leadingContent,
            subtitle = subtitle,
            trailingContent = {
                Switch(
                    checked = checked,
                    onCheckedChange = null,
                    enabled = enabled,
                    modifier = Modifier.semantics { stateDescription = if (checked) "Enabled" else "Disabled" },
                    thumbContent = { Icon(if (checked) Icons.Outlined.Check else Icons.Outlined.Close, null, Modifier.size(SwitchDefaults.IconSize)) }
                )
            }
        )
    }
}
