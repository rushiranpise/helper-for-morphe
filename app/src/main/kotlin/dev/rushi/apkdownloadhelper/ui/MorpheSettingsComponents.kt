package dev.rushi.apkdownloadhelper.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Stable
internal class SettingsSearch(private val query: () -> String) {
    val isActive: Boolean get() = query().isNotBlank()
    fun matches(texts: List<String?>): Boolean {
        val q = query().trim()
        return q.isEmpty() || texts.any { it != null && it.contains(q, ignoreCase = true) }
    }
}
internal val LocalSettingsSearch = staticCompositionLocalOf<SettingsSearch?> { null }

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
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    subtitle: String? = null
) {
    val matches = LocalSettingsSearch.current?.matches(listOf(title, subtitle)) ?: true
    AnimatedVisibility(visible = matches) {
        SettingsItem(
            onClick = onToggle,
            title = title,
            modifier = modifier,
            icon = icon,
            leadingContent = leadingContent,
            subtitle = subtitle,
            trailingContent = {
                Switch(
                    checked = checked,
                    onCheckedChange = null,
                    modifier = Modifier.semantics { stateDescription = if (checked) "Enabled" else "Disabled" },
                    thumbContent = { Icon(if (checked) Icons.Outlined.Check else Icons.Outlined.Close, null, Modifier.size(SwitchDefaults.IconSize)) }
                )
            }
        )
    }
}

@Composable
internal fun Footnote(
    text: String,
    modifier: Modifier = Modifier,
    tone: SemanticTone = SemanticTone.Neutral,
    icon: ImageVector = when (tone) { SemanticTone.Error -> Icons.Outlined.Error; SemanticTone.Warning -> Icons.Outlined.Warning; else -> Icons.Outlined.Info }
) {
    Row(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, null, tint = tone.accent, modifier = Modifier.size(20.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = tone.accent, modifier = Modifier.weight(1f))
    }
}