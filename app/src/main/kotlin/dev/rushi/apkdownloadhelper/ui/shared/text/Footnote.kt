package dev.rushi.apkdownloadhelper.ui.shared.text

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import dev.rushi.apkdownloadhelper.ui.theme.SemanticTone

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
