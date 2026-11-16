package dev.rushi.apkdownloadhelper.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SdStorage
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

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
    val scrollState = rememberScrollState()

    MorphePinnedHeaderPage(
        header = {
            // Kept the "Settings" title here in the sticky header
            MorpheHeaderBar(title = "Settings")
        },
        scrollState = scrollState
    ) { insets ->
        CompositionLocalProvider(LocalSettingsSearch provides search) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(
                        start = insets.horizontal,
                        top = insets.top + 16.dp,
                        end = insets.horizontal,
                        bottom = 100.dp
                    ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Version text removed from here

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

@Composable
internal fun SettingsPageDetail(
    title: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    BackHandler { onBack() }

    MorpheSubpageScaffold(
        title = title,
        onBack = onBack
    ) {
        content()
    }
}