package dev.rushi.apkdownloadhelper.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

/**
 * Central style tokens for the shared components. Change a colour, shape or size here and every
 * screen that uses the component (home, settings, updates, dialogs...) follows.
 */
internal object SearchFieldStyle {
    val Height = 40.dp
    val HorizontalPadding = 14.dp
    val IconSize = 20.dp
    val Shape: Shape = CircleShape

    @Composable
    fun containerColor(): Color {
        val isDark = MaterialTheme.colorScheme.background.luminance() < 0.05f
        return MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.35f else 0.6f)
    }

    @Composable
    fun placeholderColor(): Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
}

/** Colours for the trailing value shown on the right side of a settings row. */
internal object SettingsValueStyle {
    @Composable
    fun color(): Color = MaterialTheme.colorScheme.primary

    @Composable
    fun destructiveColor(): Color = MaterialTheme.colorScheme.error
}

/** Look shared by every alert dialog (update, storage, picker, settings choice...). */
internal object AlertDialogStyle {
    @Composable
    fun containerColor(): Color = MaterialTheme.colorScheme.background
}
