package dev.rushi.apkdownloadhelper

/*
 * Accent colour picker, ported from morphe-manager's
 * `ui/screen/shared/ColorPickerDialog.kt` and
 * `ui/screen/shared/colorpicker/{ColorPickerCanvas,ColorPresets}.kt`.
 *
 * Hue, saturation and value rather than red, green and blue: those are the axes a
 * colour is actually chosen along, and two of them fit one panel. Hex stays as the
 * way to carry a colour in and out, and as the way to type an exact one.
 */

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Colorize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The manager's theme presets: enough spread to reach a usual choice in one tap
 * without a palette long enough to push the picker off the screen.
 */
internal val MorpheAccentPresets: List<Color> = listOf(
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

/** Where the colour sits, in the terms the two controls are laid out in. */
@Immutable
private data class HsvColor(val hue: Float, val saturation: Float, val value: Float) {
    val color: Color get() = Color.hsv(hue.coerceIn(0f, 360f), saturation, value)

    companion object {
        /**
         * Carries hue in its own right rather than leaving it to be read back off
         * the colour, a grey having none to recover.
         */
        val Saver = listSaver<HsvColor, Float>(
            save = { listOf(it.hue, it.saturation, it.value) },
            restore = { HsvColor(it[0], it[1], it[2]) }
        )
    }
}

private val MarkerRadius = 10.dp
private val MarkerStroke = 3.dp

/**
 * Exactly the handle's own diameter, so it fills the strip end to end. A ring is
 * stroked centred on its radius, which puts only half the width outside it.
 */
private val HueStripHeight = (MarkerRadius + MarkerStroke / 2) * 2

/** The hue wheel walked in even steps, which a sweep of stops approximates closely enough. */
private val HueStops = List(13) { Color.hsv(it * 30f, 1f, 1f) }

/** Black or white, whichever stays legible on [fill]. */
private fun legibleOn(fill: Color): Color =
    if (fill.luminance() < 0.5f) Color.White else Color.Black

/**
 * Saturation across, value down, over a background of the pure hue. Both a tap and
 * a drag report continuously, the handle being drawn from the value rather than
 * held as state of its own.
 */
@Composable
private fun SaturationValuePanel(
    hsv: HsvColor,
    onChange: (saturation: Float, value: Float) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 200.dp,
    contentDescription: String? = null
) {
    var size by remember { mutableStateOf(Size.Zero) }

    fun report(position: Offset) {
        if (size == Size.Zero) return
        onChange(
            (position.x / size.width).coerceIn(0f, 1f),
            1f - (position.y / size.height).coerceIn(0f, 1f)
        )
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(MorpheDefaults.CompactCornerRadius))
            .semantics { contentDescription?.let { this.contentDescription = it } }
            .pointerInput(Unit) { detectTapGestures { report(it) } }
            .pointerInput(Unit) {
                detectDragGestures(onDragStart = { report(it) }) { change, _ ->
                    change.consume()
                    report(change.position)
                }
            }
    ) {
        size = this.size

        drawRect(Brush.horizontalGradient(listOf(Color.White, Color.hsv(hsv.hue, 1f, 1f))))
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))

        // Straight off the value, so the handle sits under the finger during a drag
        // and is already in place when the dialog opens
        drawMarker(
            center = Offset(hsv.saturation * size.width, (1f - hsv.value) * size.height),
            fill = hsv.color,
            radius = MarkerRadius.toPx()
        )
    }
}

/**
 * The hue wheel laid out flat. Saturation and value stay where they are, so the
 * strip always shows fully saturated colours and reads as a spectrum rather than
 * as a slice of the current colour.
 */
@Composable
private fun HueSlider(
    hue: Float,
    onChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = HueStripHeight,
    contentDescription: String? = null
) {
    var width by remember { mutableFloatStateOf(0f) }

    fun report(x: Float) {
        if (width <= 0f) return
        onChange((x / width).coerceIn(0f, 1f) * 360f)
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .semantics { contentDescription?.let { this.contentDescription = it } }
            .pointerInput(Unit) { detectTapGestures { report(it.x) } }
            .pointerInput(Unit) {
                detectDragGestures(onDragStart = { report(it.x) }) { change, _ ->
                    change.consume()
                    report(change.position.x)
                }
            }
    ) {
        width = size.width

        // Rounded by the draw rather than by a clip, which would take the handle with it
        drawRoundRect(
            brush = Brush.horizontalGradient(HueStops),
            cornerRadius = CornerRadius(size.height / 2f)
        )

        drawMarker(
            center = Offset((hue / 360f) * size.width, size.height / 2f),
            fill = Color.hsv(hue, 1f, 1f),
            radius = MarkerRadius.toPx()
        )
    }
}

