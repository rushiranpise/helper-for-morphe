package dev.rushi.apkdownloadhelper.ui.shared.dialog

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal object AppDialogDefaults {
    /** Gap between the dialog content and the screen edge. */
    val HorizontalInset = 16.dp
    /** Widest the dialog gets (tablets / landscape). */
    val MaxWidth = 480.dp
    /** Gap between icon, title, message and buttons. */
    val SectionSpacing = 16.dp
    /** Gap between the two buttons. */
    val ButtonSpacing = 12.dp

    // ---- Round icon on top of a dialog (Fast Mode bolt, cancel cross) ----
    /** Big circle at the top of the centred popup group. */
    val IconCircleSize = 112.dp
    /** The icon inside that circle. */
    val IconGlyphSize = 56.dp
    /** Extra gap between the circle and the title (added on top of SectionSpacing). */
    val IconBottomSpacing = 8.dp
    /** Opacity of the default icon background (surfaceVariant). */
    const val IconContainerAlpha = 0.65f
    /** Title shrinks down to this size when it is too long for one line. */
    val TitleMinFontSize = 12.sp

    /** THE default icon-circle colours for every popup. */
    @Composable
    @ReadOnlyComposable
    fun defaultIconContainer(): Color =
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = IconContainerAlpha)

    @Composable
    @ReadOnlyComposable
    fun defaultIconTint(): Color = MaterialTheme.colorScheme.onSurfaceVariant

    /** Fill of every right-side (confirm) dialog button: the same colour as the icon circle. */
    @Composable
    @ReadOnlyComposable
    fun defaultButtonContainer(): Color = defaultIconContainer()

    /**
     * THE Morphe button colour: identical to the Cancel / Pause / Switch source buttons in the
     * download card (DownloadPillButton). Default fill of every right-side confirm button.
     */
    @Composable
    @ReadOnlyComposable
    fun confirmButtonContainer(): Color =
        MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)

    // ---- Round selector in option lists (green check / empty ring) ----
    val SelectCircleSize = 24.dp
    val SelectCheckSize = 16.dp
    val SelectCircleBorder = 2.dp

    // ---- Option list (SettingsChoiceDialog) ----
    /** Increase this to show more option cards before the list scrolls. */
    // Very large on purpose: the list grows until the screen ends, behind the Close bar.
    // Lower it (e.g. 480.dp) if you want a shorter list.
    val ChoiceListMaxHeight = 2000.dp
    /** Height of the soft fade where the list scrolls under the title. */
    val TopFadeHeight = 32.dp
    /** Space kept free at the bottom for a pinned bar such as the Close button. */
    val BottomBarHeight = 72.dp
}