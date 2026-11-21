package dev.rushi.apkdownloadhelper.ui.screen.helper

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.rushi.apkdownloadhelper.BuildConfig
import dev.rushi.apkdownloadhelper.R
import dev.rushi.apkdownloadhelper.storage.HelperSettings
import dev.rushi.apkdownloadhelper.ui.shared.button.BoltButton
import dev.rushi.apkdownloadhelper.ui.shared.layout.AppWordmark
import dev.rushi.apkdownloadhelper.ui.theme.AppDefaults

//========================================================================
// Shared top header (AppWordmark + version + BoltButton) used by both views.
//========================================================================

@Composable
internal fun HelperTopHeader(
    settings: HelperSettings,
    onSettingsChange: (HelperSettings) -> Unit
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppDefaults.ItemSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            AppWordmark(
                modifier = Modifier.height(26.dp),
                textColor = MaterialTheme.colorScheme.onBackground
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = stringResource(R.string.helper),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Text(
                    text = "v${BuildConfig.VERSION_NAME}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
            }
        }
        BoltButton(
            fastMode = settings.fastMode,
            onClick = {
                val enabled = !settings.fastMode
                onSettingsChange(settings.copy(fastMode = enabled))
                Toast.makeText(
                    context,
                    if (enabled) "Fast Mode on" else "Fast Mode off",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }
}