/**
 * The handle both controls share: the picked colour ringed in white over black,
 * which stays visible on any part of either gradient. Kept a full ring inside the
 * bounds, since a clipped handle reads as a rendering fault rather than as a value
 * at the end of its range.
 */
private fun DrawScope.drawMarker(center: Offset, fill: Color, radius: Float) {
    val stroke = MarkerStroke.toPx()
    val outer = radius + stroke / 2f
    val clamped = Offset(
        center.x.coerceIn(outer, (size.width - outer).coerceAtLeast(outer)),
        center.y.coerceIn(outer, (size.height - outer).coerceAtLeast(outer))
    )

    drawCircle(Color.Black.copy(alpha = 0.35f), outer, clamped)
    drawCircle(fill, radius, clamped)
    drawCircle(Color.White, radius, clamped, style = Stroke(stroke))
}

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
    val shape = RoundedCornerShape(MorpheDefaults.CompactCornerRadius)
    val scheme = MaterialTheme.colorScheme

    val borderWidth by animateDpAsState(if (selected) 3.dp else 1.dp)
    val fillColor by animateColorAsState(fill)
    val borderColor by animateColorAsState(
        if (selected) legibleOn(fillColor) else scheme.outline.copy(alpha = 0.5f)
    )

    Box(
        modifier = Modifier
            .size(MorpheDefaults.MinTouchTarget)
            .clip(shape)
            .background(fillColor, shape)
            .border(borderWidth, borderColor, shape)
            .clickable(onClick = onClick)
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
                modifier = Modifier.size(MorpheDefaults.IconSizeSmall)
            )
        }
    }
}

/**
 * Custom accent picker: a live preview, the saturation/value panel, the hue strip
 * and a hex field, so a colour can be dialled in or typed exactly. The presets stay
 * on the screen behind this, which is why the panel gets the whole dialog.
 */
@Composable
internal fun MorpheAccentPickerDialog(
    currentColorHex: String,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    val initial = currentColorHex.toAccentColorOrNull() ?: MorpheAccentPresets.first()
    // Saved rather than merely remembered, a colour half chosen being worth more
    // than the one it started from
    var hsv by rememberSaveable(stateSaver = HsvColor.Saver) {
        mutableStateOf(initial.toHsv())
    }
    var hexInput by rememberSaveable { mutableStateOf(initial.toHexString()) }
    val hexValid = hexInput.toAccentColorOrNull() != null

    /** Moving on the panel or the strip is what the hex readout follows, never the other way. */
    fun moveTo(updated: HsvColor) {
        hsv = updated
        hexInput = updated.color.toHexString()
    }

    MorpheDialog(
        title = stringResource(R.string.custom_accent),
        onDismiss = onDismiss,
        actions = {
            MorpheDialogButton(
                text = "Save",
                onClick = { hexInput.toAccentColorOrNull()?.let(onColorSelected) },
                enabled = hexValid,
                modifier = Modifier.weight(1f)
            )
            MorpheDialogButton(
                text = "Cancel",
                onClick = onDismiss,
                filled = false,
                modifier = Modifier.weight(1f)
            )
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(MorpheDefaults.CompactCornerRadius))
                .background(hsv.color),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = hexInput,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = legibleOn(hsv.color)
            )
        }

        SaturationValuePanel(
            hsv = hsv,
            onChange = { saturation, value -> moveTo(hsv.copy(saturation = saturation, value = value)) },
            contentDescription = stringResource(R.string.shade)
        )

        HueSlider(
            hue = hsv.hue,
            onChange = { moveTo(hsv.copy(hue = it)) },
            contentDescription = stringResource(R.string.hue)
        )

        OutlinedTextField(
            value = hexInput,
            onValueChange = { input ->
                hexInput = input
                val parsed = input.toAccentColorOrNull()
                if (parsed != null) {
                    // A grey types as hue 0, which would swing the panel back to red,
                    // so a colour carrying no hue of its own keeps the one on screen
                    val parsedHsv = parsed.toHsv()
                    hsv = if (parsedHsv.saturation == 0f) {
                        parsedHsv.copy(hue = hsv.hue)
                    } else {
                        parsedHsv
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = hexInput.isNotBlank() && !hexValid,
            label = { Text(stringResource(R.string.hex_colour)) },
            placeholder = { Text("#RRGGBB") }
        )
    }
}

/** The colour's position on the two axes the picker works in. */
private fun Color.toHsv(): HsvColor {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(toArgb(), hsv)
    return HsvColor(hsv[0], hsv[1], hsv[2])
}

/** Padding helper for a section that holds only a swatch grid. */
@Composable
internal fun AccentGridSection(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.padding(MorpheDefaults.ContentPadding),
        verticalArrangement = Arrangement.spacedBy(MorpheDefaults.ItemSpacing)
    ) {
        content()
    }
}
