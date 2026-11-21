package dev.rushi.apkdownloadhelper.ui.shared.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.rushi.apkdownloadhelper.R
import dev.rushi.apkdownloadhelper.ui.shared.button.DialogButton
import dev.rushi.apkdownloadhelper.ui.shared.color.AccentPresets
import dev.rushi.apkdownloadhelper.ui.shared.color.HsvColor
import dev.rushi.apkdownloadhelper.ui.shared.color.HueSlider
import dev.rushi.apkdownloadhelper.ui.shared.color.SaturationValuePanel
import dev.rushi.apkdownloadhelper.ui.shared.color.legibleOn
import dev.rushi.apkdownloadhelper.ui.shared.color.toHsv
import dev.rushi.apkdownloadhelper.ui.theme.AppDefaults
import dev.rushi.apkdownloadhelper.ui.theme.toAccentColorOrNull
import dev.rushi.apkdownloadhelper.ui.theme.toHexString

/**
 * Custom accent picker: a live preview, the saturation/value panel, the hue strip
 * and a hex field, so a colour can be dialled in or typed exactly. The presets stay
 * on the screen behind this, which is why the panel gets the whole dialog.
 */
@Composable
internal fun AccentPickerDialog(
    currentColorHex: String,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    val initial = currentColorHex.toAccentColorOrNull() ?: AccentPresets.first()
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

    AppDialog(
        title = stringResource(R.string.custom_accent),
        onDismiss = onDismiss,
        actions = {
            DialogButton(
                text = "Save",
                onClick = { hexInput.toAccentColorOrNull()?.let(onColorSelected) },
                enabled = hexValid,
                modifier = Modifier.weight(1f)
            )
            DialogButton(
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
                .clip(RoundedCornerShape(AppDefaults.CompactCornerRadius))
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
