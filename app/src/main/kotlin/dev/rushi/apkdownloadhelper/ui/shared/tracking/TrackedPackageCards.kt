package dev.rushi.apkdownloadhelper.ui.shared.tracking

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun PendingTrackedPackagesCard(
    packages: Set<String>,
    onRemove: (String) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.05f
    val container = scheme.surfaceVariant.copy(alpha = if (isDark) 0.35f else 0.6f)
    val sorted = remember(packages) { packages.sorted() }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = container,
        border = BorderStroke(1.dp, scheme.outline.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = null,
                    tint = scheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Waiting for index",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${packages.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant
                )
            }
            Text(
                text = "We'll notify you when these packages appear in the Morphe index.",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant
            )
            sorted.forEach { packageName ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(scheme.surface.copy(alpha = if (isDark) 0.25f else 0.65f))
                        .padding(start = 10.dp, end = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .clickable(role = Role.Button) { onRemove(packageName) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Stop waiting for $packageName",
                            tint = scheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun TrackPackageField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    notice: String?,
    noticeIsError: Boolean,
    onTrack: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.05f
    val canTrack = enabled && value.isNotBlank()
    val container = scheme.surfaceVariant.copy(alpha = if (isDark) 0.35f else 0.6f)

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = container,
        border = BorderStroke(1.dp, scheme.outline.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Apps,
                    contentDescription = null,
                    tint = scheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = "Package name (com.example.app)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        enabled = enabled,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = if (enabled) {
                                scheme.onSurface
                            } else {
                                scheme.onSurfaceVariant.copy(alpha = 0.5f)
                            }
                        ),
                        cursorBrush = SolidColor(scheme.primary),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { if (canTrack) onTrack() }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (canTrack) scheme.primary else container)
                        .clickable(enabled = canTrack, role = Role.Button, onClick = onTrack)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Track",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (canTrack) scheme.onPrimary else scheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
            Text(
                text = notice ?: "Not in the index yet? It will be tracked when it appears.",
                style = MaterialTheme.typography.bodySmall,
                color = when {
                    notice == null -> scheme.onSurfaceVariant.copy(alpha = 0.75f)
                    noticeIsError -> scheme.error
                    else -> scheme.primary
                },
                modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 10.dp)
            )
        }
    }
}
