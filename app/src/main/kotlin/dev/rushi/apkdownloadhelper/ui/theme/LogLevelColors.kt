package dev.rushi.apkdownloadhelper.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import dev.rushi.apkdownloadhelper.storage.LogLevel

@Composable
internal fun LogLevel.color(): Color = when (this) {
    LogLevel.Info -> MaterialTheme.colorScheme.onSurfaceVariant
    LogLevel.Warning -> SemanticTone.Warning.accent
    LogLevel.Error -> MaterialTheme.colorScheme.error
}
