package dev.rushi.apkdownloadhelper.ui.screen.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.rushi.apkdownloadhelper.storage.*
import dev.rushi.apkdownloadhelper.ui.shared.appcard.AppListCard
import dev.rushi.apkdownloadhelper.ui.shared.icon.ThemedIcon
import dev.rushi.apkdownloadhelper.ui.shared.settings.SettingsGroup
import dev.rushi.apkdownloadhelper.ui.shared.settings.SettingsItemCard
import dev.rushi.apkdownloadhelper.ui.shared.settings.SettingsSection
import dev.rushi.apkdownloadhelper.ui.shared.settings.SettingsSwitchItem
import dev.rushi.apkdownloadhelper.ui.theme.AppDefaults
import dev.rushi.apkdownloadhelper.ui.theme.LocalMonochromeTheme
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ==========================================
// 1B. MORE CUSTOMIZATION TAB
// ==========================================
@Composable
internal fun CustomizationTabContent(
    settings: HelperSettings,
    onSettingsChange: (HelperSettings) -> Unit
) {
    val monochrome = LocalMonochromeTheme.current
    val useGradient = settings.useGradientCardColors && !monochrome
    val scheme = MaterialTheme.colorScheme

    var liveYoutubeApp by remember {
        mutableStateOf<dev.rushi.apkdownloadhelper.updater.ArchiveApp?>(null)
    }
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        liveYoutubeApp = withContext(Dispatchers.IO) {
            runCatching {
                dev.rushi.apkdownloadhelper.updater.MorpheArchive
                    .loadCachedIndex(context)
            }.getOrNull()
        }?.apps?.firstOrNull { it.packageName == "com.google.android.youtube" }
    }

    val previewApp = liveYoutubeApp ?: dev.rushi.apkdownloadhelper.updater.ArchiveApp(
        packageName = "com.google.android.youtube",
        name = "YouTube",
        iconColor = "#FF0033",
        iconUrl = "https://play-lh.googleusercontent.com/QNmuZQc9I6Zbe3mWnSr0hycnENqGFCI5p3yE29Hkxtf22T0IWS6zTrpxULLyyjWpB7ONAXDsDQXnXcVWokl3eg"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SettingsSection(title = "Find apps layout", icon = Icons.Outlined.ViewList) {
            SettingsGroup {
                SettingsSwitchItem(
                    icon = Icons.Outlined.Dns,
                    title = "Hide stats",
                    subtitle = "Hide the Sources, Apps, Patches, and Universal counters at the top.",
                    checked = settings.hideFindAppsStats,
                    onToggle = {
                        onSettingsChange(settings.copy(hideFindAppsStats = !settings.hideFindAppsStats))
                    }
                )
                SettingsSwitchItem(
                    icon = Icons.Outlined.Warning,
                    title = "Hide disclaimer",
                    subtitle = "Hide the community patch index disclaimer banner.",
                    checked = settings.hideCommunityNotice,
                    onToggle = {
                        onSettingsChange(settings.copy(hideCommunityNotice = !settings.hideCommunityNotice))
                    }
                )

                // Toggle + Live YouTube Preview inside the SAME card container
                SettingsItemCard(
                    onClick = {
                        onSettingsChange(settings.copy(useGradientCardColors = !settings.useGradientCardColors))
                    }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ThemedIcon(
                                icon = Icons.Outlined.AutoAwesome,
                                size = AppDefaults.IconSize,
                                tint = scheme.primary
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "Use gradient card colors",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = scheme.onSurface
                                )
                                Text(
                                    text = "Tint app cards, loading skeletons, and app headers with each app's brand color.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = scheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = settings.useGradientCardColors,
                                onCheckedChange = {
                                    onSettingsChange(settings.copy(useGradientCardColors = it))
                                },
                                thumbContent = {
                                    Icon(
                                        imageVector = if (settings.useGradientCardColors) Icons.Outlined.Check else Icons.Outlined.Close,
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                    )
                                }
                            )
                        }

                        // Framed on the app's real background color so the theme card color stands out clearly
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(26.dp))
                                .background(scheme.background)
                                .padding(10.dp)
                        ) {
                            AppListCard(
                                app = previewApp,
                                installed = false,
                                favourite = false,
                                shape = RoundedCornerShape(22.dp),
                                useGradient = settings.useGradientCardColors,
                                onToggleFavourite = {},
                                onClick = {
                                    onSettingsChange(settings.copy(useGradientCardColors = !settings.useGradientCardColors))
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}
