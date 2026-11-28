package dev.rushi.apkdownloadhelper.ui.shared.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import dev.rushi.apkdownloadhelper.ui.shared.button.DialogButton
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.HelpOutline
import android.net.Uri
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.SdStorage
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.rushi.apkdownloadhelper.R
import dev.rushi.apkdownloadhelper.storage.DownloadHistoryEntry
import dev.rushi.apkdownloadhelper.storage.DownloadLocation
import dev.rushi.apkdownloadhelper.storage.customFolderRelativePath
import dev.rushi.apkdownloadhelper.storage.loadHelperSettings
import dev.rushi.apkdownloadhelper.storage.storageLocation
import dev.rushi.apkdownloadhelper.ui.shared.appcard.AppAvatar
import dev.rushi.apkdownloadhelper.ui.shared.chip.StatusBadge
import dev.rushi.apkdownloadhelper.ui.shared.chip.StatusBadgeRow
import dev.rushi.apkdownloadhelper.ui.shared.icon.ThemedIcon
import dev.rushi.apkdownloadhelper.ui.theme.SemanticTone
import dev.rushi.apkdownloadhelper.ui.util.formatBytes
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import dev.rushi.apkdownloadhelper.ui.shared.scroll.verticalScrollFade
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

/** A previously downloaded file offered for reuse, with its size on disk. */
internal data class ReuseOption(
    val entry: DownloadHistoryEntry,
    val sizeBytes: Long
)

@Composable
internal fun ReuseOfferDialog(
    options: List<ReuseOption>,
    onUseExisting: (ReuseOption) -> Unit,
    onDownloadNew: () -> Unit,
    useItButtonColor: Color = AppDialogDefaults.confirmButtonContainer()
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val customTreeUri = remember { context.loadHelperSettings().downloadCustomTreeUri }
    val screenHeightDp = LocalConfiguration.current.screenHeightDp.dp
    val systemBarsVertical = with(density) {
        (WindowInsets.systemBars.getTop(this) + WindowInsets.systemBars.getBottom(this)).toDp()
    }
    val maxCardsHeight = (screenHeightDp - systemBarsVertical - 210.dp).coerceAtLeast(320.dp)
    val cardsScrollState = rememberScrollState()
    var selectedIndex by remember(options) { mutableIntStateOf(0) }
    var showCards by remember(options) { mutableStateOf(false) }

    LaunchedEffect(options) {
        // Wait for MorpheDialogEnter (220ms) to finish before expanding the cards
        delay(230L)
        showCards = true
    }

    AppAlertDialog(
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.a_previous_download_for_this_exact),
                    style = MaterialTheme.typography.bodyLarge,
                    color = scheme.onSurfaceVariant
                )

                AnimatedVisibility(
                    visible = showCards,
                    enter = expandVertically(
                        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
                        expandFrom = Alignment.CenterVertically
                    ) + fadeIn(
                        animationSpec = tween(durationMillis = 220, delayMillis = 50, easing = FastOutSlowInEasing)
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = maxCardsHeight)
                            .verticalScrollFade(cardsScrollState, length = 24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(cardsScrollState)
                                .padding(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            options.forEachIndexed { index, option ->
                        val entry = option.entry
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(scheme.surfaceColorAtElevation(4.dp))
                                .clickable(enabled = options.size > 1) { selectedIndex = index }
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                                // App icon + name + package
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AppAvatar(
                                        packageName = entry.packageName,
                                        initial = entry.appName.firstOrNull()?.uppercaseChar() ?: '?'
                                    )
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = entry.appName,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = scheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = entry.packageName,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = scheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (options.size > 1) {
                                        SelectCircle(selected = index == selectedIndex)
                                    }
                                }

                                // Version, source, format & file size pills
                                StatusBadgeRow(modifier = Modifier.fillMaxWidth()) {
                                    entry.versionName?.let { version ->
                                        StatusBadge(
                                            text = version,
                                            icon = Icons.Outlined.History,
                                            tone = SemanticTone.Primary
                                        )
                                    }
                                    StatusBadge(
                                        text = entry.sourceName,
                                        containerColor = scheme.surfaceVariant.copy(alpha = 0.65f),
                                        contentColor = scheme.onSurface
                                    )
                                    if (entry.fileKind.isNotBlank() && !entry.fileKind.equals("web", ignoreCase = true)) {
                                        StatusBadge(
                                            text = entry.fileKind.uppercase(),
                                            containerColor = scheme.surfaceVariant.copy(alpha = 0.65f),
                                            contentColor = scheme.onSurfaceVariant
                                        )
                                    }
                                    if (option.sizeBytes > 0L) {
                                        StatusBadge(
                                            text = option.sizeBytes.formatBytes(),
                                            containerColor = scheme.surfaceVariant.copy(alpha = 0.65f),
                                            contentColor = scheme.onSurfaceVariant
                                        )
                                    }
                                }

                                val locationLabel = remember(entry.uri, entry.storage, entry.isCached) {
                                    val loc = entry.storageLocation()
                                    when {
                                        entry.isCached -> "Cached (temporary)"
                                        loc == DownloadLocation.INTERNAL -> "Internal app storage"
                                        loc == DownloadLocation.EXTERNAL -> "Downloads/helper-for-morphe"
                                        else -> customFolderRelativePath(
                                            context,
                                            Uri.parse(entry.uri),
                                            customTreeUri
                                        ) ?: "Custom folder"
                                    }
                                }

                                // Storage location + file name strip
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(scheme.surfaceVariant.copy(alpha = 0.35f))
                                        .padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.SdStorage,
                                            contentDescription = null,
                                            tint = scheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = locationLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = scheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.FolderOpen,
                                            contentDescription = null,
                                            tint = scheme.onSurfaceVariant,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = entry.fileName,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = scheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                // Scan verdict (if any)
                                entry.scanVerdict?.let { verdict ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        ThemedIcon(
                                            icon = when {
                                                verdict.malicious -> Icons.Outlined.Warning
                                                verdict.failed -> Icons.Outlined.HelpOutline
                                                else -> Icons.Outlined.CheckCircle
                                            },
                                            size = 15.dp,
                                            tint = when {
                                                verdict.malicious -> SemanticTone.Error.accent
                                                verdict.failed -> SemanticTone.Warning.accent
                                                else -> SemanticTone.Primary.accent
                                            }
                                        )
                                        Text(
                                            text = verdict.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Medium,
                                            color = if (verdict.malicious) {
                                                scheme.error
                                            } else {
                                                scheme.onSurfaceVariant
                                            },
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppDialogDefaults.ButtonSpacing)
                ) {
                    DialogButton(
                        text = "Download new",
                        onClick = onDownloadNew,
                        filled = false,
                        icon = Icons.Outlined.Download,
                        modifier = Modifier.weight(1f)
                    )
                    DialogButton(
                        text = "Use it",
                        onClick = { options.getOrNull(selectedIndex)?.let(onUseExisting) },
                        icon = Icons.Outlined.CheckCircle,
                        buttonColor = useItButtonColor,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {}
    )
}
