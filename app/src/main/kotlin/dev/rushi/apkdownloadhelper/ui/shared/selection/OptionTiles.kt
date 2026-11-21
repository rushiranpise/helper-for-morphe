package dev.rushi.apkdownloadhelper.ui.shared.selection

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.rushi.apkdownloadhelper.storage.ThemeMode
import dev.rushi.apkdownloadhelper.storage.ThemeStyle
import dev.rushi.apkdownloadhelper.ui.shared.button.ButtonGroupRow
import dev.rushi.apkdownloadhelper.ui.shared.button.buttonGroupItem
import dev.rushi.apkdownloadhelper.ui.shared.card.SurfaceCard
import dev.rushi.apkdownloadhelper.ui.shared.shape.PillGroupShapes
import dev.rushi.apkdownloadhelper.ui.shared.shape.animateCornersAsState
import dev.rushi.apkdownloadhelper.ui.shared.shape.rememberPressShape
import dev.rushi.apkdownloadhelper.ui.screen.settings.icon
import dev.rushi.apkdownloadhelper.ui.theme.AppColors
import dev.rushi.apkdownloadhelper.ui.theme.AppDefaults
import dev.rushi.apkdownloadhelper.ui.theme.SelectedTileBorder

// =====================================================================================
// Theme mode + Color style selector (ported from morphe-manager's ThemeSelector,
// OptionTiles, SelectionTile, ButtonGroupRow, PressFeedback and ConnectedShapes)
// =====================================================================================
/**
 * Theme mode and color style, each a single row of compact tiles in one card.
 */
@Composable
internal fun ThemeSelectorCard(
    themeMode: ThemeMode,
    onThemeModeSelected: (ThemeMode) -> Unit,
    themeStyle: ThemeStyle,
    supportsDynamicColor: Boolean,
    onThemeStyleSelected: (ThemeStyle) -> Unit
) {
    // Same order as Morphe: System, Light, Dark
    val modes = listOf(ThemeMode.SYSTEM, ThemeMode.LIGHT, ThemeMode.DARK)
    val styles = ThemeStyle.entries.filter { it != ThemeStyle.MATERIAL_YOU || supportsDynamicColor }

    // SurfaceCard draws no border by default; SectionCard is the one with the hairline border
    SurfaceCard(cornerRadius = AppDefaults.SectionCornerRadius) {
        Column(
            modifier = Modifier.padding(AppDefaults.ContentPadding),
            verticalArrangement = Arrangement.spacedBy(AppDefaults.ItemSpacing)
        ) {
            ThemeOptionRow(
                title = "Theme mode",
                options = modes.map { ThemeOption(it, it.icon(), it.title) },
                selected = themeMode,
                onSelected = onThemeModeSelected
            )
            ThemeOptionRow(
                title = "Color style",
                options = styles.map { ThemeOption(it, it.icon(), it.title) },
                selected = themeStyle,
                onSelected = onThemeStyleSelected
            )
        }
    }
}

private data class ThemeOption<T>(val value: T, val icon: ImageVector, val label: String)

@Composable
private fun <T> ThemeOptionRow(
    title: String,
    options: List<ThemeOption<T>>,
    selected: T,
    onSelected: (T) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDefaults.ContentPaddingSmall)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        OptionGroup(items = options) { option, shape, interactionSource, itemModifier ->
            IconOptionCard(
                selected = option.value == selected,
                onClick = { onSelected(option.value) },
                icon = option.icon,
                label = option.label,
                modifier = itemModifier,
                shape = shape,
                interactionSource = interactionSource
            )
        }
    }
}

// =====================================================================================
// Option group + tile
// =====================================================================================
/** A single choice among a few [items], as one connected group whose buttons widen under a press. */
@Composable
internal fun <T> OptionGroup(
    items: List<T>,
    modifier: Modifier = Modifier,
    item: @Composable (item: T, shape: CornerBasedShape, interactionSource: MutableInteractionSource, modifier: Modifier) -> Unit
) {
    ButtonGroupRow(modifier = modifier) {
        items.forEachIndexed { index, option ->
            val interactionSource = remember { MutableInteractionSource() }
            item(
                option,
                PillGroupShapes.inRow(index, items.size),
                interactionSource,
                Modifier.buttonGroupItem(interactionSource)
            )
        }
    }
}

/** Compact icon-over-label tile, the one Morphe's theme selector uses. */
@Composable
internal fun IconOptionCard(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: CornerBasedShape = RoundedCornerShape(AppDefaults.CardCornerRadius),
    interactionSource: MutableInteractionSource? = null
) {
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    // A floor rather than a fixed height, so a label under a large font scale grows the tile
    val minHeight = if (landscape) 72.dp else 64.dp

    SelectionTile(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = minHeight),
        shape = shape,
        interactionSource = interactionSource
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(AppDefaults.ItemSpacing),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) LocalContentColor.current else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = if (selected) LocalContentColor.current else MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                lineHeight = MaterialTheme.typography.bodySmall.fontSize * 1.2
            )
        }
    }
}

/**
 * Selectable glass tile. The picked tile rounds out fully (pill) and gets a thicker border;
 * corners also shrink slightly while pressed.
 */
@Composable
internal fun SelectionTile(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: CornerBasedShape = RoundedCornerShape(AppDefaults.CardCornerRadius),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val tileShape = rememberPressShape(
        source,
        animateCornersAsState(if (selected) AppDefaults.PillShape else shape),
        enabled
    )
    // Unselected tiles never draw a border. The picked tile's border follows SelectedTileBorder
    val border = if (selected && SelectedTileBorder) {
        BorderStroke(2.dp, AppColors.glassBorderColor(selected = true))
    } else {
        null
    }

    Surface(
        modifier = modifier.semantics(mergeDescendants = true) {
            role = Role.RadioButton
            this.selected = selected
        },
        shape = tileShape,
        color = AppColors.glassContainerColor(selected),
        contentColor = AppColors.glassContentColor(selected),
        border = border,
        onClick = onClick,
        enabled = enabled,
        interactionSource = source
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
            content = content
        )
    }
}
