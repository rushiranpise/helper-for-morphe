package dev.rushi.apkdownloadhelper.ui.shared.button

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
internal fun SourceSquareButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    IconActionButton(
        icon = icon,
        contentDescription = contentDescription,
        onClick = onClick
    )
}
