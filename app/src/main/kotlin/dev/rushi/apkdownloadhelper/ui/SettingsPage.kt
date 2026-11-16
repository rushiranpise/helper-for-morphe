package dev.rushi.apkdownloadhelper.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SdStorage
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.rushi.apkdownloadhelper.storage.HelperSettings

internal enum class SettingsCategory(
    val title: String,
    val subtitle: String,
    val icon: ImageVector
) {
    DOWNLOADS("Downloads & sources", "Save location, retention, Fast Mode & providers", Icons.Outlined.SdStorage),
    APPEARANCE("Appearance", "Theme mode, colour style, pure black & accent", Icons.Outlined.Palette),
    ADVANCED("Advanced", "Network policy, connections, DNS & VirusTotal", Icons.Outlined.Tune),
    LOGS("Logs & history", "Download history, Logcat & request diagnostics", Icons.AutoMirrored.Outlined.ViewList)
}

@Composable
internal fun SettingsPage(
    versionName: String,
    onOpenCategory: (SettingsCategory) -> Unit,
    onOpenUpdates: () -> Unit
) {
    val search = SettingsSearch { "" }

    CompositionLocalProvider(LocalSettingsSearch provides search) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(
                top = 16.dp,
                bottom = 100.dp // Space for floating pill
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Left-aligned header
                Column(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Version $versionName",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                SettingsGroup {
                    SettingsCategory.entries.forEach { categoryItem ->
                        SettingsItem(
                            onClick = { onOpenCategory(categoryItem) },
                            icon = categoryItem.icon,
                            title = categoryItem.title,
                            subtitle = categoryItem.subtitle
                        )
                    }
                }
            }

            item {
                SettingsGroup {
                    SettingsItem(
                        onClick = onOpenUpdates,
                        icon = Icons.Outlined.Refresh,
                        title = "Updates",
                        subtitle = "Check for new releases, changelog & Shizuku install"
                    )
                }
            }
        }
    }
}

// Subpage detail header (Rendered outside the Pager in MainActivity)
@Composable
internal fun SettingsPageDetail(
    title: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    // This safely intercepts the hardware back button now that it's an overlay
    BackHandler {
        onBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // Standard Left-Aligned Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HelperHeaderIconButton(
                icon = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back",
                onClick = onBack
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(content = content)
            }
        }
    }
}