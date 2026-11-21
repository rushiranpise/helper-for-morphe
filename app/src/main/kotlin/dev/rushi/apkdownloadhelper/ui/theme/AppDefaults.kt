package dev.rushi.apkdownloadhelper.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/*
 * Morphe Manager UI parity layer.
 *
 * Ported from morphe-manager's `ui/theme` and `ui/screen/shared` so the helper's screens
 * are built from the same tokens and components: exact light/dark color values, typography,
 * the shared metric set (radii, heights, touch targets, paddings) and the card / section /
 * info / selector components the manager uses everywhere.
 */
/**
 * Shared metrics, mirroring morphe-manager's `Defaults` in
 * `ui/screen/shared/SettingComponents.kt`.
 */
internal object AppDefaults {
    val CardElevation = 2.dp
    val CardCornerRadius = 16.dp
    val CompactCornerRadius = 12.dp
    val SettingsCornerRadius = 16.dp
    val SectionCornerRadius = 20.dp
    val IconSize = 24.dp
    val IconSizeSmall = 20.dp

    val MinTouchTarget = 48.dp
    val TallTouchTarget = 52.dp

    /** Compact pill holding an icon alone. */
    val PillHeight = 36.dp

    /** Pill that carries a label next to its icon. */
    val PillHeightLarge = 40.dp

    /** Fully rounded shape shared by the pill buttons. */
    val PillShape = RoundedCornerShape(50)

    /** Height of a glass tab or toggle. */
    val GlassButtonHeight = MinTouchTarget

    /** Height of a dialog action button. */
    val DialogButtonHeight = TallTouchTarget

    /** Width that keeps a compact action aligned with a wider icon + label sibling. */
    val CompactButtonWidth = 96.dp

    val ContentPaddingSmall = 8.dp
    val ContentPadding = 16.dp
    val ContentPaddingMedium = 24.dp
    val ContentPaddingExpanded = 32.dp
    val ItemSpacing = 12.dp

    val DefaultGradientColors = listOf(Color(0xFF1E5AA8), Color(0xFF00AFAE))

    const val ANIMATION_DURATION = 220
    const val ANIMATION_DURATION_SHORT = 180
    const val SCREEN_ENTER_DURATION = 320
    const val DIALOG_SCALE = 0.95f
}
