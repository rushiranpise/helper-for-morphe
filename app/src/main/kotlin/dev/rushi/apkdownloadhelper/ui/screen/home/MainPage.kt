package dev.rushi.apkdownloadhelper.ui.screen.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.HowToVote
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

internal enum class MainPage(
    val label: String,
    val icon: ImageVector
) {
    FIND_APPS("Find apps", Icons.Outlined.Explore),
    HOME("Helper", Icons.Outlined.Download),
    SETTINGS("Settings", Icons.Outlined.Settings)
}
