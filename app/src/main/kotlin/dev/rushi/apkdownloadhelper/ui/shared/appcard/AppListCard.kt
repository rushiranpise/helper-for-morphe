package dev.rushi.apkdownloadhelper.ui.shared.appcard

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.outlined.Check
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.History
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.rushi.apkdownloadhelper.ui.shared.icon.ThemedIcon
import dev.rushi.apkdownloadhelper.ui.theme.LocalMonochromeTheme
import dev.rushi.apkdownloadhelper.ui.theme.pressScale
import dev.rushi.apkdownloadhelper.updater.ArchiveApp
import dev.rushi.apkdownloadhelper.updater.formatReleaseDate
import dev.rushi.apkdownloadhelper.updater.newestReleaseDate

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AppListCard(
    app: ArchiveApp,
    installed: Boolean,
    favourite: Boolean,
    shape: RoundedCornerShape,
    onToggleFavourite: () -> Unit,
    onClick: () -> Unit,
    tracked: Boolean = false,
    updates: Int = 0,
    newPatches: Int = 0,
    onToggleTracking: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    useGradient: Boolean = true,
    customBottomText: String? = null,
    customSubtitleText: String? = null,
    selected: Boolean = false,
    selectionMode: Boolean = false,
    onLongClick: (() -> Unit)? = null
) {
    val monochrome = LocalMonochromeTheme.current
    val effectiveGradient = useGradient && !monochrome
    val palette = remember(app.iconColor, app.packageName) { app.findAppCardColors() }
    val scheme = MaterialTheme.colorScheme
    val titleColor = if (!effectiveGradient) scheme.onSurface else palette.title
    val subtitleColor = if (!effectiveGradient) scheme.onSurfaceVariant else palette.subtitle
    val interactionSource = remember { MutableInteractionSource() }

    val titleStyle = MaterialTheme.typography.titleMedium.copy(
        fontWeight = FontWeight.SemiBold,
        shadow = if (!effectiveGradient) {
            null
        } else {
            Shadow(Color.Black.copy(alpha = 0.4f), Offset(0f, 2f), 4f)
        }
    )
    val subtitleStyle = MaterialTheme.typography.bodySmall.copy(
        shadow = if (!effectiveGradient) {
            null
        } else {
            Shadow(Color.Black.copy(alpha = 0.35f), Offset(0f, 1f), 2f)
        }
    )
    val released = formatReleaseDate(app.newestReleaseDate())

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 88.dp)
            .clip(shape)
            .then(
                if (selected) {
                    Modifier.border(2.dp, scheme.primary, shape)
                } else {
                    Modifier
                }
            )
            .drawWithCache {
                val topLeft = CornerRadius(shape.topStart.toPx(size, this))
                val topRight = CornerRadius(shape.topEnd.toPx(size, this))
                val bottomLeft = CornerRadius(shape.bottomStart.toPx(size, this))
                val bottomRight = CornerRadius(shape.bottomEnd.toPx(size, this))

                val outline = androidx.compose.ui.graphics.Outline.Rounded(
                    androidx.compose.ui.geometry.RoundRect(
                        left = 0f,
                        top = 0f,
                        right = size.width,
                        bottom = size.height,
                        topLeftCornerRadius = topLeft,
                        topRightCornerRadius = topRight,
                        bottomRightCornerRadius = bottomRight,
                        bottomLeftCornerRadius = bottomLeft
                    )
                )

                val fill: Brush = if (!effectiveGradient) {
                    SolidColor(scheme.surfaceContainerHigh)
                } else {
                    Brush.linearGradient(
                        colors = listOf(
                            palette.gradient[0].copy(alpha = 0.88f),
                            palette.gradient[1].copy(alpha = 0.76f),
                            palette.gradient[2].copy(alpha = 0.82f)
                        ),
                        start = Offset(0f, size.height),
                        end = Offset(size.width, 0f)
                    )
                }
                onDrawWithContent {
                    drawOutline(outline = outline, brush = fill)
                    drawContent()
                }
            }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FindAppIcon(app = app, gradient = palette.gradient, installed = installed)

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = app.name,
                    style = titleStyle,
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = customSubtitleText ?: app.packageName,
                    style = subtitleStyle,
                    color = subtitleColor.copy(alpha = 0.82f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (updates > 0 || newPatches > 0) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (updates > 0) {
                            TrackingChangeBadge(
                                text = "$updates update" + if (updates == 1) "" else "s",
                                container = if (effectiveGradient) {
                                    Color.White.copy(alpha = 0.22f)
                                } else {
                                    scheme.primaryContainer
                                },
                                content = if (effectiveGradient) Color.White else scheme.onPrimaryContainer
                            )
                        }
                        if (newPatches > 0) {
                            TrackingChangeBadge(
                                text = "$newPatches new patch" + if (newPatches == 1) "" else "es",
                                container = if (effectiveGradient) {
                                    Color.White.copy(alpha = 0.18f)
                                } else {
                                    scheme.tertiaryContainer
                                },
                                content = if (effectiveGradient) Color.White else scheme.onTertiaryContainer
                            )
                        }
                    }
                }
                if (customBottomText != null) {
                    Text(
                        text = customBottomText,
                        style = MaterialTheme.typography.labelSmall,
                        color = subtitleColor.copy(alpha = 0.88f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (released != null) {
                                ThemedIcon(
                                    icon = Icons.Outlined.History,
                                    size = 12.dp,
                                    tint = subtitleColor.copy(alpha = 0.75f)
                                )
                                Text(
                                    text = released,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = subtitleColor.copy(alpha = 0.75f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Text(
                            text = "${app.sourceCount} " +
                                    if (app.sourceCount == 1) "source" else "sources",
                            style = MaterialTheme.typography.labelSmall,
                            color = subtitleColor.copy(alpha = 0.9f),
                            maxLines = 1
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selectionMode) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected) Color(0xFF22C55E)
                                else if (!effectiveGradient) scheme.surfaceVariant.copy(alpha = 0.65f)
                                else Color.White.copy(alpha = 0.22f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selected) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = "Selected",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                } else if (customBottomText == null) {
                    onToggleTracking?.let { toggleTracking ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        tracked -> if (effectiveGradient) {
                                            Color.White.copy(alpha = 0.22f)
                                        } else {
                                            scheme.primaryContainer.copy(alpha = 0.85f)
                                        }

                                        !effectiveGradient -> scheme.surfaceVariant.copy(alpha = 0.65f)
                                        else -> Color.White.copy(alpha = 0.16f)
                                    }
                                )
                                .clickable(role = Role.Button, onClick = toggleTracking),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (tracked) {
                                    Icons.Filled.Notifications
                                } else {
                                    Icons.Outlined.Notifications
                                },
                                contentDescription = if (tracked) "Stop tracking" else "Track updates",
                                tint = if (tracked) {
                                    if (!effectiveGradient) scheme.onPrimaryContainer else Color.White
                                } else if (!effectiveGradient) {
                                    scheme.onSurface
                                } else {
                                    Color.White
                                },
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    favourite -> Color.White.copy(alpha = 0.92f)
                                    !effectiveGradient -> scheme.surfaceVariant.copy(alpha = 0.65f)
                                    else -> Color.White.copy(alpha = 0.16f)
                                }
                            )
                            .clickable(role = Role.Button, onClick = onToggleFavourite),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (favourite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = if (favourite) "Unlike" else "Like",
                            tint = if (favourite) {
                                Color(0xFFD32F2F)
                            } else if (!effectiveGradient) {
                                scheme.onSurface
                            } else {
                                Color.White
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackingChangeBadge(
    text: String,
    container: Color,
    content: Color
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = container,
        contentColor = content
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
