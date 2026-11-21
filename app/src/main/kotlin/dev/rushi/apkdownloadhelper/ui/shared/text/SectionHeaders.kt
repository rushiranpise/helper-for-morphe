package dev.rushi.apkdownloadhelper.ui.shared.text

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import dev.rushi.apkdownloadhelper.ui.shared.icon.ThemedIcon
import dev.rushi.apkdownloadhelper.ui.shared.layout.IconTextRow
import dev.rushi.apkdownloadhelper.ui.theme.AppDefaults

/** Section title in the manager's current small-icon, muted-label style. */
@Composable
internal fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(AppDefaults.ContentPaddingSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier.size(AppDefaults.IconSize),
                contentAlignment = Alignment.Center
            ) {
                ThemedIcon(
                    icon = icon,
                    size = AppDefaults.IconSizeSmall,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Card header: tinted, top-rounded strip above a card's content. */
@Composable
internal fun CardHeader(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    description: String? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(
                topStart = AppDefaults.SectionCornerRadius,
                topEnd = AppDefaults.SectionCornerRadius
            )
        ) {
            IconTextRow(
                modifier = Modifier.padding(AppDefaults.ContentPadding),
                leadingContent = { ThemedIcon(icon = icon) },
                title = title,
                description = description
            )
        }
    }
}
