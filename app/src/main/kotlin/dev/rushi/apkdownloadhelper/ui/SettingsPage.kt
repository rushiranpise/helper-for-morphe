package dev.rushi.apkdownloadhelper.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
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
    bottomBarHeight: Dp,
    versionName: String,
    settings: HelperSettings,
    onSettingsChange: (HelperSettings) -> Unit,
    onOpenUpdates: () -> Unit,
    onActiveCategoryChanged: (Boolean) -> Unit = {}, // NEW PARAMETER
    categoryContent: @Composable (category: SettingsCategory, bottomBarHeight: Dp, onBack: () -> Unit) -> Unit
) {
    var activeCategory by rememberSaveable { mutableStateOf<SettingsCategory?>(null) }
    val search = remember { SettingsSearch { "" } }

    // Notify MainActivity to hide the nav bar
    LaunchedEffect(activeCategory) {
        onActiveCategoryChanged(activeCategory != null)
    }

    BackHandler(enabled = activeCategory != null) {
        activeCategory = null
    }

    if (activeCategory != null) {
        categoryContent(activeCategory!!, bottomBarHeight) { activeCategory = null }
    } else {
        // HUB SCREEN
        CompositionLocalProvider(LocalSettingsSearch provides search) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(
                    top = 16.dp,
                    bottom = bottomBarHeight + 32.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Version $versionName",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                item {
                    SettingsGroup {
                        SettingsCategory.entries.forEachIndexed { index, category ->
                            SettingsItem(
                                onClick = { activeCategory = category },
                                icon = category.icon,
                                title = category.title,
                                subtitle = category.subtitle
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
}

// Subpage detail header
@Composable
internal fun SettingsPageDetail(
    title: String,
    bottomBarHeight: Dp,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = bottomBarHeight + 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(content = content)
            }
        }
    }
}