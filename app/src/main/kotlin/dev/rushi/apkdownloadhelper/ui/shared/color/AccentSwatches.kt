package dev.rushi.apkdownloadhelper.ui.shared.color

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Colorize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import dev.rushi.apkdownloadhelper.R
import dev.rushi.apkdownloadhelper.ui.shared.shape.animateCornersAsState
import dev.rushi.apkdownloadhelper.ui.shared.shape.rememberPressShape
import dev.rushi.apkdownloadhelper.ui.theme.AppDefaults
import dev.rushi.apkdownloadhelper.ui.theme.toHexString

/*
 * Accent colour picker, ported from morphe-manager's
 * `ui/screen/shared/ColorPickerDialog.kt` and
 * `ui/screen/shared/colorpicker/{ColorPickerCanvas,ColorPresets}.kt`.
 *
 * Hue, saturation and value rather than red, green and blue: those are the axes a
 * colour is actually chosen along, and two of them fit one panel. Hex stays as the
 * way to carry a colour in and out, and as the way to type an exact one.
 */
/**
 * The manager's theme presets: enough spread to reach a usual choice in one tap
 * without a palette long enough to push the picker off the screen.
 */
internal val AccentPresets: List<Color> = listOf(
    Color(0xFF6750A4),
    Color(0xFF386641),
    Color(0xFF0061A4),
    Color(0xFF8E24AA),
    Color(0xFFEF6C00),
    Color(0xFF00897B),
    Color(0xFFD81B60),
    Color(0xFF5C6BC0),
    Color(0xFF43A047),
    Color(0xFF1DE9B6),
    Color(0xFFFFC400),
    Color(0xFF00B8D4),
    Color(0xFFD32F2F),
    Color(0xFFAFB42B),
    Color(0xFF795548),
    Color(0xFF546E7A)
)

/**
 * Wrapping grid of the accent choices, bracketed by the two that are not colours:
 * keeping the style's own accent, and picking one the palette does not carry.
 * Centring keeps a partly filled last row balanced under the ones above it.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AccentSwatchGrid(
    colors: List<Color>,
    selected: Color?,
    onSelect: (Color) -> Unit,
    onClear: () -> Unit,
    onCustomClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedArgb = selected?.toArgb()
    // A colour the user picked rather than took from the grid is what the trailing
    // swatch stands for
    val customSelected = selected != null && colors.none { it.toArgb() == selectedArgb }

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Leads the row, being where the grid starts out rather than one more
        // colour to weigh in
        AccentSwatch(
            fill = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            selected = selected == null,
            label = stringResource(R.string.default_label),
            description = "Keep the style's own accent",
            onClick = onClear,
            icon = Icons.Outlined.Close
        )

        colors.forEach { preset ->
            AccentSwatch(
                fill = preset,
                selected = preset.toArgb() == selectedArgb,
                label = preset.toHexString(),
                description = "Accent ${preset.toHexString()}",
                onClick = { onSelect(preset) }
            )
        }

        // Trails it, standing for none of the above rather than for one more of them
        AccentSwatch(
            fill = selected?.takeIf { customSelected } ?: MaterialTheme.colorScheme.surfaceVariant,
            selected = customSelected,
            label = stringResource(R.string.custom),
            description = "Custom colour",
            onClick = onCustomClick,
            icon = Icons.Outlined.Colorize
        )
    }
}

/**
 * One grid cell. Selection is carried by the border rather than an overlay, so the
 * swatch keeps showing the colour it stands for at full strength.
 */
@Composable
private fun AccentSwatch(
    fill: Color,
    selected: Boolean,
    label: String,
    description: String,
    onClick: () -> Unit,
    icon: ImageVector? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val shape = rememberPressShape(
        interactionSource,
        animateCornersAsState(
            if (selected) AppDefaults.PillShape
            else RoundedCornerShape(AppDefaults.CompactCornerRadius)
        )
    )
    val scheme = MaterialTheme.colorScheme

    val borderWidth by animateDpAsState(if (selected) 3.dp else 1.dp)
    val fillColor by animateColorAsState(fill)
    val borderColor by animateColorAsState(
        if (selected) legibleOn(fillColor) else scheme.outline.copy(alpha = 0.5f)
    )

    Box(
        modifier = Modifier
            .size(AppDefaults.MinTouchTarget)
            .clip(shape)
            .background(fillColor, shape)
            .border(borderWidth, borderColor, shape)
            .clickable(interactionSource = interactionSource, onClick = onClick)
            .semantics(mergeDescendants = true) {
                role = Role.RadioButton
                contentDescription = if (selected) "$description, selected" else description
                stateDescription = label
            },
        contentAlignment = Alignment.Center
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                tint = legibleOn(fillColor),
                modifier = Modifier.size(AppDefaults.IconSizeSmall)
            )
        }
    }
}

/** Padding helper for a section that holds only a swatch grid. */
@Composable
internal fun AccentGridSection(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp))
            .padding(AppDefaults.ContentPadding),
        verticalArrangement = Arrangement.spacedBy(AppDefaults.ItemSpacing)
    ) {
        content()
    }
}
