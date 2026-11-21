package dev.rushi.apkdownloadhelper.ui.shared.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import dev.rushi.apkdownloadhelper.ui.theme.SettingsValueStyle

/** Trailing value of a settings row ("Stable", "Default", "Clear"...). Colour: [SettingsValueStyle]. */
@Composable
internal fun SettingsValueText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = SettingsValueStyle.color()
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        style = MaterialTheme.typography.bodyMedium
    )
}
