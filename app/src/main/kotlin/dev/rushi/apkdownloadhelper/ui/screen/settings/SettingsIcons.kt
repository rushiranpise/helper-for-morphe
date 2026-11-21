package dev.rushi.apkdownloadhelper.ui.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import dev.rushi.apkdownloadhelper.storage.*

internal fun DownloadLocation.icon(): ImageVector = when (this) {
    DownloadLocation.TEMPORARY -> Icons.Outlined.SdStorage
    DownloadLocation.EXTERNAL -> Icons.Outlined.PhoneAndroid
    DownloadLocation.DOWNLOADS -> Icons.Outlined.SaveAlt
    DownloadLocation.CUSTOM -> Icons.Outlined.FolderOpen
}

internal fun NetworkPolicy.icon(): ImageVector = when (this) {
    NetworkPolicy.WIFI_ONLY -> Icons.Outlined.Wifi
    NetworkPolicy.MOBILE_DATA_ONLY -> Icons.Outlined.SignalCellularAlt
    NetworkPolicy.WIFI_AND_MOBILE -> Icons.Outlined.NetworkCheck
}

internal fun ThemeMode.icon(): ImageVector = when (this) {
    ThemeMode.SYSTEM -> Icons.Outlined.Smartphone
    ThemeMode.DARK -> Icons.Outlined.DarkMode
    ThemeMode.LIGHT -> Icons.Outlined.LightMode
}

internal fun ThemeStyle.icon(): ImageVector = when (this) {
    ThemeStyle.MORPHE -> Icons.Outlined.Palette
    ThemeStyle.MATERIAL_YOU -> Icons.Outlined.AutoAwesome
    ThemeStyle.MONOCHROME -> Icons.Outlined.Contrast
}
