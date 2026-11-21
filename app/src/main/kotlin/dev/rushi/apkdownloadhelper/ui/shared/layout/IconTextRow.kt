package dev.rushi.apkdownloadhelper.ui.shared.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.rushi.apkdownloadhelper.ui.theme.AppDefaults

/** Row of optional leading content, title/description column and optional trailing content. */
@Composable
internal fun IconTextRow(
    modifier: Modifier = Modifier,
    leadingContent: @Composable (() -> Unit)? = null,
    title: String,
    description: String? = null,
    titleStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    titleWeight: FontWeight = FontWeight.Medium,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    descriptionStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    descriptionColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    trailingContent: @Composable (() -> Unit)? = null,
    spacing: Dp = AppDefaults.ItemSpacing
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        leadingContent?.invoke()

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = titleStyle,
                fontWeight = titleWeight,
                color = titleColor
            )
            description?.let {
                Text(
                    text = it,
                    style = descriptionStyle,
                    color = descriptionColor
                )
            }
        }

        trailingContent?.invoke()
    }
}
