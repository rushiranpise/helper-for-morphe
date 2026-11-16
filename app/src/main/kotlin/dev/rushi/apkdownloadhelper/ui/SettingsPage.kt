package dev.rushi.apkdownloadhelper.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SdStorage
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.rushi.apkdownloadhelper.R

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
    onOpenUpdates: () -> Unit,
    categoryContent: @Composable (category: SettingsCategory, bottomBarHeight: Dp, onBack: () -> Unit) -> Unit
) {
    var activeCategory by rememberSaveable { mutableStateOf<SettingsCategory?>(null) }

    BackHandler(enabled = activeCategory != null) {
        activeCategory = null
    }

    AnimatedContent(targetState = activeCategory, label = "settings_hub") { category ->
        if (category != null) {
            categoryContent(category, bottomBarHeight) { activeCategory = null }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = MorpheDefaults.ContentPadding),
                contentPadding = PaddingValues(
                    top = MorpheDefaults.ContentPadding,
                    bottom = bottomBarHeight + MorpheDefaults.ContentPadding
                ),
                verticalArrangement = Arrangement.spacedBy(MorpheDefaults.ItemSpacing)
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.settings),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Version ",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                items(SettingsCategory.entries.size) { index ->
                    val item = SettingsCategory.entries[index]
                    SettingsItem(
                        onClick = { activeCategory = item },
                        icon = item.icon,
                        title = item.title,
                        subtitle = item.subtitle,
                        showBorder = true
                    )
                }

                item {
                    SettingsItem(
                        onClick = onOpenUpdates,
                        icon = Icons.Outlined.Refresh,
                        title = "Updates",
                        subtitle = "Check for new releases, changelog & Shizuku install",
                        showBorder = true
                    )
                }
            }
        }
    }
}

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
            .navigationBarsPadding()
            .padding(
                start = MorpheDefaults.ContentPadding,
                end = MorpheDefaults.ContentPadding,
                top = MorpheDefaults.ContentPadding,
                bottom = bottomBarHeight
            ),
        verticalArrangement = Arrangement.spacedBy(MorpheDefaults.ItemSpacing)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.align(Alignment.Center)
            )
            HelperHeaderIconButton(
                icon = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = stringResource(R.string.back),
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart)
            )
        }
        content()
    }
}

@Composable
internal fun MorpheSectionHeader(title: String, icon: ImageVector? = null) {
    MorpheSectionTitle(text = title, icon = icon)
}

@Composable
internal fun MorpheSectionCard(content: @Composable ColumnScope.() -> Unit) {
    SectionCard {
        Column(content = content)
    }
}
