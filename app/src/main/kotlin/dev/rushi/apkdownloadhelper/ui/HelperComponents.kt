package dev.rushi.apkdownloadhelper.ui

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import dev.rushi.apkdownloadhelper.HelperButton
import dev.rushi.apkdownloadhelper.HelperOutlinedButton
import dev.rushi.apkdownloadhelper.R
import dev.rushi.apkdownloadhelper.download.isPackageInstalled
import dev.rushi.apkdownloadhelper.download.openPlayStoreListing
import dev.rushi.apkdownloadhelper.download.orderedFileKinds
import dev.rushi.apkdownloadhelper.isHistoryUriUsable
import dev.rushi.apkdownloadhelper.storage.AppLog
import dev.rushi.apkdownloadhelper.storage.DownloadHistoryEntry
import dev.rushi.apkdownloadhelper.storage.RequestLogEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale


@Composable
internal fun HelperBoltButton(
    fastMode: Boolean,
    onClick: () -> Unit
) {
    HelperIconButton(
        icon = Icons.Outlined.Bolt,
        contentDescription = if (fastMode) "Fast Mode on" else "Fast Mode off",
        onClick = onClick,
        selected = fastMode
    )
}

/**
 * Square glass button used by the header row and other single-icon actions,
 * matching the manager's glass-button family (16dp shape, 48dp touch target).
 */
@Composable
internal fun HelperIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    tint: Color? = null
) {
    val primary = MaterialTheme.colorScheme.primary
    val contentColor = tint ?: if (selected) primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        modifier = modifier
            .size(MorpheDefaults.GlassButtonHeight)
            .clip(RoundedCornerShape(MorpheDefaults.CardCornerRadius))
            .pressScale(interactionSource),
        shape = RoundedCornerShape(MorpheDefaults.CardCornerRadius),
        color = if (selected) primary.copy(alpha = 0.28f) else Color.Transparent,
        contentColor = contentColor,
        border = BorderStroke(1.dp, primary.copy(alpha = if (selected) 0.6f else 0.32f)),
        interactionSource = interactionSource
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(MorpheDefaults.IconSizeSmall)
            )
        }
    }
}

/** A previously downloaded file offered for reuse, with its size on disk. */
internal data class ReuseOption(
    val entry: DownloadHistoryEntry,
    val sizeBytes: Long
)

