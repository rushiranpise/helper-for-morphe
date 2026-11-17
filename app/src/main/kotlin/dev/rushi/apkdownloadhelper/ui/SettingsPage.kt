package dev.rushi.apkdownloadhelper.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
internal enum class SettingsCategory(
    val title: String,
    val subtitle: String,
    val icon: ImageVector
) {
    SOURCES("Sources", "Enable, disable and reorder providers", Icons.Outlined.Dns),
    APPEARANCE("Appearance", "Theme, colors, and app icon", Icons.Outlined.Palette),
    DOWNLOADS("Downloads & Storage", "Save location, storage & Fast Mode", Icons.Outlined.SdStorage),
    SECURITY_NETWORK("Security & Network", "Network policy, DNS & VirusTotal", Icons.Outlined.Security),
    SYSTEM_LOGS("System & Logs", "Updates, history & diagnostics", Icons.AutoMirrored.Outlined.ViewList),
    ABOUT("About", "Version, licenses & GitHub", Icons.Outlined.Info)
}

@Composable
internal fun SettingsPage(
    versionName: String,
    onOpenCategory: (SettingsCategory) -> Unit
) {
    val search = SettingsSearch { "" }
    val scrollState = rememberScrollState()

    MorphePinnedHeaderPage(
        header = {
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
        }
    }
}

@Composable
internal fun SettingsPageDetail(
    title: String,
    onBack: () -> Unit,
    scrollState: ScrollState = rememberScrollState(),
    content: @Composable ColumnScope.() -> Unit
) {
    BackHandler { onBack() }
    MorpheSubpageScaffold(title = title, onBack = onBack, scrollState = scrollState) {
        content()
    }
}