package dev.rushi.apkdownloadhelper.ui.screen.settings

import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.rushi.apkdownloadhelper.R
import dev.rushi.apkdownloadhelper.storage.*
import dev.rushi.apkdownloadhelper.ui.shared.color.AccentPresets
import dev.rushi.apkdownloadhelper.ui.shared.color.AccentSwatchGrid
import dev.rushi.apkdownloadhelper.ui.shared.dialog.AccentPickerDialog
import dev.rushi.apkdownloadhelper.ui.shared.selection.ThemeSelectorCard
import dev.rushi.apkdownloadhelper.ui.shared.settings.SettingsGroup
import dev.rushi.apkdownloadhelper.ui.shared.settings.SettingsItemCard
import dev.rushi.apkdownloadhelper.ui.shared.settings.SettingsSection
import dev.rushi.apkdownloadhelper.ui.shared.settings.SettingsSwitchItem
import dev.rushi.apkdownloadhelper.ui.theme.AppDefaults
import dev.rushi.apkdownloadhelper.ui.theme.toAccentColorOrNull
import dev.rushi.apkdownloadhelper.ui.theme.toHexString

// ==========================================
// 1. APPEARANCE TAB
// ==========================================
@Composable
internal fun AppearanceTabContent(
    settings: HelperSettings,
    onSettingsChange: (HelperSettings) -> Unit
) {
    var showAccentPicker by rememberSaveable { mutableStateOf(false) }
    val supportsDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val accent = settings.customAccentColor.toAccentColorOrNull()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        SettingsSection(title = "Theme", icon = Icons.Outlined.Palette) {
            ThemeSelectorCard(
                themeMode = settings.themeMode,
                onThemeModeSelected = { onSettingsChange(settings.copy(themeMode = it)) },
                themeStyle = settings.themeStyle,
                supportsDynamicColor = supportsDynamicColor,
                onThemeStyleSelected = { onSettingsChange(settings.copy(themeStyle = it)) }
            )

            SettingsGroup {
                if (settings.themeStyle == ThemeStyle.MORPHE) {
                    SettingsItemCard(onClick = null) {
                        Column(
                            modifier = Modifier.padding(AppDefaults.ContentPadding),
                            verticalArrangement = Arrangement.spacedBy(AppDefaults.ContentPaddingSmall)
                        ) {
                            Text(
                                text = "Accent color",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            AccentSwatchGrid(
                                colors = AccentPresets,
                                selected = accent,
                                onSelect = { color ->
                                    onSettingsChange(settings.copy(customAccentColor = color.toHexString()))
                                },
                                onClear = { onSettingsChange(settings.copy(customAccentColor = "")) },
                                onCustomClick = { showAccentPicker = true }
                            )
                        }
                    }
                }

                if (settings.themeMode != ThemeMode.LIGHT) {
                    SettingsSwitchItem(
                        icon = Icons.Outlined.Contrast,
                        title = stringResource(R.string.pure_black),
                        subtitle = "Uses true black backgrounds",
                        checked = settings.pureBlackTheme,
                        onToggle = { onSettingsChange(settings.copy(pureBlackTheme = !settings.pureBlackTheme)) }
                    )
                }
            }
        }

        // BACKGROUND SECTION
        SettingsSection(title = "Background", icon = Icons.Outlined.Wallpaper) {
            SettingsGroup {
                SettingsSwitchItem(
                    icon = Icons.Outlined.Wallpaper,
                    title = "Animated circle backdrop",
                    subtitle = "Drift softly behind the helper. Holds still when animations are off or battery saver is on.",
                    checked = settings.animatedBackground,
                    onToggle = {
                        onSettingsChange(settings.copy(animatedBackground = !settings.animatedBackground))
                    }
                )

                SettingsSwitchItem(
                    icon = Icons.Outlined.BlurOn,
                    title = "Blur effects",
                    subtitle = "Frosted headers and cards. Turn off to reduce GPU load on slower devices.",
                    checked = settings.blurEffects,
                    onToggle = {
                        onSettingsChange(settings.copy(blurEffects = !settings.blurEffects))
                    }
                )
            }
        }

    }

    if (showAccentPicker) {
        AccentPickerDialog(
            currentColorHex = settings.customAccentColor,
            onColorSelected = { color ->
                onSettingsChange(settings.copy(customAccentColor = color.toHexString()))
                showAccentPicker = false
            },
            onDismiss = { showAccentPicker = false }
        )
    }
}