@Composable
internal fun ReuseOfferDialog(
    options: List<ReuseOption>,
    onUseExisting: (ReuseOption) -> Unit,
    onDownloadNew: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDownloadNew,
        title = {
            Text(
                text = stringResource(R.string.use_an_existing_apk),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(MorpheDefaults.ContentPaddingSmall)
            ) {
                Text(
                    text = stringResource(R.string.a_previous_download_for_this_exact),
                    style = MaterialTheme.typography.bodyMedium
                )
                options.forEach { option ->
                    val entry = option.entry
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(MorpheDefaults.CompactCornerRadius))
                            .clickable { onUseExisting(option) },
                        shape = RoundedCornerShape(MorpheDefaults.CompactCornerRadius),
                        color = sourceCardFill(),
                        border = BorderStroke(1.dp, sourceCardBorder())
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = entry.sourceName,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                entry.versionName?.let {
                                    Text(
                                        text = "· $it",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (option.sizeBytes > 0L) {
                                    Text(
                                        text = "· ${option.sizeBytes.formatBytes()}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Text(
                                text = entry.fileName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            entry.scanVerdict?.let { verdict ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(MorpheDefaults.ContentPaddingSmall),
                                    modifier = Modifier.padding(top = 3.dp)
                                ) {
                                    ThemedIcon(
                                        icon = when {
                                            verdict.malicious -> Icons.Outlined.Warning
                                            verdict.failed -> Icons.Outlined.HelpOutline
                                            else -> Icons.Outlined.CheckCircle
                                        },
                                        size = 16.dp,
                                        tint = when {
                                            verdict.malicious -> SemanticTone.Error.accent
                                            verdict.failed -> SemanticTone.Warning.accent
                                            else -> SemanticTone.Success.accent
                                        }
                                    )
                                    Text(
                                        text = verdict.label,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = if (verdict.malicious) {
                                            MaterialTheme.colorScheme.error
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                HelperButton(
                    text = "Download new",
                    onClick = onDownloadNew,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {}
    )
}

@Composable
internal fun HelperHeaderIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    HelperIconButton(
        icon = icon,
        contentDescription = contentDescription,
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
internal fun HelperThemeButton(
    dark: Boolean,
    onToggle: () -> Unit
) {
    HelperIconButton(
        icon = if (dark) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
        contentDescription = if (dark) "Switch to light theme" else "Switch to dark theme",
        onClick = onToggle
    )
}

@Composable
internal fun AppInfoCard(
    request: HelperRequest,
    onFormatToggle: (String) -> Unit
) {
    SurfaceCard(cornerRadius = MorpheDefaults.SectionCornerRadius) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MorpheDefaults.ContentPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AppInfoHeader(request, onFormatToggle)
        }
    }
}

@Composable
internal fun AppInfoHeader(
    request: HelperRequest,
    onFormatToggle: (String) -> Unit
) {
    val context = LocalContext.current
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
    var copied by remember(request.packageName) { mutableStateOf(false) }
    var installed by remember(request.packageName) { mutableStateOf(false) }
    LaunchedEffect(request.packageName) {
        installed = withContext(Dispatchers.IO) {
            context.isPackageInstalled(request.packageName)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppAvatar(
                packageName = request.packageName,
                initial = request.appName.firstOrNull()?.uppercaseChar() ?: '?'
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = request.appName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (installed) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = stringResource(R.string.installed),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = request.packageName,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = stringResource(R.string.copy_package),
                        tint = if (copied) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier
                            .size(16.dp)
                            .clip(MorpheDefaults.PillShape)
                            .clickable {
                                clipboard?.setPrimaryClip(
                                    ClipData.newPlainText("package", request.packageName)
                                )
                                copied = true
                            }
                            .padding(2.dp)
                    )
                }
            }
        }

        // Version + Format cards side by side, same height (the build subtext
        // would otherwise make the Version card taller).
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AppInfoStatCard(
                label = stringResource(R.string.version),
                value = request.requestedVersionName ?: "Any",
                subtext = request.versionCodeSummary?.let { "build $it" },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            AppInfoFormatCard(
                kinds = request.availableFileKinds.orderedFileKinds(),
                selectedKinds = request.requestedFileKinds,
                onToggle = onFormatToggle,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }

        AppInfoArchCard(
            abis = request.availableAbis
        )
    }
}

@Composable
internal fun AppInfoStatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    subtext: String? = null
) {
    // The manager's BundleInfoCard: a tinted container holding a small label
    // above a medium-weight value, on the shared warning tone.
    SurfaceCard(
        modifier = modifier,
        cornerRadius = MorpheDefaults.CompactCornerRadius,
        color = SemanticTone.Warning.container
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                color = SemanticTone.Warning.content.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = SemanticTone.Warning.content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            subtext?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
internal fun AppInfoFormatCard(
    kinds: List<String>,
    selectedKinds: Set<String>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(MorpheDefaults.CompactCornerRadius)
    val selected = kinds.filter { it in selectedKinds }
    val summary = when {
        selected.isEmpty() || selected.size == kinds.size -> "Any"
        selected.size == 1 -> selected.first().uppercase(Locale.US)
        else -> selected.joinToString(" + ") { it.uppercase(Locale.US) }
    }
    var expanded by rememberSaveable(kinds) { mutableStateOf(false) }

    Surface(
        modifier = modifier.clip(shape),
        shape = shape,
        color = sourceCardFill(),
        border = BorderStroke(1.dp, sourceCardBorder())
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = stringResource(R.string.format),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = summary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (kinds.size > 1) {
                    // Same count pill + chevron affordance as the Architecture card.
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(MorpheDefaults.PillShape)
                            .clickable { expanded = !expanded }
                            .padding(horizontal = 2.dp, vertical = 2.dp)
                    ) {
                        if (!expanded) {
                            Text(
                                text = "${selected.size}/${kinds.size}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier
                                    .clip(MorpheDefaults.PillShape)
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Icon(
                            imageVector = if (expanded) {
                                Icons.Outlined.ExpandLess
                            } else {
                                Icons.Outlined.ExpandMore
                            },
                            contentDescription = if (expanded) "Collapse formats" else "Choose formats",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            // Plain conditional (no AnimatedVisibility): this card sits in an
            // equal-height Row(IntrinsicSize.Min), where the expand animation's
            // intrinsic height is measured as collapsed and clips the list.
            if (expanded) {
                androidx.compose.material3.HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                )
                kinds.forEach { kind ->
                    val isSelected = kind in selectedKinds
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(MorpheDefaults.CompactCornerRadius))
                            .clickable { onToggle(kind) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = kind.uppercase(Locale.US),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun AppInfoArchCard(abis: List<String>) {
    val shape = RoundedCornerShape(MorpheDefaults.CompactCornerRadius)
    var expanded by rememberSaveable { mutableStateOf(false) }
    val displayAbis = abis.takeIf { it.isNotEmpty() } ?: listOf("Default")

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(enabled = displayAbis.size > 1) { expanded = !expanded },
        shape = shape,
        color = sourceCardFill(),
        border = BorderStroke(1.dp, sourceCardBorder())
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.architecture),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.weight(1f)
                )
                if (displayAbis.size > 1 && !expanded) {
                    Text(
                        text = "+${displayAbis.size - 1}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier
                            .clip(MorpheDefaults.PillShape)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                Icon(
                    imageVector = if (expanded) {
                        Icons.Outlined.ExpandLess
                    } else {
                        Icons.Outlined.ExpandMore
                    },
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
            AnimatedExpand(visible = expanded) {
                MorpheStatusBadgeRow(modifier = Modifier.fillMaxWidth()) {
                    displayAbis.forEach { abi ->
                        MorpheStatusBadge(text = abi)
                    }
                }
            }
            if (!expanded) {
                Text(
                    text = displayAbis.first(),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
internal fun AppAvatar(packageName: String, initial: Char) {
    val context = LocalContext.current
    // Fetch the installed app's real icon; when the app isn't installed use the
    // Android default app icon; the letter tile is the last-resort fallback.
    var iconBitmap by remember(packageName) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(packageName) {
        iconBitmap = withContext(Dispatchers.IO) {
            val installed = runCatching {
                context.packageManager.getApplicationIcon(packageName)
            }.getOrNull()
            val drawable = installed
                ?: context.packageManager.getDefaultActivityIcon()
            runCatching {
                drawable.toBitmap(width = 176, height = 176).asImageBitmap()
            }.getOrNull()
        }
    }

    if (iconBitmap != null) {
        Image(
            bitmap = iconBitmap!!,
            contentDescription = null,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(MorpheDefaults.CompactCornerRadius))
        )
    } else {
        val colors = listOf(
            Color(0xFF1A73E8),
            Color(0xFF4C8DFF),
            Color(0xFFFBBC04),
            Color(0xFFEA4335),
            Color(0xFF4285F4),
            Color(0xFFF25C1B)
        )
        val color = colors[initial.code % colors.size]
        // Monochrome mode drops the per-app hue and takes the neutral accent, so
        // a list of these tiles does not read as the only colour left on screen.
        val tile = MonochromeThemeDefaults.accentColor(color)
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(MorpheDefaults.CompactCornerRadius))
                .background(tile.copy(alpha = 0.85f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial.toString(),
                color = MonochromeThemeDefaults.iconTint(Color.White),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/** Shared expand/collapse animation for every collapsible section. */
@Composable
internal fun AnimatedExpand(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = expandVertically(
            animationSpec = tween(MorpheDefaults.ANIMATION_DURATION),
            expandFrom = Alignment.Top
        ) + fadeIn(animationSpec = tween(MorpheDefaults.ANIMATION_DURATION)),
        exit = shrinkVertically(
            animationSpec = tween(MorpheDefaults.ANIMATION_DURATION_SHORT),
            shrinkTowards = Alignment.Top
        ) + fadeOut(animationSpec = tween(MorpheDefaults.ANIMATION_DURATION_SHORT))
    ) {
        content()
    }
}

@Composable
internal fun SwitchCandidateDialog(
    prompt: SwitchCandidatePrompt,
    onChoose: (DownloadCandidate) -> Unit,
    onDismiss: () -> Unit
) {
    MorpheDialog(
        title = "Choose a variant from ${prompt.source.label}",
        onDismiss = onDismiss,
        actions = {
            HelperOutlinedButton(
                text = "Cancel",
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            )
        }
    ) {
        Text(
            text = prompt.detail,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            prompt.candidates.forEach { candidate ->
                val variant = candidate.variantLabel ?: candidate.variantTag
                SurfaceCard(
                    onClick = { onChoose(candidate) },
                    cornerRadius = MorpheDefaults.CompactCornerRadius,
                    borderWidth = 1.dp,
                    borderColor = MaterialTheme.colorScheme.outlineVariant
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(MorpheDefaults.ContentPadding),
                        horizontalArrangement = Arrangement.spacedBy(MorpheDefaults.ItemSpacing),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = candidate.versionDisplay,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = listOfNotNull(
                                    candidate.fileKind.uppercase(Locale.US),
                                    variant?.takeIf { it.isNotBlank() }
                                ).joinToString(" · "),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = if (candidate.directDownload) {
                                Icons.Outlined.Download
                            } else {
                                Icons.Outlined.OpenInBrowser
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun LoadingState() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CircularProgressIndicator()
    }
}

internal data class PrimaryAction(
    val label: String,
    val icon: ImageVector,
    val enabled: Boolean = true,
    val loading: Boolean = false,
    val run: () -> Unit
)

@Composable
internal fun SourcePickerFlow(
    request: HelperRequest,
    result: CandidateResult,
    selectedPagerPage: Int,
    onPagerPageChanged: (Int) -> Unit,
    onResolve: (DownloadSource, CandidateOption) -> Unit,
    onDownload: (DownloadCandidate) -> Unit,
    onPickDownloadedFile: (DownloadCandidate) -> Unit,
    onUseInstalledApp: (DownloadCandidate) -> Unit,
    onSolveCaptcha: (DownloadCandidate) -> Unit,
    onVersionHistory: (DownloadSource) -> Unit,
    onDownloadVersion: (DownloadCandidate) -> Unit,
    onRefresh: () -> Unit,
    onCancel: () -> Unit,
    installedPackageRefreshToken: Int,
    onPrimaryActionChanged: (PrimaryAction?) -> Unit
) {
    val groups = result.sourceGroups

    // All sources disabled: show a hint instead of crashing on an empty pager.
    if (groups.isEmpty()) {
        SideEffect { onPrimaryActionChanged(null) }
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(MorpheDefaults.ItemSpacing)
        ) {
            InfoCard(
                "All sources are disabled. Enable at least one source in " +
                    "Settings → Sources to resolve or download."
            )
        }
        return
    }

    val initialPage = selectedPagerPage.coerceIn(0, (groups.size - 1).coerceAtLeast(0))
    // Plain state instead of a HorizontalPager: swiping on the page content used
    // to change the source, which users found accidental. Sources are now only
    // switched via the dropdown below.
    var currentPage by rememberSaveable { mutableIntStateOf(initialPage) }
    SideEffect { onPagerPageChanged(currentPage) }
    var showHowItWorks by remember { mutableStateOf(false) }
    // The source cards collapse by default so the page stays focused on the
    // selected source's content; the SelectedSourceBar below always shows
    // what's selected.
    var sourcesExpanded by rememberSaveable { mutableStateOf(false) }
    // Version type per source, so switching sources keeps the chosen mode.
    var subTabBySource by remember { mutableStateOf<Map<DownloadSource, SourceSubTab>>(emptyMap()) }

    // Clamp in case a source was disabled while this screen was showing.
    val currentGroup = groups[currentPage.coerceIn(0, groups.lastIndex)]
    val currentSubTab = subTabBySource[currentGroup.source]
        ?: defaultSubTab(currentGroup, request)

    val context = LocalContext.current
    val openCandidateLink: (DownloadCandidate) -> Unit = { candidate ->
        if (candidate.source == DownloadSource.PLAY) {
            context.openPlayStoreListing(candidate.packageName, candidate.url)
        } else {
            onSolveCaptcha(candidate)
        }
    }

    val action = remember(currentGroup, currentSubTab) {
        buildPrimaryAction(
            group = currentGroup,
            tab = currentSubTab,
            onResolve = onResolve,
            onDownload = onDownload,
            onVersionHistory = onVersionHistory,
            openLink = openCandidateLink
        )
    }
    SideEffect { onPrimaryActionChanged(action) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SelectedSourceBar(source = currentGroup.source)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MorpheDefaults.ItemSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tapping the title toggles the source cards.
                Row(
                    modifier = Modifier
                        .clip(MorpheDefaults.PillShape)
                        .clickable { sourcesExpanded = !sourcesExpanded }
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MorpheSectionTitle(text = "Download source", icon = Icons.Outlined.Dns)
                    if (!sourcesExpanded) {
                        Text(
                            text = "· ${groups.size}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Icon(
                        imageVector = if (sourcesExpanded) {
                            Icons.Outlined.ExpandLess
                        } else {
                            Icons.Outlined.ExpandMore
                        },
                        contentDescription = if (sourcesExpanded) "Collapse sources" else "Expand sources",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.how_it_works),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(MorpheDefaults.PillShape)
                        .clickable { showHowItWorks = !showHowItWorks }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            // Two separate expand blocks: the "How it works" card and the source
            // grid used to share one AnimatedExpand, and when the card toggled while
            // the grid was already expanded, its content stayed at the container's
            // top and the paragraph rendered over the grid (overlapping text).
            // Keeping each in its own block means no block's content height changes
            // mid-animation, so the layout can never go stale.
            AnimatedExpand(visible = showHowItWorks) {
                InfoCard(stringResource(R.string.request_how_it_works))
            }
            Box {
                DropdownMenu(
                    expanded = sourcesExpanded,
                    onDismissRequest = { sourcesExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.85f)
                ) {
                    groups.forEachIndexed { index, group ->
                        SourceMenuItem(
                            source = group.source,
                            selected = index == currentPage,
                            onClick = {
                                currentPage = index
                                sourcesExpanded = false
                            }
                        )
                    }
                }
            }
        }

        SourcePageContent(
            request = request,
            group = currentGroup,
            selectedTab = subTabBySource[currentGroup.source] ?: defaultSubTab(currentGroup, request),
            onSelectTab = { tab ->
                subTabBySource = subTabBySource + (currentGroup.source to tab)
            },
            onResolve = onResolve,
            onDownload = onDownload,
            onPickDownloadedFile = onPickDownloadedFile,
            onUseInstalledApp = onUseInstalledApp,
            onSolveCaptcha = onSolveCaptcha,
            onVersionHistory = onVersionHistory,
            onDownloadVersion = onDownloadVersion,
            installedPackageRefreshToken = installedPackageRefreshToken
        )
    }
}

internal fun subTabsFor(group: SourceCandidateGroup, request: HelperRequest): List<SourceSubTab> =
    buildList {
        if (group.manual.isNotEmpty()) add(SourceSubTab.Manual)
        if (request.hasKnownVersionRequest && group.source.supportsRecommended) {
            add(SourceSubTab.Recommended)
        }
        add(SourceSubTab.Latest)
        if (group.source.supportsHistory) {
            add(SourceSubTab.History)
        }
    }

internal fun defaultSubTab(group: SourceCandidateGroup, request: HelperRequest): SourceSubTab =
    subTabsFor(group, request).firstOrNull() ?: SourceSubTab.Latest

internal fun buildPrimaryAction(
    group: SourceCandidateGroup,
    tab: SourceSubTab,
    onResolve: (DownloadSource, CandidateOption) -> Unit,
    onDownload: (DownloadCandidate) -> Unit,
    onVersionHistory: (DownloadSource) -> Unit,
    openLink: (DownloadCandidate) -> Unit
): PrimaryAction = when (tab) {
    SourceSubTab.Manual -> {
        val first = group.manual.firstOrNull()
        PrimaryAction(
            label = if (first == null) "No manual link" else "Open source site",
            icon = Icons.Outlined.OpenInBrowser,
            enabled = first != null,
            run = { first?.let(openLink) }
        )
    }

    SourceSubTab.Recommended,
    SourceSubTab.Latest -> {
        val isLatest = tab == SourceSubTab.Latest
        val state = if (isLatest) group.latest else group.recommended
        val option = if (isLatest) CandidateOption.LATEST else CandidateOption.REQUESTED
        val resolveLabel = if (isLatest) "Find latest version" else "Find requested version"
        when (state) {
            ResolveState.Loading -> PrimaryAction(
                label = if (isLatest) "Checking latest..." else "Checking requested version...",
                icon = Icons.Outlined.Search,
                enabled = false,
                loading = true,
                run = {}
            )
            is ResolveState.Done -> {
                val direct = state.candidates.firstOrNull { it.directDownload }
                if (direct != null) {
                    PrimaryAction(
                        label = "Download ${direct.versionDisplay}",
                        icon = Icons.Outlined.Download,
                        run = { onDownload(direct) }
                    )
                } else {
                    PrimaryAction(
                        label = resolveLabel,
                        icon = Icons.Outlined.Search,
                        run = { onResolve(group.source, option) }
                    )
                }
            }
            else -> PrimaryAction(
                label = resolveLabel,
                icon = Icons.Outlined.Search,
                run = { onResolve(group.source, option) }
            )
        }
    }

    SourceSubTab.History -> when (group.history) {
        VersionHistoryState.Idle -> PrimaryAction(
            label = "Load versions",
            icon = Icons.Outlined.History,
            run = { onVersionHistory(group.source) }
        )
        VersionHistoryState.Loading -> PrimaryAction(
            label = "Loading versions...",
            icon = Icons.Outlined.History,
            enabled = false,
            loading = true,
            run = {}
        )
        else -> PrimaryAction(
            label = "Reload versions",
            icon = Icons.Outlined.Refresh,
            run = { onVersionHistory(group.source) }
        )
    }
}

@Composable
internal fun SourceGrid(
    groups: List<SourceCandidateGroup>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        groups.chunked(2).forEach { rowGroups ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowGroups.forEach { group ->
                    val index = groups.indexOfFirst { it.source == group.source }
                    SourceCard(
                        group = group,
                        selected = index == selectedIndex,
                        onClick = { onSelect(index) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowGroups.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

/** Reusable dropdown item: source brand icon + label + optional check. */
@Composable
internal fun SourceMenuItem(
    source: DownloadSource,
    selected: Boolean,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = { Text(source.label) },
        leadingIcon = { SourceAvatar(source = source, size = 24.dp) },
        trailingIcon = {
            if (selected) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        },
        onClick = onClick
    )
}

/**
 * Fill used by the list/selection cards (sources, dropdown rows, stat cards).
 * Matches the manager's card surface so every row sits on the same neutral tone
 * in both themes instead of a hand-tuned per-theme shade.
 */
@Composable
internal fun sourceCardFill(): Color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)

@Composable
internal fun sourceCardBorder(): Color = MaterialTheme.colorScheme.outlineVariant

@Composable
internal fun SourceCard(
    group: SourceCandidateGroup,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val confirmed = group.source != DownloadSource.PLAY &&
        ((group.latest as? ResolveState.Done)?.candidates?.isNotEmpty() == true ||
            (group.recommended as? ResolveState.Done)?.candidates?.isNotEmpty() == true)

    SettingsItemCard(
        onClick = onClick,
        borderWidth = if (selected) 1.5.dp else 1.dp,
        borderColor = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
        } else {
            MaterialTheme.colorScheme.outlineVariant
        },
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SourceAvatar(source = group.source)
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = group.source.label,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (confirmed) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = stringResource(R.string.available),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            RadioDot(selected = selected)
        }
    }
}

internal data class SourceBrand(
    val resId: Int
)

internal fun DownloadSource.brand(): SourceBrand = when (this) {
    DownloadSource.PLAY -> SourceBrand(resId = R.drawable.ic_src_play)
    DownloadSource.APK_MIRROR -> SourceBrand(resId = R.drawable.ic_src_apkmirror)
    DownloadSource.APK_PURE -> SourceBrand(resId = R.drawable.ic_src_apkpure)
    DownloadSource.APK_CUBE -> SourceBrand(resId = R.drawable.ic_src_apkcube)
    DownloadSource.UPTODOWN -> SourceBrand(resId = R.drawable.ic_src_uptodown)
    DownloadSource.AURORA -> SourceBrand(resId = R.drawable.ic_src_aurora)
    DownloadSource.APTOIDE -> SourceBrand(resId = R.drawable.ic_src_aptoide)
    DownloadSource.MORPHE_LINK -> SourceBrand(resId = R.drawable.ic_src_morphe)
}

@Composable
internal fun SourceAvatar(source: DownloadSource, size: Dp = 40.dp) {
    // Official brand logo: square artwork, shown as-is with rounded corners sized to the
    // artwork so a small inline icon stays a rounded square rather than turning into a circle.
    Image(
        painter = painterResource(source.brand().resId),
        contentDescription = null,
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(size / 4))
    )
}

@Composable
internal fun RadioDot(selected: Boolean) {
    Icon(
        imageVector = if (selected) {
            Icons.Outlined.RadioButtonChecked
        } else {
            Icons.Outlined.RadioButtonUnchecked
        },
        contentDescription = if (selected) "Selected" else null,
        tint = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
        },
        modifier = Modifier.size(18.dp)
    )
}

@Composable
internal fun SelectedSourceBar(source: DownloadSource) {
    SurfaceCard(
        cornerRadius = MorpheDefaults.CardCornerRadius,
        borderWidth = 1.5.dp,
        borderColor = SemanticTone.Primary.accent.copy(alpha = 0.65f),
        color = SemanticTone.Primary.container.copy(alpha = 0.4f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SourceAvatar(source = source, size = 32.dp)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = stringResource(R.string.selected_source),
                    color = SemanticTone.Primary.content.copy(alpha = 0.75f),
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = source.label,
                    fontWeight = FontWeight.Bold,
                    color = SemanticTone.Primary.content,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            ThemedIcon(
                icon = Icons.Outlined.CheckCircle,
                contentDescription = stringResource(R.string.selected),
                size = 22.dp
            )
        }
    }
}

@Composable
internal fun SourceBottomBar(
    action: PrimaryAction,
    onRefresh: () -> Unit,
    onCancel: () -> Unit
) {
    Column {
        MorpheDivider(fullWidth = true)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = MorpheDefaults.ContentPadding, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SourceSquareButton(
                icon = Icons.Outlined.Refresh,
                contentDescription = stringResource(R.string.refresh),
                onClick = onRefresh
            )
            SourceSquareButton(
                icon = Icons.Outlined.Close,
                contentDescription = stringResource(R.string.cancel),
                onClick = onCancel
            )
            val primaryInteractionSource = remember { MutableInteractionSource() }
            Button(
                onClick = action.run,
                enabled = action.enabled,
                modifier = Modifier
                    .weight(1f)
                    .height(MorpheDefaults.DialogButtonHeight)
                    .pressScale(primaryInteractionSource, enabled = action.enabled),
                interactionSource = primaryInteractionSource,
                shape = RoundedCornerShape(MorpheDefaults.CardCornerRadius),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.48f)),
                contentPadding = PaddingValues(horizontal = MorpheDefaults.ContentPadding)
            ) {
                if (action.loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = action.icon,
                        contentDescription = null,
                        modifier = Modifier.size(MorpheDefaults.IconSizeSmall)
                    )
                }
                Spacer(Modifier.width(MorpheDefaults.ContentPaddingSmall))
                Text(
                    action.label,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
internal fun SourceSquareButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    HelperIconButton(
        icon = icon,
        contentDescription = contentDescription,
        onClick = onClick
    )
}

internal enum class SourceSubTab(
    val label: String,
    val icon: ImageVector
) {
    Manual("Manual", Icons.Outlined.Tune),
    Recommended("Recommended", Icons.Outlined.CheckCircle),
    Latest("Latest", Icons.Outlined.Star),
    History("History", Icons.Outlined.History)
}

/** Sources that can resolve the exact requested version from their own data. */
internal val DownloadSource.supportsRecommended: Boolean
    get() = when (this) {
        DownloadSource.AURORA,
        DownloadSource.PLAY -> false
        else -> true
    }

/** Sources that expose a version history list. */
internal val DownloadSource.supportsHistory: Boolean
    get() = when (this) {
        // APKCube has an old-versions page but no history list. Aurora/Play never
        // offer originals.
        DownloadSource.APK_CUBE,
        DownloadSource.AURORA,
        DownloadSource.PLAY -> false
        else -> true
    }

@Composable
internal fun SourcePageContent(
    request: HelperRequest,
    group: SourceCandidateGroup,
    selectedTab: SourceSubTab,
    onSelectTab: (SourceSubTab) -> Unit,
    onResolve: (DownloadSource, CandidateOption) -> Unit,
    onDownload: (DownloadCandidate) -> Unit,
    onPickDownloadedFile: (DownloadCandidate) -> Unit,
    onUseInstalledApp: (DownloadCandidate) -> Unit,
    onSolveCaptcha: (DownloadCandidate) -> Unit,
    onVersionHistory: (DownloadSource) -> Unit,
    onDownloadVersion: (DownloadCandidate) -> Unit,
    installedPackageRefreshToken: Int
) {
    val subTabs = remember(group, request) { subTabsFor(group, request) }
    val safeTab = subTabs.firstOrNull { it == selectedTab } ?: subTabs.firstOrNull()
        ?: SourceSubTab.Latest

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MorpheSectionTitle(text = "Version type", icon = Icons.Outlined.Tune)
            VersionTypeRow(
                tabs = subTabs,
                selected = safeTab,
                onSelect = onSelectTab
            )
        }

        AboutModeCard(tab = safeTab)

        when (safeTab) {
            SourceSubTab.Manual -> {
                group.manual.forEach { candidate ->
                    CandidateCard(
                        request = request,
                        candidate = candidate,
                        onDownload = { onDownload(candidate) },
                        onPickDownloadedFile = { onPickDownloadedFile(candidate) },
                        onUseInstalledApp = { onUseInstalledApp(candidate) },
                        onSolveCaptcha = { onSolveCaptcha(candidate) },
                        installedPackageRefreshToken = installedPackageRefreshToken
                    )
                }
            }

            SourceSubTab.Recommended -> {
                CandidateResolveSection(
                    request = request,
                    state = group.recommended,
                    emptyText = "Requested version was not found on this source. Use Manual mode for this source instead.",
                    onDownload = onDownload,
                    onPickDownloadedFile = onPickDownloadedFile,
                    onUseInstalledApp = onUseInstalledApp,
                    onSolveCaptcha = onSolveCaptcha,
                    installedPackageRefreshToken = installedPackageRefreshToken
                )
            }

            SourceSubTab.Latest -> {
                when (group.source) {
                    DownloadSource.AURORA -> {
                        InfoCard("Aurora only provides the latest Play Store version. Use Manual mode if you need a specific version.")
                    }
                    DownloadSource.PLAY -> {
                        InfoCard("Play opens the official Play Store listing for this app. Use Manual mode if you need a specific version.")
                    }
                    else -> Unit
                }
                CandidateResolveSection(
                    request = request,
                    state = group.latest,
                    emptyText = "Latest version was not found on this source. Use Manual mode for this source instead.",
                    onDownload = onDownload,
                    onPickDownloadedFile = onPickDownloadedFile,
                    onUseInstalledApp = onUseInstalledApp,
                    onSolveCaptcha = onSolveCaptcha,
                    installedPackageRefreshToken = installedPackageRefreshToken
                )
            }

            SourceSubTab.History -> {
                VersionHistorySection(
                    state = group.history,
                    onDownloadVersion = onDownloadVersion,
                    onSolveCaptcha = onSolveCaptcha
                )
            }
        }
    }
}

@Composable
internal fun VersionTypeRow(
    tabs: List<SourceSubTab>,
    selected: SourceSubTab,
    onSelect: (SourceSubTab) -> Unit
) {
    MorpheSelectorRow(
        options = tabs.map { MorpheSelectorOption(label = it.label, icon = it.icon) },
        selectedIndex = tabs.indexOf(selected),
        onSelect = { onSelect(tabs[it]) },
        // labelLarge keeps four-in-a-row tab labels on one line at phone widths.
        labelStyle = MaterialTheme.typography.labelLarge
    )
}

/**
 * Settings tabs, mirroring the manager's three-tab organisation: appearance,
 * how files come and go, and the troubleshooting/scanning corner.
 */
internal enum class SettingsTab(
    val label: String,
    val icon: ImageVector
) {
    APPEARANCE("Appearance", Icons.Outlined.Palette),
    SYSTEM("System", Icons.Outlined.PhoneAndroid),
    ADVANCED("Advanced", Icons.Outlined.Tune),
    // Request logs get a tab of their own: in Advanced they sat under the
    // scanning settings and made that tab scroll for pages.
    LOGS("Logs", Icons.Outlined.BugReport)
}

@Composable
internal fun SettingsTabRow(
    selected: SettingsTab,
    onSelect: (SettingsTab) -> Unit
) {
    MorpheSelectorRow(
        options = SettingsTab.entries.map { MorpheSelectorOption(label = it.label, icon = it.icon) },
        selectedIndex = SettingsTab.entries.indexOf(selected),
        onSelect = { onSelect(SettingsTab.entries[it]) },
        // Four tabs across, so keep the labels on the smaller step.
        labelStyle = MaterialTheme.typography.labelLarge
    )
}

@Composable
internal fun AboutModeCard(tab: SourceSubTab) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val title = when (tab) {
        SourceSubTab.Manual -> "About manual mode"
        SourceSubTab.Recommended -> "About recommended mode"
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
            .clip(RoundedCornerShape(MorpheDefaults.CompactCornerRadius)),
        shape = RoundedCornerShape(MorpheDefaults.CompactCornerRadius),
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.05f)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(MorpheDefaults.CompactCornerRadius))
                    .clickable { expanded = !expanded }
                    .padding(horizontal = MorpheDefaults.ContentPadding, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
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
                MorpheDivider(fullWidth = true)
                Text(
                    text = description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(
                        horizontal = MorpheDefaults.ContentPadding,
                        vertical = MorpheDefaults.ContentPadding
                    )
                )
            }
        }
    }
}

@Composable
internal fun VersionHistorySection(
    state: VersionHistoryState,
    onDownloadVersion: (DownloadCandidate) -> Unit,
    onSolveCaptcha: (DownloadCandidate) -> Unit
) {
    when (state) {
        // Idle and Loading are driven entirely by the bottom-bar primary
        // action button; the content area shows results only.
        VersionHistoryState.Idle,
        VersionHistoryState.Loading -> Unit

        is VersionHistoryState.Error -> {
            InfoCard(state.message)
        }

        is VersionHistoryState.Done -> {
            if (state.candidates.isEmpty()) {
                InfoCard("No version list was available for this source.")
            } else {
                state.candidates.forEach { candidate ->
                    VersionHistoryRow(
                        candidate = candidate,
                        showOpenLink = candidate.identityKey() in state.noDirectDownloadKeys,
                        onDownloadVersion = { onDownloadVersion(candidate) },
                        onSolveCaptcha = { onSolveCaptcha(candidate) }
                    )
                }
            }
        }
    }
}

@Composable
internal fun VersionHistoryRow(
    candidate: DownloadCandidate,
    showOpenLink: Boolean,
    onDownloadVersion: () -> Unit,
    onSolveCaptcha: (DownloadCandidate) -> Unit
) {
    val context = LocalContext.current
    SurfaceCard(cornerRadius = MorpheDefaults.CompactCornerRadius) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MorpheDefaults.ContentPadding),
            horizontalArrangement = Arrangement.spacedBy(MorpheDefaults.ItemSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = candidate.versionDisplay,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = when {
                        // Captcha-gated rows are browsed in the in-app captcha
                        // browser, not downloaded.
                        candidate.captchaUrl != null && !candidate.directDownload ->
                            "Browsable in the in-app browser (version history)"
                        showOpenLink -> "No direct download  open the version page"
                        // History rows know only the version page until the
                        // user taps Download, which resolves the real format.
                        // "web" is a placeholder, not an actual file type, so
                        // don't render it as if the row just links out.
                        candidate.fileKind.equals("web", ignoreCase = true) ->
                            "Direct download  format resolved on download"
                        else -> candidate.fileKind.uppercase(Locale.US)
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            when {
                (showOpenLink || (candidate.captchaUrl != null && !candidate.directDownload)) &&
                    candidate.source != DownloadSource.AURORA &&
                    candidate.source != DownloadSource.PLAY -> {
                    HelperButton(
                        text = "Solve captcha",
                        onClick = { onSolveCaptcha(candidate) },
                        icon = Icons.Outlined.VerifiedUser,
                        modifier = Modifier.widthIn(min = 140.dp)
                    )
                }
                showOpenLink -> {
                    HelperOutlinedButton(
                        text = "Open in app",
                        onClick = {
                            if (candidate.source == DownloadSource.PLAY) {
                                context.openPlayStoreListing(candidate.packageName, candidate.url)
                            } else {
                                onSolveCaptcha(candidate)
                            }
                        },
                        icon = Icons.Outlined.OpenInBrowser,
                        modifier = Modifier.widthIn(min = 120.dp)
                    )
                }
                else -> {
                    HelperButton(
                        text = "Download",
                        onClick = onDownloadVersion,
                        icon = Icons.Outlined.Download,
                        modifier = Modifier.widthIn(min = 120.dp)
                    )
                }
            }
        }
    }
}

@Composable
internal fun CandidateResolveSection(
    request: HelperRequest,
    state: ResolveState,
    emptyText: String,
    onDownload: (DownloadCandidate) -> Unit,
    onPickDownloadedFile: (DownloadCandidate) -> Unit,
    onUseInstalledApp: (DownloadCandidate) -> Unit,
    onSolveCaptcha: (DownloadCandidate) -> Unit,
    installedPackageRefreshToken: Int
) {
    when (state) {
        // Idle and Loading are driven entirely by the bottom-bar primary
        // action button; the content area shows results only.
        ResolveState.Idle,
        ResolveState.Loading -> Unit

        is ResolveState.Done -> {
            if (state.candidates.isEmpty()) {
                InfoCard(emptyText)
            } else {
                state.candidates.forEach { candidate ->
                    CandidateCard(
                        request = request,
                        candidate = candidate,
                        onDownload = { onDownload(candidate) },
                        onPickDownloadedFile = { onPickDownloadedFile(candidate) },
                        onUseInstalledApp = { onUseInstalledApp(candidate) },
                        onSolveCaptcha = { onSolveCaptcha(candidate) },
                        installedPackageRefreshToken = installedPackageRefreshToken
                    )
                }
            }
        }

        is ResolveState.Error -> {
            InfoCard(state.message)
            state.fallbackCandidate?.let { candidate ->
                CandidateCard(
                    request = request,
                    candidate = candidate,
                    onDownload = { onDownload(candidate) },
                    onPickDownloadedFile = { onPickDownloadedFile(candidate) },
                    onUseInstalledApp = { onUseInstalledApp(candidate) },
                    onSolveCaptcha = { onSolveCaptcha(candidate) },
                    installedPackageRefreshToken = installedPackageRefreshToken
                )
            }
        }
    }
}



/**
 * Informational paragraph  the manager's `InfoBox` container, kept as the helper's one-line
 * note component.
 */
@Composable
internal fun InfoCard(text: String) {
    InfoBox(
        title = text,
        titleColor = MaterialTheme.colorScheme.onSurfaceVariant
    ) {}
}

internal enum class HistoryFilter(val label: String) {
    All("All"),
    Scanned("Scanned"),
    Flagged("Flagged")
}

@Composable
internal fun DownloadHistorySection(
    entries: List<DownloadHistoryEntry>,
    onClear: () -> Unit,
    onOpen: (DownloadHistoryEntry) -> Unit,
    onShare: (DownloadHistoryEntry) -> Unit
) {
    val context = LocalContext.current
    var filter by rememberSaveable { mutableStateOf(HistoryFilter.All) }
    val filtered = entries.filter { entry ->
        when (filter) {
            HistoryFilter.All -> true
            HistoryFilter.Scanned -> entry.scanVerdict != null
            HistoryFilter.Flagged -> entry.scanVerdict?.malicious == true
        }
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MorpheDefaults.ItemSpacing)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MorpheDefaults.ItemSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.download_history),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            HelperOutlinedButton(
                text = "Clear",
                onClick = onClear,
                modifier = Modifier.width(MorpheDefaults.CompactButtonWidth)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HistoryFilter.entries.forEach { f ->
                val count = when (f) {
                    HistoryFilter.All -> entries.size
                    HistoryFilter.Scanned -> entries.count { it.scanVerdict != null }
                    HistoryFilter.Flagged -> entries.count { it.scanVerdict?.malicious == true }
                }
                MorpheFilterChip(
                    selected = f == filter,
                    onClick = { filter = f },
                    label = "${f.label} · $count",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (entries.isEmpty()) {
            InfoCard("No hand-offs recorded yet. Downloads and picked files you return to Morphe show up here.")
        } else if (filtered.isEmpty()) {
            InfoCard(
                when (filter) {
                    HistoryFilter.All -> "No hand-offs recorded yet."
                    HistoryFilter.Scanned -> "None of these downloads were scanned by VirusTotal."
                    HistoryFilter.Flagged -> "No flagged downloads — everything came back clean."
                }
            )
        } else {
            filtered.forEach { entry ->
                val usable = remember(entry.uri) { context.isHistoryUriUsable(entry.uri) }
                HistoryEntryCard(
                    entry = entry,
                    usable = usable,
                    onOpen = { onOpen(entry) },
                    onShare = { onShare(entry) }
                )
            }
        }
    }
}

@Composable
internal fun RequestLogsCard(
    logs: List<RequestLogEntry>,
    onClearLogs: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MorpheDefaults.ItemSpacing)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MorpheDefaults.ItemSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.request_logs),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            HelperOutlinedButton(
                text = "Share",
                icon = Icons.Outlined.Share,
                onClick = {
                    val share = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "Morphe Helper logs")
                        putExtra(Intent.EXTRA_TEXT, AppLog.exportText())
                    }
                    context.startActivity(Intent.createChooser(share, "Share logs"))
                },
                // Icon + text needs more room than the plain "Clear" button.
                modifier = Modifier.widthIn(min = 120.dp)
            )
            HelperOutlinedButton(
                text = "Clear",
                onClick = onClearLogs,
                modifier = Modifier.width(MorpheDefaults.CompactButtonWidth)
            )
        }

        if (logs.isEmpty()) {
            InfoCard("No logs yet.")
        } else {
            SurfaceCard(cornerRadius = MorpheDefaults.CompactCornerRadius) {
                SelectionContainer {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(MorpheDefaults.ContentPadding),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        logs.takeLast(80).forEach { entry ->
                            Text(
                                text = "${entry.time} ${entry.level.badge} ${entry.message}",
                                color = entry.level.color(),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}





@Composable
internal fun EmptyLaunchState(
    onOpenMorphe: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(MorpheDefaults.ItemSpacing)) {
        MorpheEmptyState(
            message = stringResource(R.string.empty_launch_message),
            icon = Icons.Outlined.Download
        )
        HelperSetupCard()
        HelperButton(
            text = "Open Morphe Manager",
            onClick = onOpenMorphe,
            icon = Icons.Outlined.OpenInNew,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
internal fun HelperSetupCard() {
    val colors = MaterialTheme.colorScheme
    SectionCard {
        Column(
            modifier = Modifier.padding(MorpheDefaults.ContentPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = stringResource(R.string.setup_title),
                    color = colors.onSurface,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            listOf(
                R.string.setup_step_open_settings,
                R.string.setup_step_download_helper,
                R.string.setup_step_trust
            ).forEachIndexed { index, step ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${index + 1}.",
                        color = colors.primary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(step),
                        color = colors.onSurface,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Text(
                text = stringResource(R.string.setup_footer),
                color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

