package dev.rushi.apkdownloadhelper.ui.screen.helper

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Notifications
import dev.rushi.apkdownloadhelper.ui.shared.button.GlassHeaderButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.rushi.apkdownloadhelper.BuildConfig
import dev.rushi.apkdownloadhelper.R
import dev.rushi.apkdownloadhelper.storage.HelperSettings
import dev.rushi.apkdownloadhelper.ui.shared.layout.AppWordmark
import dev.rushi.apkdownloadhelper.ui.shared.state.EmptyLaunchState
import dev.rushi.apkdownloadhelper.ui.theme.AppColors
import dev.rushi.apkdownloadhelper.ui.theme.AppDefaults
import dev.rushi.apkdownloadhelper.ui.theme.hazeSourceIfEnabled
import dev.rushi.apkdownloadhelper.ui.theme.glassFill
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.outlined.DownloadDone
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.core.content.ContextCompat
import dev.rushi.apkdownloadhelper.isHistoryUriUsable
import dev.rushi.apkdownloadhelper.storage.DownloadHistoryEntry
import dev.rushi.apkdownloadhelper.storage.DownloadHistoryStore
import dev.rushi.apkdownloadhelper.tracking.TrackingCheckScheduler
import dev.rushi.apkdownloadhelper.ui.model.FavouriteApps
import dev.rushi.apkdownloadhelper.ui.model.TrackedAppChanges
import dev.rushi.apkdownloadhelper.ui.model.TrackedAppSnapshot
import dev.rushi.apkdownloadhelper.ui.model.TrackingStore
import dev.rushi.apkdownloadhelper.ui.model.trackedChanges
import dev.rushi.apkdownloadhelper.ui.model.trackingPackageKey
import dev.rushi.apkdownloadhelper.ui.shared.appcard.AppListCard
import dev.rushi.apkdownloadhelper.ui.shared.text.SectionTitle
import dev.rushi.apkdownloadhelper.ui.theme.animatedListItem
import dev.rushi.apkdownloadhelper.ui.theme.pressScale
import dev.rushi.apkdownloadhelper.ui.theme.rememberMorphePillShape
import dev.rushi.apkdownloadhelper.updater.ArchiveApp
import dev.rushi.apkdownloadhelper.updater.MorpheArchive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class StoredApkCardItem(
    val entry: DownloadHistoryEntry,
    val app: ArchiveApp,
    val installed: Boolean
)

private val usableUriCache = java.util.concurrent.ConcurrentHashMap<String, Boolean>()

private fun android.content.Context.isHistoryUriUsableCached(uri: String): Boolean =
    usableUriCache[uri] ?: isHistoryUriUsable(uri).also { usableUriCache[uri] = it }

private fun loadStoredApkItems(context: android.content.Context): List<StoredApkCardItem> {
    val archiveAppsByPkg = runCatching {
        MorpheArchive.loadCachedIndex(context)?.apps?.associateBy { it.packageName }
    }.getOrNull().orEmpty()
    val installedCache = mutableMapOf<String, Boolean>()

    return DownloadHistoryStore.entries(context)
        .filter { context.isHistoryUriUsableCached(it.uri) }
        .map { entry ->
            val baseApp = archiveAppsByPkg[entry.packageName]
            val app = baseApp?.copy(
                name = entry.appName.ifBlank { baseApp.name }
            ) ?: ArchiveApp(
                packageName = entry.packageName,
                name = entry.appName,
                versions = listOfNotNull(entry.versionName)
            )
            val installed = installedCache.getOrPut(entry.packageName) {
                runCatching {
                    context.packageManager.getPackageInfo(entry.packageName, 0) != null
                }.getOrDefault(false)
            }
            StoredApkCardItem(entry = entry, app = app, installed = installed)
        }
}

//==================================================
// Normal App Open
//==================================================

