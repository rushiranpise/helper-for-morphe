package dev.rushi.apkdownloadhelper.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ==== COLORS & BORDERS ====
object AppColors {
    @Composable
    fun glassContainerColor(selected: Boolean = false): Color {
        val base = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        val alpha = if (selected) 0.55f else 0.35f
        return base.copy(alpha = alpha)
    }

    @Composable
    fun glassBorderColor(selected: Boolean = false): Color {
        return if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
        } else {
            MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        }
    }

    @Composable
    fun glassContentColor(selected: Boolean = false): Color {
        return if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    }
}
