package dev.rushi.apkdownloadhelper.ui.shared.source

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.rushi.apkdownloadhelper.ui.shared.icon.ThemedIcon
import dev.rushi.apkdownloadhelper.ui.shared.layout.AnimatedExpand
import dev.rushi.apkdownloadhelper.ui.shared.layout.AppDivider
import dev.rushi.apkdownloadhelper.ui.theme.AppDefaults

@Composable
internal fun AboutModeCard(tab: SourceSubTab) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val title = when (tab) {
        SourceSubTab.Manual -> "About manual mode"
        SourceSubTab.Recommended -> "About requested mode"
        SourceSubTab.Latest -> "About latest mode"
        SourceSubTab.History -> "About version history"
    }
    val description = when (tab) {
        SourceSubTab.Manual ->
            "Opens the source's website so you can download the file yourself, " +
                "then select it to return it to Morphe."
        SourceSubTab.Recommended ->
            "Finds the exact version Morphe requested from the selected source " +
                "and downloads it."
        SourceSubTab.Latest ->
            "Finds the newest compatible version from the selected source and downloads it."
        SourceSubTab.History ->
            "Lists every version this source offers  pick any of them and download it."
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppDefaults.CompactCornerRadius)),
        shape = RoundedCornerShape(AppDefaults.CompactCornerRadius),
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.05f),
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(AppDefaults.CompactCornerRadius))
                    .clickable { expanded = !expanded }
                    .padding(horizontal = AppDefaults.ContentPadding, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ThemedIcon(
                    icon = Icons.Outlined.HelpOutline,
                    size = 18.dp,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) {
                        Icons.Outlined.ExpandLess
                    } else {
                        Icons.Outlined.ExpandMore
                    },
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AnimatedExpand(visible = expanded) {
                AppDivider(fullWidth = true)
                Text(
                    text = description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(
                        horizontal = AppDefaults.ContentPadding,
                        vertical = AppDefaults.ContentPadding
                    )
                )
            }
        }
    }
}