@Composable
internal fun HelperStandaloneContent(
    settings: HelperSettings,
    onSettingsChange: (HelperSettings) -> Unit,
    onOpenMorphe: () -> Unit,
    onOpenTracking: () -> Unit = {},
    hideTracking: Boolean = false,
    onOpenApp: (ArchiveApp) -> Unit = {}
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val headerHaze = remember { HazeState() }
    var headerHeightDp by remember { mutableStateOf(0.dp) }
    val isScrolled by remember(listState) {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
        }
    }

    var storedItems by remember { mutableStateOf<List<StoredApkCardItem>?>(null) }
    var entryToDelete by remember { mutableStateOf<DownloadHistoryEntry?>(null) }

    LaunchedEffect(Unit) {
        storedItems = withContext(Dispatchers.IO) {
            loadStoredApkItems(context)
        }
    }

    fun deleteStoredEntry(entry: DownloadHistoryEntry) {
        val uri = android.net.Uri.parse(entry.uri)
        runCatching {
            if (entry.uri.startsWith("content://")) {
                context.contentResolver.delete(uri, null, null)
            } else {
                uri.path?.let { java.io.File(it).delete() }
            }
        }
        storedItems = storedItems?.filterNot {
            it.entry.uri == entry.uri && it.entry.timestamp == entry.timestamp
        }
    }

    fun shareStoredEntry(entry: DownloadHistoryEntry) {
        val uri = android.net.Uri.parse(entry.uri)
        val mime = if (entry.fileName.endsWith(".apk", ignoreCase = true)) {
            "application/vnd.android.package-archive"
        } else {
            "application/octet-stream"
        }
        val share = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = mime
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            clipData = android.content.ClipData.newUri(context.contentResolver, entry.fileName, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching {
            context.startActivity(
                android.content.Intent.createChooser(
                    share,
                    "Share ${entry.fileName}"
                )
            )
        }
    }

    entryToDelete?.let { target ->
        dev.rushi.apkdownloadhelper.ui.shared.dialog.AppAlertDialog(
            onDismissRequest = { entryToDelete = null },
            title = {
                Text(
                    text = "Delete APK?",
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
            },
            text = { Text("Delete ${target.fileName} from storage?") },
            confirmButton = {
                val deleteInteraction = remember { MutableInteractionSource() }
                androidx.compose.material3.TextButton(
                    onClick = {
                        entryToDelete = null
                        deleteStoredEntry(target)
                    },
                    interactionSource = deleteInteraction,
                    modifier = Modifier.pressScale(deleteInteraction, restCorner = 20.dp, pressedCorner = 10.dp)
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                val cancelInteraction = remember { MutableInteractionSource() }
                androidx.compose.material3.TextButton(
                    onClick = { entryToDelete = null },
                    interactionSource = cancelInteraction,
                    modifier = Modifier.pressScale(cancelInteraction, restCorner = 20.dp, pressedCorner = 10.dp)
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val items = storedItems
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .hazeSourceIfEnabled(headerHaze),
            contentPadding = PaddingValues(
                top = headerHeightDp + 10.dp,
                start = AppDefaults.ContentPadding,
                end = AppDefaults.ContentPadding,
                bottom = 160.dp
            ),
            verticalArrangement = Arrangement.spacedBy(
                if (!items.isNullOrEmpty()) 3.dp else AppDefaults.ItemSpacing
            )
        ) {
            if (items.isNullOrEmpty()) {
                item(key = "empty_launch") {
                    EmptyLaunchState(onOpenMorphe = onOpenMorphe)
                }
            } else {
                itemsIndexed(
                    items = items,
                    key = { index, item -> "${item.entry.uri}_${item.entry.timestamp}_$index" }
                ) { index, item ->
                    val isFirst = index == 0
                    val isLast = index == items.lastIndex
                    val outerRadius = 22.dp
                    val innerRadius = 10.dp
                    val cardShape = RoundedCornerShape(
                        topStart = if (isFirst) outerRadius else innerRadius,
                        topEnd = if (isFirst) outerRadius else innerRadius,
                        bottomStart = if (isLast) outerRadius else innerRadius,
                        bottomEnd = if (isLast) outerRadius else innerRadius
                    )
                    val versionLine = listOfNotNull(
                        item.entry.versionName?.takeIf { it.isNotBlank() } ?: item.entry.fileName,
                        item.entry.sourceName.takeIf { it.isNotBlank() }
                    ).joinToString(" · ")

                    AppListCard(
                        app = item.app,
                        installed = item.installed,
                        favourite = false,
                        shape = cardShape,
                        useGradient = settings.useGradientCardColors,
                        customBottomText = versionLine,
                        onToggleFavourite = {},
                        onShare = { shareStoredEntry(item.entry) },
                        onDelete = { entryToDelete = item.entry },
                        onClick = { onOpenApp(item.app) },
                        modifier = Modifier.animatedListItem(this)
                    )
                }
            }
        }

        HelperFloatingHeader(
            settings = settings,
            onSettingsChange = onSettingsChange,
            onOpenTracking = onOpenTracking,
            hideTracking = hideTracking,
            isScrolled = isScrolled,
            headerHaze = headerHaze,
            onHeaderHeightChanged = { headerHeightDp = it },
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
private fun HelperFloatingHeader(
    settings: HelperSettings,
    onSettingsChange: (HelperSettings) -> Unit,
    onOpenTracking: () -> Unit,
    hideTracking: Boolean,
    isScrolled: Boolean,
    headerHaze: HazeState,
    onHeaderHeightChanged: (Dp) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val headerShape = RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { size ->
                onHeaderHeightChanged(with(density) { size.height.toDp() })
            }
            .then(
                if (isScrolled) {
                    Modifier.glassFill(
                        glass = AppColors.glassContainerColor(),
                        shape = headerShape,
                        hazeState = headerHaze
                    )
                } else {
                    Modifier
                }
            )
            .statusBarsPadding()
            .padding(
                horizontal = AppDefaults.ContentPadding,
                vertical = 12.dp
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                AppWordmark(
                    modifier = Modifier.height(26.dp),
                    textColor = MaterialTheme.colorScheme.onBackground
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.helper),
                        style = MaterialTheme.typography.bodySmall.copy(
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    Text(
                        text = "v${BuildConfig.VERSION_NAME}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall.copy(
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
            }

            if (!hideTracking) {
                GlassHeaderButton(
                    icon = Icons.Outlined.Notifications,
                    contentDescription = "Tracking",
                    onClick = onOpenTracking
                )
            }

            HelperGlassBoltButton(
                fastMode = settings.fastMode,
                onClick = {
                    val enabled = !settings.fastMode
                    onSettingsChange(settings.copy(fastMode = enabled))
                    Toast.makeText(
                        context,
                        if (enabled) "Fast Mode on" else "Fast Mode off",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }
    }
}

@Composable
private fun HelperGlassBoltButton(
    fastMode: Boolean,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val idleAlpha = if (isDark) 0.35f else 0.6f
    val activeAlpha = if (isDark) 0.55f else 0.72f
    val baseContainer = if (fastMode) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = activeAlpha)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = idleAlpha)
    }
    val containerColor by animateColorAsState(
        targetValue = if (pressed) {
            baseContainer.copy(alpha = (baseContainer.alpha * 0.75f).coerceAtLeast(0.18f))
        } else {
            baseContainer
        },
        animationSpec = tween(200),
        label = "helper_bolt_container"
    )
    val contentColor by animateColorAsState(
        targetValue = if (fastMode) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(200),
        label = "helper_bolt_content"
    )
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = tween(200),
        label = "helper_bolt_scale"
    )

    Surface(
        onClick = onClick,
        modifier = Modifier
            .size(40.dp)
            .pressScale(interactionSource, restCorner = 20.dp, pressedCorner = 10.dp, pressedScale = 0.90f),
        shape = rememberMorphePillShape(interactionSource),
        color = containerColor,
        contentColor = contentColor,
        interactionSource = interactionSource
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Outlined.Bolt,
                contentDescription = if (fastMode) "Fast Mode on" else "Fast Mode off",
                modifier = Modifier.size(20.dp)
            )
        }
    }
}