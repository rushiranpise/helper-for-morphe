package dev.rushi.apkdownloadhelper.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ---------------------------------------------------------------------------
// Semantic tones and status badges  ports of the manager's `StatusBadge.kt`
// ---------------------------------------------------------------------------
/**
 * Semantic colour roles shared by everything that carries a tint: badges, notices and status
 * rows. One definition, so the same meaning cannot read as two different colours in two
 * screens.
 */
internal enum class SemanticTone {
    Neutral,
    Primary,
    Success,
    Warning,
    Error;

    /** Background of a filled element in this role. */
    val container: Color
        @Composable get() = when (this) {
            Neutral -> MaterialTheme.colorScheme.surfaceVariant
            Primary -> MaterialTheme.colorScheme.primaryContainer
            Success -> MaterialTheme.colorScheme.tertiaryContainer
            Warning -> MaterialTheme.colorScheme.secondaryContainer
            Error -> MaterialTheme.colorScheme.errorContainer
        }

    /** Content drawn on top of [container]. */
    val content: Color
        @Composable get() = when (this) {
            Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
            Primary -> MaterialTheme.colorScheme.onPrimaryContainer
            Success -> MaterialTheme.colorScheme.onTertiaryContainer
            Warning -> MaterialTheme.colorScheme.onSecondaryContainer
            Error -> MaterialTheme.colorScheme.onErrorContainer
        }

    /** Standalone colour for text or icons carrying the role without a filled background. */
    val accent: Color
        @Composable get() = when (this) {
            Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
            Primary -> MaterialTheme.colorScheme.primary
            Success -> MaterialTheme.colorScheme.tertiary
            Warning -> MaterialTheme.colorScheme.secondary
            Error -> MaterialTheme.colorScheme.error
        }
}
