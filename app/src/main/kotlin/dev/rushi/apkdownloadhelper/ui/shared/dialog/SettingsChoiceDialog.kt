package dev.rushi.apkdownloadhelper.ui.shared.dialog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.rushi.apkdownloadhelper.ui.shared.settings.SettingsOptionCard
import dev.rushi.apkdownloadhelper.ui.model.DownloadSource
import dev.rushi.apkdownloadhelper.ui.theme.AppDefaults

internal data class SettingsChoice(
    val icon: ImageVector?,
    val title: String,
    val description: String,
    val source: DownloadSource? = null
)

@Composable
internal fun SettingsChoiceDialog(
    title: String,
    choices: List<SettingsChoice>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(AppDefaults.ContentPaddingSmall)
            ) {
                choices.forEachIndexed { index, choice ->
                    SettingsOptionCard(
                        icon = choice.icon,
                        title = choice.title,
                        description = choice.description,
                        selected = index == selectedIndex,
                        source = choice.source,
                        onClick = { onSelect(index) }
                    )
                }
            }
        },
        confirmButton = {}
    )
}
