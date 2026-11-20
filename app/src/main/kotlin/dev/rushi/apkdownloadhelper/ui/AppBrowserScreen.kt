package dev.rushi.apkdownloadhelper.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.LruCache
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.SortByAlpha
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.rushi.apkdownloadhelper.HelperButton
import dev.rushi.apkdownloadhelper.MORPHE_MANAGER_PACKAGES
import dev.rushi.apkdownloadhelper.R
import dev.rushi.apkdownloadhelper.download.openPlayStoreListing
import dev.rushi.apkdownloadhelper.download.playStoreUrl
import dev.rushi.apkdownloadhelper.updater.ArchiveApp
import dev.rushi.apkdownloadhelper.updater.ArchiveFreshness
import dev.rushi.apkdownloadhelper.updater.ArchiveSource
import dev.rushi.apkdownloadhelper.updater.MorpheArchive
import dev.rushi.apkdownloadhelper.updater.archiveFreshness
import dev.rushi.apkdownloadhelper.updater.formatBundleRelease
import dev.rushi.apkdownloadhelper.updater.formatReleaseDate
import dev.rushi.apkdownloadhelper.updater.groupArchiveSources
import dev.rushi.apkdownloadhelper.updater.newestReleaseDate
import dev.rushi.apkdownloadhelper.tracking.TrackingCheckScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.Locale
import kotlin.coroutines.resume
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import java.util.concurrent.ConcurrentHashMap
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Public
import dev.rushi.apkdownloadhelper.updater.MorpheArchiveIndex
import androidx.compose.foundation.basicMarquee
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.util.lerp
import dev.rushi.apkdownloadhelper.updater.ArchiveChanges
import dev.rushi.apkdownloadhelper.updater.bundleVersionOf
import dev.rushi.apkdownloadhelper.updater.releaseDateOf
import kotlin.math.roundToInt

internal enum class AppListTab(
    val label: String,
    val icon: ImageVector,
    val contentDescription: String
) {
    All("All", Icons.Outlined.ViewList, "All"),
    Favourites("Liked", Icons.Outlined.FavoriteBorder, "Liked"),
    Tracked("Tracked", Icons.Outlined.Notifications, "Tracked"),
    Installed("Installed", Icons.Outlined.CheckCircle, "Installed"),
    NotInstalled("Not installed", Icons.Outlined.Block, "Not installed")
}

internal enum class AppSort(val label: String, val icon: ImageVector, val rotation: Float = 0f) {
    AZ("A–Z", Icons.Outlined.SortByAlpha),
    ZA("Z–A", Icons.Outlined.SortByAlpha, 180f),
    Sources("Sources", Icons.Outlined.Extension),
    Newest("Newest", Icons.Outlined.History),
    Updates("Updates", Icons.Outlined.Notifications),
    NewPatches("New patches", Icons.Outlined.Extension)
}

internal object MorpheFavourites {
    private const val PREFS = "morphe_favourites"
    private const val KEY = "packages"

    fun load(context: Context): Set<String> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY, emptySet())
            .orEmpty()

    fun toggle(context: Context, packageName: String): Set<String> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val current = prefs.getStringSet(KEY, emptySet()).orEmpty().toMutableSet()
        if (!current.add(packageName)) current.remove(packageName)
        prefs.edit().putStringSet(KEY, current).apply()
        return current
    }
}

@Composable
private fun FindAppsStatsStrip(index: MorpheArchiveIndex?) {
    val scheme = MaterialTheme.colorScheme
    val transition = rememberInfiniteTransition(label = "stats_shimmer")
    val shimmerAlpha by transition.animateFloat(
        initialValue = 0.16f,
        targetValue = 0.34f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "stats_shimmer_alpha"
    )

    val stats = listOf(
        Triple("SOURCES", index?.repoCount, Icons.Outlined.Dns),
        Triple("APPS", index?.appCount, Icons.Outlined.Apps),
        Triple("PATCHES", index?.patchCount, Icons.Outlined.Extension),
        Triple("UNIVERSAL", index?.universalPatchCount, Icons.Outlined.Public)
    )

    val outerRadius = 20.dp
    val innerRadius = 6.dp
    val statCount = stats.size
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        stats.forEachIndexed { i, (label, value, icon) ->
            val isFirst = i == 0
            val isLast = i == statCount - 1
            val cardShape = RoundedCornerShape(
                topStart = if (isFirst) outerRadius else innerRadius,
                bottomStart = if (isFirst) outerRadius else innerRadius,
                topEnd = if (isLast) outerRadius else innerRadius,
                bottomEnd = if (isLast) outerRadius else innerRadius
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(cardShape)
                    .background(scheme.surfaceColorAtElevation(2.dp))
                    .padding(vertical = 10.dp, horizontal = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (value == null) {
                    Box(
                        modifier = Modifier
                            .width(42.dp)
                            .height(16.dp)
                            .clip(CircleShape)
                            .background(scheme.onSurface.copy(alpha = shimmerAlpha))
                    )
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = scheme.primary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = value.toString(),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = scheme.onSurface,
                            maxLines = 1
                        )
                    }
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
internal fun AppDisclaimerBanner() {
    SurfaceCard(
        cornerRadius = MorpheDefaults.CompactCornerRadius,
        color = SemanticTone.Warning.container.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ThemedIcon(
                icon = Icons.Outlined.Warning,
                size = 18.dp,
                tint = SemanticTone.Warning.accent
            )
            Text(
                text = stringResource(R.string.patch_index_maintained_by_the_community) +
                        "Community bundles are maintained by their respective authors and are not " +
                        "individually verified. Morphe and the developer of this app are not " +
                        "responsible for third-party patches.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun PendingTrackedPackagesCard(
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
private fun TrackPackageField(
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

@Composable
private fun CompactFilterChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconRotation: Float = 0f
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.05f
    val targetContainer = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.35f else 0.6f)
    }
    val bgColor by animateColorAsState(
        targetValue = targetContainer,
        animationSpec = tween(200),
        label = "chip_container"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(200),
        label = "chip_content"
    )
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = tween(200),
        label = "chip_scale"
    )

    Surface(
        onClick = onClick,
        modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale },
        shape = CircleShape,
        color = bgColor,
        contentColor = contentColor,
        interactionSource = interactionSource
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .size(16.dp)
                    .rotate(iconRotation)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@Composable
internal fun AppBrowserScreen(
    trackedOnly: Boolean = false,
    hideStats: Boolean = false,
    hideCommunityNotice: Boolean = false,
    useGradientCards: Boolean = true,
    bottomContentPadding: Dp = 160.dp,
    onBack: (() -> Unit)? = null,
    onOpenApp: (ArchiveApp) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var apps by remember { mutableStateOf<List<ArchiveApp>?>(null) }
    var archiveIndex by remember { mutableStateOf<MorpheArchiveIndex?>(null) }
    var freshness by remember { mutableStateOf<ArchiveFreshness?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var trackPackageQuery by remember { mutableStateOf("") }
    var trackPackageNotice by remember { mutableStateOf<String?>(null) }
    var trackPackageNoticeIsError by remember { mutableStateOf(false) }
    var searchExpanded by remember { mutableStateOf(false) }
    var loadKey by remember { mutableIntStateOf(0) }
    var tab by remember(trackedOnly) {
        mutableStateOf(if (trackedOnly) AppListTab.Tracked else AppListTab.All)
    }
    var sort by remember { mutableStateOf(AppSort.AZ) }
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var favourites by remember { mutableStateOf<Set<String>>(emptySet()) }
    var tracked by remember { mutableStateOf<Set<String>>(emptySet()) }
    var trackingSnapshots by remember {
        mutableStateOf<Map<String, TrackedAppSnapshot>>(emptyMap())
    }
    var installedPackages by remember { mutableStateOf<Set<String>>(emptySet()) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    val listState = rememberLazyListState()
    val searchFocusRequester = remember { FocusRequester() }
    val density = LocalDensity.current
    var headerHeightDp by remember { mutableStateOf(0.dp) }
    val headerHaze = remember { HazeState() }

    val isScrolled by remember(listState) {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
        }
    }
    val scope = rememberCoroutineScope()

    fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun trackPackage() {
        val entered = trackPackageQuery.trim()
        if (entered.isBlank()) {
            trackPackageNotice = "Enter a package name."
            trackPackageNoticeIsError = true
            return
        }
        val packageKey = entered.lowercase(Locale.US)
        val app = apps?.firstOrNull {
            it.packageName.equals(entered, ignoreCase = true)
        }
        if (app == null) {
            if (packageKey in tracked) {
                trackPackageNotice = "$packageKey is already waiting for the index."
                trackPackageNoticeIsError = false
                tab = AppListTab.Tracked
                return
            }

            tracked = MorpheTracking.trackPackage(context, packageKey)
            trackingSnapshots = MorpheTracking.loadSnapshots(context, tracked)
            TrackingCheckScheduler.sync(context)
            requestNotificationPermission()
            tab = AppListTab.Tracked
            trackPackageQuery = ""
            trackPackageNotice = "Waiting for $packageKey to appear in the Morphe index."
            trackPackageNoticeIsError = false
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
            return
        }
        if (app.packageName in tracked) {
            trackPackageNotice = "${app.name} is already tracked."
            trackPackageNoticeIsError = false
            tab = AppListTab.Tracked
            return
        }

        tracked = MorpheTracking.toggle(context, app)
        trackingSnapshots = MorpheTracking.loadSnapshots(context, tracked)
        TrackingCheckScheduler.sync(context)
        requestNotificationPermission()
        tab = AppListTab.Tracked
        trackPackageQuery = ""
        trackPackageNotice = "Tracking ${app.name}."
        trackPackageNoticeIsError = false
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
    }

    fun removePendingPackage(packageName: String) {
        tracked = MorpheTracking.untrack(context, packageName)
        trackingSnapshots = MorpheTracking.loadSnapshots(context, tracked)
        TrackingCheckScheduler.sync(context)
        trackPackageNotice = "Stopped waiting for $packageName."
        trackPackageNoticeIsError = false
    }

    LaunchedEffect(Unit) {
        favourites = MorpheFavourites.load(context)
        tracked = MorpheTracking.load(context)
        trackingSnapshots = MorpheTracking.loadSnapshots(context, tracked)
        installedPackages = withContext(Dispatchers.IO) {
            context.packageManager.getInstalledApplications(0)
                .mapNotNull { it.packageName.takeIf(String::isNotBlank) }
                .toSet()
        }
    }

    DisposableEffect(context) {
        val observer = MorpheTracking.observe(context) {
            tracked = MorpheTracking.load(context)
            trackingSnapshots = MorpheTracking.loadSnapshots(context, tracked)
        }
        onDispose { observer.close() }
    }

    LaunchedEffect(loadKey) {
        val cached = withContext(Dispatchers.IO) { MorpheArchive.loadCachedIndex(context) }
        if (cached != null && apps == null) {
            apps = cached.apps.sortedBy { it.name.lowercase(Locale.US) }
            archiveIndex = cached
            freshness = archiveFreshness(cached.generatedAt)
        } else if (cached == null && apps == null) {
            archiveIndex = null
            error = null
        }
        try {
            val index = MorpheArchive.fetchIndex(context)
            MorpheArchive.invalidatePatchListCache()
            apps = index.apps.sortedBy { it.name.lowercase(Locale.US) }
            archiveIndex = index
            freshness = archiveFreshness(index.generatedAt)
            error = null
        } catch (e: Exception) {
            if (apps == null) {
                error = e.message ?: "Failed to load the app index"
            }
        }
    }

    LaunchedEffect(searchExpanded) {
        if (searchExpanded) {
            runCatching { searchFocusRequester.requestFocus() }
        }
    }

    BackHandler(enabled = searchExpanded) {
        query = ""
        searchExpanded = false
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
    }

    LaunchedEffect(query) {
        if (listState.firstVisibleItemIndex != 0 || listState.firstVisibleItemScrollOffset != 0) {
            listState.scrollToItem(0)
        }
    }

//===========================================================
// Adjust bottomStart / bottomEnd to change header roundness
//===========================================================
    val headerShape = RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp)

    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        val loaded = apps
        val changesByPackage = remember(loaded, trackingSnapshots) {
            loaded.orEmpty().associate { app ->
                app.packageName to app.trackedChanges(trackingSnapshots[app.packageName])
            }
        }
        val indexedPackages = remember(loaded) {
            loaded.orEmpty().mapNotNull { it.packageName.takeIf(String::isNotBlank) }.toSet()
        }
        val pendingTracked = remember(tracked, indexedPackages) {
            tracked - indexedPackages
        }
        var filtered by remember { mutableStateOf<List<ArchiveApp>>(emptyList()) }

        LaunchedEffect(
            loaded,
            query,
            tab,
            sort,
            favourites,
            installedPackages,
            tracked,
            trackedOnly,
            changesByPackage
        ) {
            val q = query.trim()
            if (q.isNotEmpty()) {
                delay(50)
            }
            val matches = withContext(Dispatchers.Default) {
                val qLower = q.lowercase(Locale.US)
                val matches = loaded.orEmpty()
                    .filter { app ->
                        (!trackedOnly || app.packageName in tracked) &&
                            (q.isBlank() || app.matchesQuery(qLower))
                    }
                    .filter { app ->
                        when (tab) {
                            AppListTab.All -> true
                            AppListTab.Favourites -> app.packageName in favourites
                            AppListTab.Tracked -> app.packageName in tracked
                            AppListTab.Installed -> app.packageName in installedPackages
                            AppListTab.NotInstalled -> app.packageName !in installedPackages
                        }
                    }
                    .let { list ->
                        if (q.isNotBlank()) {
                            list
                                .map { it to it.scoreMatch(qLower) }
                                .sortedWith(
                                    compareByDescending<Pair<ArchiveApp, Int>> { it.second }
                                        .thenByDescending { it.first.patchCount }
                                        .thenBy { it.first.name.lowercase(Locale.US) }
                                )
                                .map { it.first }
                        } else {
                            when (sort) {
                                AppSort.AZ -> list.sortedBy { it.name.lowercase(Locale.US) }
                                AppSort.ZA -> list.sortedByDescending { it.name.lowercase(Locale.US) }
                                AppSort.Sources -> list.sortedByDescending { it.sourceCount }
                                AppSort.Newest -> list
                                    .map { app -> app.newestReleaseDate().orEmpty() to app }
                                    .sortedByDescending { it.first }
                                    .map { it.second }
                                AppSort.Updates -> list.sortedWith(
                                    compareByDescending<ArchiveApp> {
                                        changesByPackage[it.packageName]?.updates ?: 0
                                    }.thenBy { it.name.lowercase(Locale.US) }
                                )
                                AppSort.NewPatches -> list.sortedWith(
                                    compareByDescending<ArchiveApp> {
                                        changesByPackage[it.packageName]?.newPatches ?: 0
                                    }.thenBy { it.name.lowercase(Locale.US) }
                                )
                            }
                        }
                    }
                matches
            }
            filtered = matches
        }
        val availableTabs = remember(trackedOnly) {
            if (trackedOnly) AppListTab.entries.filter { it != AppListTab.All } else AppListTab.entries
        }

        // CHILD 1: Full-screen LazyColumn
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = headerHaze),
            contentPadding = PaddingValues(
                top = headerHeightDp + 10.dp,
                start = MorpheDefaults.ContentPadding,
                end = MorpheDefaults.ContentPadding,
                bottom = bottomContentPadding
            )
        ) {
            // Always show Disclaimer, Filter, and Sort immediately
            item(key = "controls") {
                Column(
                    modifier = Modifier.padding(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!hideStats) {
                        FindAppsStatsStrip(archiveIndex)
                    }
                    if (!hideCommunityNotice) {
                        AppDisclaimerBanner()
                    }
                    if (trackedOnly) {
                        TrackPackageField(
                            value = trackPackageQuery,
                            onValueChange = {
                                trackPackageQuery = it
                                trackPackageNotice = null
                            },
                            enabled = loaded != null,
                            notice = trackPackageNotice,
                            noticeIsError = trackPackageNoticeIsError,
                            onTrack = { trackPackage() }
                        )
                        if (loaded != null && pendingTracked.isNotEmpty()) {
                            PendingTrackedPackagesCard(
                                packages = pendingTracked,
                                onRemove = { removePendingPackage(it) }
                            )
                        }
                    }

                    var filterMenuOpen by remember { mutableStateOf(false) }
                    var sortMenuOpen by remember { mutableStateOf(false) }
                    var filterWidthPx by remember { mutableIntStateOf(0) }
                    var sortWidthPx by remember { mutableIntStateOf(0) }

                    val menuShape = RoundedCornerShape(18.dp)
                    val menuContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
                    val menuBorder = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Filter Dropdown Button
                        Box(modifier = Modifier.weight(1f)) {
                            CompactFilterChip(
                                label = "Filter: ${tab.label}",
                                icon = tab.icon,
                                selected = tab != AppListTab.All,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .onSizeChanged { filterWidthPx = it.width },
                                onClick = { filterMenuOpen = true }
                            )
                            DropdownMenu(
                                expanded = filterMenuOpen,
                                onDismissRequest = { filterMenuOpen = false },
                                shape = menuShape,
                                containerColor = menuContainerColor,
                                border = menuBorder,
                                modifier = Modifier.width(with(density) { filterWidthPx.toDp() })
                            ) {
                                availableTabs.forEach { itemTab ->
                                    val isSelected = tab == itemTab
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = itemTab.label,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = itemTab.icon,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                        modifier = Modifier.height(38.dp),
                                        onClick = {
                                            tab = itemTab
                                            filterMenuOpen = false
                                        }
                                    )
                                }
                            }
                        }

                        // Sort Dropdown Button
                        Box(modifier = Modifier.weight(1f)) {
                            CompactFilterChip(
                                label = "Sort: ${sort.label}",
                                icon = sort.icon,
                                iconRotation = sort.rotation,
                                selected = sort != AppSort.AZ,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .onSizeChanged { sortWidthPx = it.width },
                                onClick = { sortMenuOpen = true }
                            )
                            DropdownMenu(
                                expanded = sortMenuOpen,
                                onDismissRequest = { sortMenuOpen = false },
                                shape = menuShape,
                                containerColor = menuContainerColor,
                                border = menuBorder,
                                modifier = Modifier.width(with(density) { sortWidthPx.toDp() })
                            ) {
                                AppSort.entries.forEach { itemSort ->
                                    val isSelected = sort == itemSort
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = itemSort.label,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = itemSort.icon,
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .rotate(itemSort.rotation),
                                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                        modifier = Modifier.height(38.dp),
                                        onClick = {
                                            sort = itemSort
                                            sortMenuOpen = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            when {
                error != null -> {
                    item(key = "error") {
                        Column(verticalArrangement = Arrangement.spacedBy(MorpheDefaults.ItemSpacing)) {
                            InfoCard(error!!)
                            HelperButton(
                                text = "Retry",
                                onClick = { loadKey++ },
                                icon = Icons.Outlined.Refresh,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                loaded == null -> {
                    val skeletonCount = 7
                    items(skeletonCount, key = { "skeleton_$it" }) { index ->
                        val isFirst = index == 0
                        val isLast = index == skeletonCount - 1
                        val outerRadius = 22.dp
                        val innerRadius = 10.dp
                        val cardShape = RoundedCornerShape(
                            topStart = if (isFirst) outerRadius else innerRadius,
                            topEnd = if (isFirst) outerRadius else innerRadius,
                            bottomStart = if (isLast) outerRadius else innerRadius,
                            bottomEnd = if (isLast) outerRadius else innerRadius
                        )
                        FindAppCardLoading(
                            index = index,
                            shape = cardShape,
                            useGradient = useGradientCards,
                            modifier = Modifier.padding(bottom = if (isLast) 0.dp else 3.dp)
                        )
                    }
                }

                filtered.isEmpty() -> {
                    item(key = "empty") {
                        InfoCard(
                            when {
                                trackedOnly && tracked.isEmpty() ->
                                    "No tracked apps yet. Enter a package name above or tap the bell on any app."
                                tab == AppListTab.Tracked && tracked.isEmpty() ->
                                    "No tracked apps yet. Tap the bell on any app to follow its updates and patches."
                                tab == AppListTab.Tracked &&
                                    pendingTracked.isNotEmpty() && query.isBlank() ->
                                    "Waiting for ${pendingTracked.size} tracked " +
                                        (if (pendingTracked.size == 1) "package" else "packages") +
                                        " to appear in the Morphe index."
                                tab == AppListTab.Tracked ->
                                    "No tracked apps match \"$query\"."
                                tab == AppListTab.Favourites ->
                                    "No liked apps yet. Tap the heart on any app to pin it here."
                                tab == AppListTab.Installed ->
                                    "No patched apps in the index are installed."
                                tab == AppListTab.NotInstalled ->
                                    "Every app in the index is installed on this device."
                                else -> "No apps match \"$query\"."
                            }
                        )
                    }
                }

                else -> {
                    itemsIndexed(
                        filtered,
                        key = { _, app -> app.packageName }
                    ) { index, app ->
                        val isFirst = index == 0
                        val isLast = index == filtered.lastIndex

//===================================================================================
// outerRadius = top of first card & bottom of last card
// innerRadius = all corners between middle cards (increase/decrease 12.dp to adjust)
//===================================================================================
                        val outerRadius = 22.dp
                        val innerRadius = 10.dp
                        val cardShape = RoundedCornerShape(
                            topStart = if (isFirst) outerRadius else innerRadius,
                            topEnd = if (isFirst) outerRadius else innerRadius,
                            bottomStart = if (isLast) outerRadius else innerRadius,
                            bottomEnd = if (isLast) outerRadius else innerRadius
                        )

                        val changes = changesByPackage[app.packageName] ?: TrackedAppChanges()
                        AppBrowserRow(
                            app = app,
                            installed = app.packageName in installedPackages,
                            favourite = app.packageName in favourites,
                            tracked = app.packageName in tracked,
                            updates = changes.updates,
                            newPatches = changes.newPatches,
                            shape = cardShape,
                            useGradient = useGradientCards,
                            onToggleFavourite = {
                                favourites = MorpheFavourites.toggle(context, app.packageName)
                            },
                            onToggleTracking = {
                                val adding = app.packageName !in tracked
                                tracked = MorpheTracking.toggle(context, app)
                                trackingSnapshots = MorpheTracking.loadSnapshots(context, tracked)
                                TrackingCheckScheduler.sync(context)
                                if (adding) requestNotificationPermission()
                            },
                            onClick = {
                                if (app.packageName in tracked) {
                                    MorpheTracking.markSeen(context, app)
                                    trackingSnapshots = MorpheTracking.loadSnapshots(context, tracked)
                                }
                                focusManager.clearFocus(force = true)
                                keyboardController?.hide()
                                onOpenApp(app)
                            },
                            modifier = Modifier
                                .animatedListItem(this)
                                .padding(bottom = if (isLast) 0.dp else 3.dp)
                        )
                    }
                }
            }
        }

        // CHILD 2: Scrollbar
        LazyListScrollbar(
            listState = listState,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .fillMaxHeight()
                .padding(top = headerHeightDp)
                .navigationBarsPadding()
        )

        // CHILD 3: Go-to-top FAB
        MorpheFab(
            visible = loaded != null && listState.firstVisibleItemIndex > 0,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 20.dp, bottom = 104.dp)
        ) {
            Surface(
                onClick = { scope.launch { listState.animateScrollToItem(0) } },
                shape = CircleShape,
                color = SemanticTone.Primary.container,
                contentColor = SemanticTone.Primary.content
            ) {
                ThemedIcon(
                    icon = Icons.Outlined.KeyboardArrowUp,
                    contentDescription = stringResource(R.string.go_to_top),
                    tint = SemanticTone.Primary.content,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // CHILD 4: Top-aligned Floating Header (MUST have .align(Alignment.TopCenter))
        val showHeaderSurface = isScrolled

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .onSizeChanged { size ->
                    headerHeightDp = with(density) { size.height.toDp() }
                }
                .then(
                    if (showHeaderSurface) {
                        Modifier.glassFill(
                            glass = MorpheColors.glassContainerColor(),
                            shape = headerShape,
                            hazeState = headerHaze
                        )
                    } else {
                        Modifier
                    }
                )
                .statusBarsPadding()
                .padding(
                    horizontal = MorpheDefaults.ContentPadding,
                    vertical = 12.dp
                )
        ) {
            AnimatedContent(
                targetState = searchExpanded,
                transitionSpec = {
                    val exitDuration = 100
                    val enterDuration = 200
                    (fadeIn(tween(enterDuration, delayMillis = exitDuration, easing = LinearOutSlowInEasing)) +
                            expandHorizontally(tween(enterDuration, delayMillis = exitDuration, easing = LinearOutSlowInEasing)))
                        .togetherWith(
                            fadeOut(tween(exitDuration, easing = FastOutLinearInEasing)) +
                                    shrinkHorizontally(tween(exitDuration, easing = FastOutLinearInEasing))
                        )
                },
                label = "header_search_transition",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) { isSearchActive ->
                if (isSearchActive) {
                    // 40.dp search bar vertically centered inside the 52.dp header row (zero height jump)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isDark = MaterialTheme.colorScheme.background.luminance() < 0.05f
                        val fieldContainer by animateColorAsState(
                            targetValue = MaterialTheme.colorScheme.surfaceVariant.copy(
                                alpha = if (isDark) 0.35f else 0.6f
                            ),
                            animationSpec = tween(200),
                            label = "search_field_container"
                        )
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            shape = CircleShape,
                            color = fieldContainer
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (query.isEmpty()) {
                                        Text(
                                            text = stringResource(R.string.search_apps_or_packages),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                alpha = 0.7f
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    BasicTextField(
                                        value = query,
                                        onValueChange = { query = it },
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                                            color = MaterialTheme.colorScheme.onSurface
                                        ),
                                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .focusRequester(searchFocusRequester)
                                    )
                                }
                                if (query.isNotEmpty()) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = "Clear search",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .clickable { query = "" }
                                    )
                                }
                            }
                        }

                        GlassHeaderButton(
                            icon = Icons.Outlined.SearchOff,
                            contentDescription = "Close search",
                            active = true,
                            onClick = {
                                query = ""
                                searchExpanded = false
                                focusManager.clearFocus(force = true)
                                keyboardController?.hide()
                            }
                        )
                    }
                } else {
                    // Normal header vertically centered inside the same 52.dp height
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (onBack != null) {
                            HelperHeaderIconButton(
                                icon = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = stringResource(R.string.back),
                                onClick = onBack
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (trackedOnly) "Tracking" else stringResource(R.string.find_new_apps),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    platformStyle = androidx.compose.ui.text.PlatformTextStyle(
                                        includeFontPadding = false
                                    )
                                ),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val fresh = freshness
                            Text(
                                text = when {
                                    fresh != null && trackedOnly ->
                                        "${tracked.size} tracked \u00b7 updated ${fresh.label}"
                                    fresh != null -> "Updated ${fresh.label}"
                                    apps == null -> "Loading…"
                                    trackedOnly && tracked.isEmpty() ->
                                        "Track updates and new patches"
                                    trackedOnly -> "${tracked.size} tracked"
                                    else -> "Community patch index"
                                },
                                color = if (fresh?.stale == true) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    platformStyle = androidx.compose.ui.text.PlatformTextStyle(
                                        includeFontPadding = false
                                    )
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        GlassHeaderButton(
                            icon = Icons.Outlined.Refresh,
                            contentDescription = stringResource(R.string.refresh),
                            onClick = { loadKey++ }
                        )

                        GlassHeaderButton(
                            icon = Icons.Outlined.Search,
                            contentDescription = stringResource(R.string.search_apps_or_packages),
                            onClick = { searchExpanded = true }
                        )
                    }
                }
            }
        }
    }
}

/**
 * A single circular icon button matching Morphe's HeaderControlDefaults:
 * 40 dp, animated glass fill, animated border on [active] state.
 */
@Composable
private fun GlassHeaderButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    active: Boolean = false
) {
    val isDark = isSystemInDarkTheme()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    // Container: idle = surfaceVariant glass; active = primaryContainer glass
    val idleAlpha = if (isDark) 0.35f else 0.6f
    val activeAlpha = if (isDark) 0.55f else 0.72f
    val baseContainer = if (active) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = activeAlpha)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = idleAlpha)
    }
    val containerColor by animateColorAsState(
        targetValue = if (pressed) baseContainer.copy(alpha = (baseContainer.alpha * 0.75f).coerceAtLeast(0.18f)) else baseContainer,
        animationSpec = tween(200),
        label = "glass_header_btn_container"
    )

    // Border: none when idle, primary-tinted when active
    val idleBorderAlpha = if (isDark) 0.4f else 0.6f
    val activeBorderAlpha = if (isDark) 0.55f else 0.45f
    val borderColor by animateColorAsState(
        targetValue = if (active) {
            MaterialTheme.colorScheme.primary.copy(alpha = activeBorderAlpha)
        } else {
            MaterialTheme.colorScheme.outline.copy(alpha = idleBorderAlpha)
        },
        animationSpec = tween(200),
        label = "glass_header_btn_border"
    )

    // Content
    val contentColor by animateColorAsState(
        targetValue = if (active) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(200),
        label = "glass_header_btn_content"
    )

    // Scale down on press like Morphe's rememberPressShape
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = tween(200),
        label = "glass_header_btn_scale"
    )

    Surface(
        onClick = onClick,
        modifier = Modifier
            .size(40.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale },
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor,
        interactionSource = interactionSource
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
internal fun AppBrowserRow(
    app: ArchiveApp,
    installed: Boolean,
    favourite: Boolean,
    shape: RoundedCornerShape,
    onToggleFavourite: () -> Unit,
    onClick: () -> Unit,
    tracked: Boolean = false,
    updates: Int = 0,
    newPatches: Int = 0,
    onToggleTracking: () -> Unit = {},
    modifier: Modifier = Modifier,
    useGradient: Boolean = true
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
            .pressScale(interactionSource)
            .clip(shape)
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
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
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
                    text = app.packageName,
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

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                        .clickable(role = Role.Button, onClick = onToggleTracking),
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

private val FindAppPlaceholderGradients = listOf(
    listOf(Color(0xFF1E5AA8), Color(0xFF00AFAE)),
    listOf(Color(0xFF6A3AB2), Color(0xFF3D5AFE)),
    listOf(Color(0xFFB3541E), Color(0xFFE0A030)),
    listOf(Color(0xFF2E7D32), Color(0xFF00AFAE)),
    listOf(Color(0xFF00796B), Color(0xFF1E5AA8))
)

@Composable
private fun FindAppCardLoading(
    index: Int,
    shape: RoundedCornerShape,
    modifier: Modifier = Modifier,
    useGradient: Boolean = true
) {
    val monochrome = LocalMonochromeTheme.current
    val effectiveGradient = useGradient && !monochrome
    val scheme = MaterialTheme.colorScheme
    val palette = FindAppPlaceholderGradients[index % FindAppPlaceholderGradients.size]
    val skeletonColor = if (!effectiveGradient) scheme.onSurface else Color.White

    val transition = rememberInfiniteTransition(label = "skeleton_shimmer_$index")
    val shimmerAlpha by transition.animateFloat(
        initialValue = 0.16f,
        targetValue = 0.34f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer_alpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(88.dp)
            .clip(shape)
            .background(
                if (!effectiveGradient) {
                    SolidColor(scheme.surfaceContainerHigh)
                } else {
                    Brush.linearGradient(
                        colors = palette.map { it.copy(alpha = 0.55f) },
                        start = Offset(0f, 0f),
                        end = Offset(1000f, 300f)
                    )
                }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(skeletonColor.copy(alpha = shimmerAlpha))
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .height(16.dp)
                        .clip(CircleShape)
                        .background(skeletonColor.copy(alpha = shimmerAlpha + 0.06f))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.80f)
                        .height(12.dp)
                        .clip(CircleShape)
                        .background(skeletonColor.copy(alpha = shimmerAlpha))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.42f)
                        .height(10.dp)
                        .clip(CircleShape)
                        .background(skeletonColor.copy(alpha = shimmerAlpha * 0.8f))
                )
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(skeletonColor.copy(alpha = 0.14f))
            )
        }
    }
}

private data class FindAppCardColors(
    val gradient: List<Color>,
    val title: Color,
    val subtitle: Color
)

private val FindAppFallbackColors = listOf(
    Color(0xFF1E5AA8),
    Color(0xFF00796B),
    Color(0xFF6A3AB2),
    Color(0xFFB3541E),
    Color(0xFFB3261E),
    Color(0xFF2E7D32)
)

private fun ArchiveApp.findAppCardColors(): FindAppCardColors {
    val base = iconColor.toAccentColorOrNull()
        ?: FindAppFallbackColors[(packageName.hashCode() and 0x7FFFFFFF) % FindAppFallbackColors.size]
    val luminance = base.luminance()
    val safe = if (luminance > 0.4f) {
        lerp(base, Color.Black, ((luminance - 0.4f) / luminance).coerceIn(0f, 0.65f))
    } else {
        base
    }
    return FindAppCardColors(
        gradient = listOf(
            lerp(safe, Color.Black, 0.42f),
            safe,
            lerp(safe, Color.White, 0.12f)
        ),
        title = Color.White,
        subtitle = Color.White.copy(alpha = 0.82f)
    )
}

private val installedAppIconCache = ConcurrentHashMap<String, ImageBitmap>()

@Composable
private fun rememberInstalledAppIcon(packageName: String): ImageBitmap? {
    val context = LocalContext.current
    var icon by remember(packageName) { mutableStateOf(installedAppIconCache[packageName]) }
    LaunchedEffect(packageName) {
        installedAppIconCache[packageName]?.let {
            icon = it
            return@LaunchedEffect
        }
        icon = withContext(Dispatchers.IO) {
            runCatching {
                context.packageManager.getApplicationIcon(packageName)
                    .toBitmap(width = 176, height = 176)
                    .asImageBitmap()
            }.getOrNull()
        }?.also { installedAppIconCache[packageName] = it }
    }
    return icon
}

@Composable
private fun FindAppIconPlaceholder(
    gradient: List<Color>,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val monochrome = LocalMonochromeTheme.current
    val base = if (monochrome) scheme.surfaceContainerHighest else gradient.getOrElse(0) { scheme.primary }
    val mid = if (monochrome) scheme.surfaceContainerHighest else gradient.getOrElse(1) { base }
    val end = if (monochrome) scheme.surfaceContainerHigh else gradient.lastOrNull() ?: base
    val highlight = if (monochrome) scheme.onSurface.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.50f)
    val borderHighlight = if (monochrome) scheme.outline.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.55f)

    Box(
        modifier = modifier.drawWithCache {
            val corner = CornerRadius(16.dp.toPx())
            val glass = Brush.linearGradient(
                colors = listOf(
                    highlight,
                    base.copy(alpha = 0.22f),
                    end.copy(alpha = 0.20f)
                ),
                start = Offset(0f, 0f),
                end = Offset(size.width, size.height)
            )
            val border = Brush.linearGradient(
                colors = listOf(
                    borderHighlight,
                    mid.copy(alpha = 0.30f),
                    borderHighlight
                ),
                start = Offset(0f, 0f),
                end = Offset(size.width, size.height)
            )
            onDrawBehind {
                drawRoundRect(brush = glass, cornerRadius = corner)
                drawRoundRect(
                    brush = border,
                    cornerRadius = corner,
                    style = Stroke(1.dp.toPx())
                )
            }
        }
    )
}

@Composable
private fun FindAppIcon(
    app: ArchiveApp,
    gradient: List<Color>,
    installed: Boolean,
    size: Dp = 56.dp,
    cornerRadius: Dp = 16.dp
) {
    val initial = app.name.firstOrNull()?.uppercaseChar() ?: '?'
    if (installed) {
        val icon = rememberInstalledAppIcon(app.packageName)
        if (icon != null) {
            Image(
                bitmap = icon,
                contentDescription = null,
                modifier = Modifier
                    .size(size)
                    .clip(RoundedCornerShape(cornerRadius))
            )
        } else {
            AsyncAvatar(
                url = null,
                fallbackText = initial.toString(),
                size = size,
                cornerRadius = cornerRadius
            )
        }
        return
    }
    val iconUrl = app.iconUrl?.takeIf { it.isNotBlank() }
    AsyncAvatar(
        url = iconUrl,
        fallbackText = initial.toString(),
        size = size,
        cornerRadius = cornerRadius,
        fallbackGradient = gradient.takeIf { iconUrl == null }
    )
}

/** Decoded archive icons, bounded so a long scroll cannot fill the heap. */
private const val ARCHIVE_ICON_CACHE_BYTES = 12 * 1024 * 1024
private const val ARCHIVE_ICON_MAX_PX = 256

private val archiveIconCache = object : LruCache<String, ImageBitmap>(ARCHIVE_ICON_CACHE_BYTES) {
    override fun sizeOf(key: String, value: ImageBitmap): Int =
        value.width * value.height * 4
}

/** Decode at avatar scale instead of retaining full-resolution source bitmaps. */
private fun decodeArchiveIcon(bytes: ByteArray): ImageBitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    val maxDimension = maxOf(bounds.outWidth, bounds.outHeight)
    var sampleSize = 1
    while (maxDimension / (sampleSize * 2) >= ARCHIVE_ICON_MAX_PX) {
        sampleSize *= 2
    }
    val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)?.asImageBitmap()
}

/** Icon fetch that is cancelled when the row leaves the viewport. */
private suspend fun fetchArchiveIcon(iconUrl: String): ImageBitmap? {
    val request = runCatching {
        Request.Builder()
            .url(iconUrl)
            .header(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0 Mobile Safari/537.36"
            )
            .build()
    }.getOrNull() ?: return null
    val call = MorpheArchive.http.newCall(request)
    return suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isActive) continuation.resume(null)
            }

            override fun onResponse(call: Call, response: Response) {
                val bitmap = runCatching {
                    response.use {
                        if (!it.isSuccessful) return@use null
                        it.body.bytes().let { bytes -> decodeArchiveIcon(bytes) }
                    }
                }.getOrNull()
                if (continuation.isActive) continuation.resume(bitmap)
            }
        })
    }
}

@Composable
internal fun AsyncAvatar(
    url: String?,
    fallbackText: String,
    size: Dp = 20.dp,
    cornerRadius: Dp = 6.dp,
    fallbackGradient: List<Color>? = null
) {
    var bitmap by remember(url) { mutableStateOf(url?.let(archiveIconCache::get)) }
    LaunchedEffect(url) {
        val iconUrl = url
        if (iconUrl == null) {
            bitmap = null
            return@LaunchedEffect
        }
        archiveIconCache.get(iconUrl)?.let {
            bitmap = it
            return@LaunchedEffect
        }
        delay(120)
        val loaded = fetchArchiveIcon(iconUrl) ?: return@LaunchedEffect
        archiveIconCache.put(iconUrl, loaded)
        bitmap = loaded
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap!!,
            contentDescription = null,
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(cornerRadius))
        )
    } else if (fallbackGradient != null) {
        FindAppIconPlaceholder(
            gradient = fallbackGradient,
            modifier = Modifier.size(size)
        )
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(cornerRadius))
                .background(SemanticTone.Primary.container),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = fallbackText,
                color = SemanticTone.Primary.content,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

internal fun openAddSource(context: Context, addUrl: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(addUrl))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    val manager = MORPHE_MANAGER_PACKAGES.firstOrNull { pkg ->
        runCatching {
            context.packageManager.queryIntentActivities(intent.setPackage(pkg), 0).isNotEmpty()
        }.getOrDefault(false)
    }
    if (manager != null) intent.setPackage(manager)
    runCatching { context.startActivity(intent) }
}

/**
 * Displays version pills in a single horizontally scrollable row by default.
 * Automatically detects when the pills overflow past the screen edge and shows
 * a right-aligned down-arrow button to expand/collapse all pills into a wrapped FlowRow.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExpandableVersionRow(
    versions: List<String>,
    chipShape: RoundedCornerShape,
    chipHorizontalPadding: Dp,
    chipVerticalPadding: Dp,
    chipSpacing: Dp,
    containerColorFor: @Composable (String) -> Color,
    contentColorFor: @Composable (String) -> Color,
    borderColorFor: ((String) -> Color)? = null,
    expanded: Boolean? = null,
    onExpandedChange: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var internalExpanded by remember(versions) { mutableStateOf(false) }
    val isExpanded = expanded ?: internalExpanded
    val setExpanded: (Boolean) -> Unit = onExpandedChange ?: { internalExpanded = it }

    val scrollState = rememberScrollState()
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val hasOverflow = if (versions.isEmpty()) {
            false
        } else {
            var requiredWidth = chipSpacing * (versions.size - 1).coerceAtLeast(0)
            versions.forEach { ver ->
                val textWidth = textMeasurer.measure(
                    text = ver,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    softWrap = false
                ).size.width
                requiredWidth += with(density) { textWidth.toDp() } + chipHorizontalPadding * 2
            }
            requiredWidth > maxWidth
        }
        val showToggle = isExpanded || hasOverflow

        if (isExpanded) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                FlowRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(chipSpacing),
                    verticalArrangement = Arrangement.spacedBy(chipSpacing)
                ) {
                    versions.forEach { ver ->
                        VersionPillChip(
                            text = ver,
                            shape = chipShape,
                            horizontalPadding = chipHorizontalPadding,
                            verticalPadding = chipVerticalPadding,
                            containerColor = containerColorFor(ver),
                            contentColor = contentColorFor(ver),
                            borderColor = borderColorFor?.invoke(ver),
                            wrap = true
                        )
                    }
                }
                VersionExpandToggleButton(
                    expanded = true,
                    onClick = { setExpanded(false) }
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(scrollState),
                    horizontalArrangement = Arrangement.spacedBy(chipSpacing),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    versions.forEach { ver ->
                        VersionPillChip(
                            text = ver,
                            shape = chipShape,
                            horizontalPadding = chipHorizontalPadding,
                            verticalPadding = chipVerticalPadding,
                            containerColor = containerColorFor(ver),
                            contentColor = contentColorFor(ver),
                            borderColor = borderColorFor?.invoke(ver)
                        )
                    }
                }
                if (showToggle) {
                    VersionExpandToggleButton(
                        expanded = false,
                        onClick = { setExpanded(true) }
                    )
                }
            }
        }
    }
}

@Composable
private fun VersionPillChip(
    text: String,
    shape: RoundedCornerShape,
    horizontalPadding: Dp,
    verticalPadding: Dp,
    containerColor: Color,
    contentColor: Color,
    borderColor: Color? = null,
    wrap: Boolean = false
) {
    Box(
        modifier = Modifier
            .clip(shape)
            .background(containerColor)
            .then(
                if (borderColor != null) {
                    Modifier.border(width = 1.dp, color = borderColor, shape = shape)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = horizontalPadding, vertical = verticalPadding)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = contentColor,
            maxLines = if (wrap) Int.MAX_VALUE else 1,
            overflow = if (wrap) TextOverflow.Clip else TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun VersionExpandToggleButton(
    expanded: Boolean,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(scheme.surfaceVariant.copy(alpha = 0.65f))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
            contentDescription = if (expanded) "Collapse versions" else "Show all versions",
            tint = scheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun AppVersionStrip(versions: List<String>) {
    val scheme = MaterialTheme.colorScheme
    ExpandableVersionRow(
        versions = versions,
        chipShape = RoundedCornerShape(10.dp),
        chipHorizontalPadding = 12.dp,
        chipVerticalPadding = 8.dp,
        chipSpacing = 8.dp,
        containerColorFor = { scheme.surfaceVariant.copy(alpha = 0.45f) },
        contentColorFor = { scheme.onSurface },
        borderColorFor = { scheme.outline.copy(alpha = 0.22f) }
    )
}

@Stable
private class DetailHeaderCollapse {
    var folded by mutableFloatStateOf(0f)
        private set

    private var range = 0
    private var drags = 0

    val connection: NestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (source == NestedScrollSource.UserInput) drags++
            return Offset.Zero
        }

        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            folded = (folded - consumed.y - available.y.coerceAtLeast(0f)).coerceIn(0f, range.toFloat())
            return Offset.Zero
        }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            if (folded > 0f && folded < range) {
                easeTo(if (folded < range / 2f) 0f else range.toFloat())
            }
            return Velocity.Zero
        }
    }

    private suspend fun easeTo(target: Float) {
        val drag = drags
        animate(folded, target, animationSpec = tween(220)) { value, _ ->
            if (drags == drag) folded = value
        }
    }

    suspend fun unfold() {
        if (folded > 0f) easeTo(0f)
    }

    fun measured(height: Int) {
        range = height
        if (folded > height) folded = height.toFloat()
    }
}

private fun Modifier.foldsWith(collapse: DetailHeaderCollapse): Modifier =
    clipToBounds().layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        collapse.measured(placeable.height)
        val height = (placeable.height - collapse.folded.roundToInt()).coerceAtLeast(0)
        layout(placeable.width, height) {
            placeable.place(0, height - placeable.height)
        }
    }

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AppDetailView(
    app: ArchiveApp,
    installed: Boolean = false,
    useGradientHeader: Boolean = true,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var activePatchGroup by remember(app.packageName) {
        mutableStateOf<dev.rushi.apkdownloadhelper.updater.ArchiveSourceGroup?>(null)
    }
    var lastPatchGroup by remember(app.packageName) {
        mutableStateOf<dev.rushi.apkdownloadhelper.updater.ArchiveSourceGroup?>(null)
    }
    if (activePatchGroup != null) {
        lastPatchGroup = activePatchGroup
    }

    BackHandler(enabled = activePatchGroup != null || onBack != null) {
        if (activePatchGroup != null) {
            activePatchGroup = null
        } else {
            onBack?.invoke()
        }
    }

    val context = LocalContext.current
    val density = LocalDensity.current
    val monochrome = LocalMonochromeTheme.current
    val scheme = MaterialTheme.colorScheme
    val detailListState = rememberLazyListState()
    val detailHeaderHaze = remember { HazeState() }
    val collapse = remember(app.packageName) { DetailHeaderCollapse() }
    val expandedMirrorRepos = remember(app.packageName) { androidx.compose.runtime.mutableStateMapOf<String, Boolean>() }
    val expandedVersionKeys = remember(app.packageName) { androidx.compose.runtime.mutableStateMapOf<String, Boolean>() }

    val isAtTop by remember(detailListState) {
        derivedStateOf {
            detailListState.firstVisibleItemIndex == 0 && detailListState.firstVisibleItemScrollOffset == 0
        }
    }
    LaunchedEffect(isAtTop) {
        if (isAtTop) collapse.unfold()
    }
    var unfoldedHeaderHeightDp by remember { mutableStateOf(0.dp) }
    var footerHeightDp by remember { mutableStateOf(0.dp) }

    // Morphe DialogHeaderEntrance clocks (icon spring, text slide, badges rise)
    var headerEntered by remember(app.packageName) { mutableStateOf(false) }
    LaunchedEffect(app.packageName) { headerEntered = true }
    val entranceTarget = if (headerEntered) 1f else 0f

    val iconEntrance by animateFloatAsState(
        targetValue = entranceTarget,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 320f),
        label = "morphe_header_icon_entrance"
    )
    val textEntrance by animateFloatAsState(
        targetValue = entranceTarget,
        animationSpec = tween(durationMillis = 260, delayMillis = 60, easing = EaseOutCubic),
        label = "morphe_header_text_entrance"
    )
    val badgesEntrance by animateFloatAsState(
        targetValue = entranceTarget,
        animationSpec = tween(durationMillis = 240, delayMillis = 160, easing = EaseOutBack),
        label = "morphe_header_badges_entrance"
    )

    val openUrl: (String) -> Unit = { url ->
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }
    val sourceGroups = remember(app.sources) { groupArchiveSources(app.sources) }
    val appPalette = remember(app.iconColor, app.packageName) { app.findAppCardColors() }
    val useAppAccent = useGradientHeader && !monochrome
    val sourceAccent = if (useAppAccent) appPalette.gradient[1] else scheme.primary
    val released = formatReleaseDate(app.newestReleaseDate())

    val headerBandShape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
    val isDetailScrolled by remember(detailListState) {
        derivedStateOf {
            detailListState.firstVisibleItemIndex > 0 || detailListState.firstVisibleItemScrollOffset > 0
        }
    }
    val tintAlpha = if (isDetailScrolled) 0.48f else 0.72f
    val bandTint = when {
        monochrome -> scheme.surfaceColorAtElevation(3.dp).copy(alpha = tintAlpha)
        !useGradientHeader -> lerp(scheme.surfaceColorAtElevation(2.dp), scheme.primary, 0.24f).copy(alpha = tintAlpha)
        else -> lerp(scheme.surfaceColorAtElevation(2.dp), sourceAccent, 0.22f).copy(alpha = tintAlpha)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.background)
            .nestedScroll(collapse.connection)
    ) {
        // 1. Base App Details List (scrolls under floating header & pinned Close footer)
        LazyColumn(
            state = detailListState,
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = detailHeaderHaze),
            contentPadding = PaddingValues(
                top = unfoldedHeaderHeightDp + 12.dp,
                start = MorpheDefaults.ContentPadding,
                end = MorpheDefaults.ContentPadding,
                bottom = footerHeightDp + 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(MorpheDefaults.ItemSpacing)
        ) {
            // Sources section
            if (sourceGroups.isEmpty()) {
                item(key = "no_sources") {
                    MorpheEmptyState(message = "No patch sources listed for this app.")
                }
            } else {
                items(
                    items = sourceGroups,
                    key = { group ->
                        group.primary.patches.map { it.name }.sorted().joinToString("\u0000")
                    }
                ) { group ->
                    AppSourceCard(
                        packageName = app.packageName,
                        source = group.primary,
                        accentColor = sourceAccent,
                        mirrors = group.mirrors,
                        appVersions = app.versions,
                        mirrorsExpanded = expandedMirrorRepos[group.primary.repo] == true,
                        onMirrorsExpandedChange = { expandedMirrorRepos[group.primary.repo] = it },
                        expandedVersionKeys = expandedVersionKeys,
                        onOpenUrl = openUrl,
                        onAddToMorphe = { openAddSource(context, it) },
                        onViewPatches = { activePatchGroup = group },
                        onViewMirrorPatches = { mirrorSource ->
                            activePatchGroup = dev.rushi.apkdownloadhelper.updater.ArchiveSourceGroup(
                                primary = mirrorSource,
                                mirrors = emptyList()
                            )
                        }
                    )
                }
            }

        }

        // 2. Morphe Official ListDialogHeader Band (pure icon + title + subtitle + 1-row scrollable badges)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .onSizeChanged { size ->
                    val measuredDp = with(density) { size.height.toDp() }
                    if (collapse.folded == 0f && measuredDp > 0.dp) {
                        unfoldedHeaderHeightDp = measuredDp
                    }
                }
                .clip(headerBandShape)
                .glassFill(
                    glass = MorpheColors.glassContainerColor(),
                    shape = headerBandShape,
                    hazeState = detailHeaderHaze
                )
                .background(bandTint)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(
                        horizontal = MorpheDefaults.ContentPadding,
                        vertical = 14.dp
                    )
            ) {
                // Pinned Top Row: 48.dp Rounded App Icon + Bold App Name + Package Name Subtitle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .graphicsLayer {
                                val s = lerp(0.6f, 1f, iconEntrance)
                                scaleX = s
                                scaleY = s
                                alpha = iconEntrance.coerceIn(0f, 1f)
                            }
                    ) {
                        FindAppIcon(
                            app = app,
                            gradient = appPalette.gradient,
                            installed = installed,
                            size = 48.dp,
                            cornerRadius = 14.dp
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = app.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = scheme.onSurface,
                            maxLines = 1,
                            modifier = Modifier
                                .graphicsLayer {
                                    translationX = lerp(40f, 0f, textEntrance)
                                    alpha = textEntrance.coerceIn(0f, 1f)
                                }
                                .basicMarquee()
                        )
                        val subtitleProgress = ((textEntrance - 0.15f) / 0.85f).coerceIn(0f, 1f)
                        Text(
                            text = app.packageName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant,
                            maxLines = 1,
                            modifier = Modifier
                                .graphicsLayer {
                                    translationX = lerp(40f, 0f, subtitleProgress)
                                    alpha = subtitleProgress
                                }
                                .basicMarquee()
                        )
                    }
                }

                // Single-row horizontally scrollable badges that collapse smoothly on scroll
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .foldsWith(collapse)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .horizontalScroll(rememberScrollState())
                            .graphicsLayer {
                                translationY = lerp(20f, 0f, badgesEntrance)
                                alpha = badgesEntrance.coerceIn(0f, 1f)
                            },
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val badgeFill = if (monochrome) {
                            scheme.surfaceVariant.copy(alpha = 0.65f)
                        } else {
                            lerp(scheme.surfaceVariant, sourceAccent, 0.25f).copy(alpha = 0.65f)
                        }
                        MorpheStatusBadge(
                            text = "${app.sourceCount} " +
                                    if (app.sourceCount == 1) "source" else "sources",
                            icon = Icons.Outlined.Dns,
                            containerColor = badgeFill,
                            contentColor = scheme.onSurface
                        )
                        if (app.versions.isNotEmpty()) {
                            MorpheStatusBadge(
                                text = "${app.versions.size} " +
                                        if (app.versions.size == 1) "version" else "versions",
                                icon = Icons.Outlined.History,
                                containerColor = badgeFill,
                                contentColor = scheme.onSurface
                            )
                        }
                        released?.let { date ->
                            MorpheStatusBadge(
                                text = date,
                                icon = Icons.Outlined.History,
                                containerColor = badgeFill,
                                contentColor = scheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Telegram-style bottom dim scrim (above scrolling list, behind Close pill & system nav bar)
        val detailNavHeight = with(density) {
            androidx.compose.foundation.layout.WindowInsets.navigationBars.getBottom(this).toDp()
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(detailNavHeight + 76.dp)
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.45f to scheme.background.copy(alpha = 0.55f),
                        0.80f to scheme.background.copy(alpha = 0.85f),
                        1.0f to scheme.background.copy(alpha = 0.96f)
                    )
                )
        )

        // 3. Pinned bottom actions: Play Store stays available while the list scrolls.
        val closeInteraction = remember { MutableInteractionSource() }
        val closeBorderColor = if (monochrome) {
            scheme.outlineVariant.copy(alpha = 0.5f)
        } else {
            sourceAccent.copy(alpha = 0.35f)
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .onSizeChanged { size ->
                    footerHeightDp = with(density) { size.height.toDp() }
                }
                .navigationBarsPadding()
                .padding(
                    horizontal = MorpheDefaults.ContentPadding,
                    vertical = 12.dp
                )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = {
                        context.openPlayStoreListing(
                            app.packageName,
                            playStoreUrl(app.packageName)
                        )
                    },
                    modifier = Modifier
                        .weight(if (onBack != null) 1.4f else 1f)
                        .heightIn(min = 48.dp),
                    shape = CircleShape,
                    color = scheme.surfaceVariant.copy(alpha = 0.65f),
                    contentColor = scheme.onSurface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_src_play),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.open_in_play_store),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (onBack != null) {
                    androidx.compose.material3.OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .pressScale(closeInteraction)
                            .glassFill(
                                glass = Color.Transparent,
                                shape = CircleShape,
                                hazeState = detailHeaderHaze
                            ),
                        interactionSource = closeInteraction,
                        shape = CircleShape,
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = scheme.onSurface.copy(alpha = 0.85f)
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.close),
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // 4. Smooth bottom-to-top animated Patches overlay
        AnimatedVisibility(
            visible = activePatchGroup != null,
            enter = MorpheAnimations.pushEnter,
            exit = MorpheAnimations.pushExit
        ) {
            val group = lastPatchGroup
            if (group != null) {
                MorphePushedScreen {
                    SourcePatchesScreen(
                        packageName = app.packageName,
                        source = group.primary,
                        mirrors = group.mirrors,
                        accentColor = sourceAccent,
                        onOpenUrl = openUrl,
                        onAddToMorphe = { openAddSource(context, it) },
                        onBack = { activePatchGroup = null }
                    )
                }
            }
        }
    }
}

@Composable
internal fun AppSourceCard(
    packageName: String,
    source: ArchiveSource,
    accentColor: Color,
    onOpenUrl: (String) -> Unit,
    onAddToMorphe: (String) -> Unit,
    onViewPatches: () -> Unit,
    onViewMirrorPatches: (ArchiveSource) -> Unit,
    mirrors: List<ArchiveSource> = emptyList(),
    appVersions: List<String> = emptyList(),
    mirrorsExpanded: Boolean = false,
    onMirrorsExpandedChange: (Boolean) -> Unit = {},
    expandedVersionKeys: MutableMap<String, Boolean> = remember { androidx.compose.runtime.mutableStateMapOf() }
) {
    var liveVersions by remember(source.repo, packageName) {
        mutableStateOf<Map<String, List<String>>?>(null)
    }
    LaunchedEffect(source.repo, packageName) {
        liveVersions = MorpheArchive.fetchPatchVersions(source, packageName)
    }

    val colors = MaterialTheme.colorScheme
    val surface = colors.surfaceColorAtElevation(2.dp)
    val owner = remember(source.repo) { source.repo.substringBefore('/', "") }
    val repoName = remember(source.repo) { source.repo.substringAfter('/', source.repo) }
    val bundleVersion = remember(source.latestChanges) { bundleVersionOf(source.latestChanges) }
    val updatedDate = remember(source.latestChanges) {
        formatReleaseDate(releaseDateOf(source.latestChanges))
    }

    val supportedAppVersions = remember(source.patches, liveVersions) {
        val fromLive = liveVersions?.values?.flatten().orEmpty()
        val fromIndex = source.patches.flatMap { it.versions }
        (fromLive + fromIndex)
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.equals("null", ignoreCase = true) }
            .distinct()
    }

    val outerPillRadius = 20.dp
    val innerPillRadius = 6.dp
    fun segmentedShape(index: Int, count: Int) = RoundedCornerShape(
        topStart = if (index == 0) outerPillRadius else innerPillRadius,
        bottomStart = if (index == 0) outerPillRadius else innerPillRadius,
        topEnd = if (index == count - 1) outerPillRadius else innerPillRadius,
        bottomEnd = if (index == count - 1) outerPillRadius else innerPillRadius
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Owner / Repo Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(accentColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncAvatar(
                        url = MorpheArchive.avatarUrlFor(source),
                        fallbackText = source.repo.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        size = 38.dp,
                        cornerRadius = 11.dp
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (owner.isNotEmpty()) {
                        Text(
                            text = owner,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = repoName,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 2. Soft-Tinted Segmented Metadata Pills (Dead-centered icons & text)
            val metaItems = buildList {
                add(
                    Icons.Outlined.Extension to (
                            "${source.patches.size} " + if (source.patches.size == 1) "patch" else "patches"
                            )
                )
                bundleVersion?.let { ver ->
                    add(Icons.Outlined.Dns to ver)
                }
                updatedDate?.let { date ->
                    add(Icons.Outlined.History to date)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                metaItems.forEachIndexed { idx, (icon, text) ->
                    SourceMetaItem(
                        icon = icon,
                        text = text,
                        shape = segmentedShape(idx, metaItems.size),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 3. Supported App Versions from patches-list.json (falls back to "Any version")
            val versionsToDisplay = when {
                liveVersions == null && supportedAppVersions.isEmpty() -> listOf("Loading…")
                supportedAppVersions.isEmpty() -> listOf("Any version")
                else -> supportedAppVersions
            }
            ExpandableVersionRow(
                versions = versionsToDisplay,
                chipShape = CircleShape,
                chipHorizontalPadding = 10.dp,
                chipVerticalPadding = 4.dp,
                chipSpacing = 6.dp,
                expanded = expandedVersionKeys[source.repo] == true,
                onExpandedChange = { expandedVersionKeys[source.repo] = it },
                containerColorFor = { ver ->
                    if (ver == "Any version" || ver == "Loading…") {
                        colors.surfaceVariant.copy(alpha = 0.45f)
                    } else {
                        colors.primaryContainer
                    }
                },
                contentColorFor = { ver ->
                    if (ver == "Any version" || ver == "Loading…") {
                        colors.onSurfaceVariant
                    } else {
                        colors.onPrimaryContainer
                    }
                }
            )

            // 4. Segmented Action Pills: Repo | Patches | Add (Dead-centered icons & text)
            data class SourceActionSpec(
                val label: String,
                val icon: ImageVector,
                val contentDescription: String,
                val isPrimary: Boolean,
                val onClick: () -> Unit
            )
            val repoDesc = stringResource(R.string.open_repo)
            val addDesc = stringResource(R.string.add_to_morphe)
            val actionSpecs = buildList {
                source.webUrl?.takeIf { it.isNotBlank() }?.let { webUrl ->
                    add(SourceActionSpec("Repo", Icons.Outlined.OpenInNew, repoDesc, false) { onOpenUrl(webUrl) })
                }
                add(SourceActionSpec("Patches", Icons.Outlined.Extension, "Patches", false, onViewPatches))
                source.addUrl?.takeIf { it.isNotBlank() }?.let { addUrl ->
                    add(SourceActionSpec("Add", Icons.Outlined.Add, addDesc, true) { onAddToMorphe(addUrl) })
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                actionSpecs.forEachIndexed { idx, spec ->
                    val pillShape = segmentedShape(idx, actionSpecs.size)
                    val containerColor = if (spec.isPrimary) {
                        SemanticTone.Primary.container
                    } else {
                        colors.surfaceVariant.copy(alpha = 0.55f)
                    }
                    val contentColor = if (spec.isPrimary) {
                        SemanticTone.Primary.content
                    } else {
                        colors.onSurface
                    }
                    Surface(
                        onClick = spec.onClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        shape = pillShape,
                        color = containerColor,
                        contentColor = contentColor
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = spec.icon,
                                    contentDescription = spec.contentDescription,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = spec.label,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        platformStyle = androidx.compose.ui.text.PlatformTextStyle(
                                            includeFontPadding = false
                                        )
                                    ),
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // 5. Redesigned Compact Mirror Bar ("Same patches in N other repos")
            if (mirrors.isNotEmpty()) {
                val chevronRotation by animateFloatAsState(
                    targetValue = if (mirrorsExpanded) 180f else 0f,
                    animationSpec = tween(200),
                    label = "mirror_chevron_rotation"
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        onClick = { onMirrorsExpandedChange(!mirrorsExpanded) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = colors.surfaceVariant.copy(alpha = 0.26f),
                        contentColor = colors.onSurfaceVariant
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Dns,
                                contentDescription = null,
                                tint = colors.onSurfaceVariant.copy(alpha = 0.8f),
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Same patches from other " +
                                        (if (mirrors.size == 1) "repo" else "repos"),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    platformStyle = androidx.compose.ui.text.PlatformTextStyle(
                                        includeFontPadding = false
                                    )
                                ),
                                fontWeight = FontWeight.Medium,
                                color = colors.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(colors.surfaceVariant.copy(alpha = 0.55f))
                                    .padding(horizontal = 7.dp, vertical = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "+${mirrors.size}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        platformStyle = androidx.compose.ui.text.PlatformTextStyle(
                                            includeFontPadding = false
                                        )
                                    ),
                                    fontWeight = FontWeight.Bold,
                                    color = colors.onSurface
                                )
                            }
                            Icon(
                                imageVector = Icons.Outlined.ExpandMore,
                                contentDescription = if (mirrorsExpanded) "Collapse mirrors" else "Expand mirrors",
                                tint = colors.onSurfaceVariant,
                                modifier = Modifier
                                    .size(18.dp)
                                    .rotate(chevronRotation)
                            )
                        }
                    }

                    AnimatedExpand(visible = mirrorsExpanded) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            mirrors.forEach { mirror ->
                                val mirrorVersion = bundleVersionOf(mirror.latestChanges)
                                val mirrorDate = formatReleaseDate(releaseDateOf(mirror.latestChanges))
                                val mirrorMeta = listOfNotNull(
                                    "${mirror.patches.size} " + if (mirror.patches.size == 1) "patch" else "patches",
                                    mirrorVersion,
                                    mirrorDate
                                ).joinToString(" · ")

                                val mirrorIndexVersions = remember(mirror.patches) {
                                    mirror.patches.flatMap { it.versions }
                                        .map { it.trim() }
                                        .filter { it.isNotEmpty() && !it.equals("null", ignoreCase = true) }
                                        .distinct()
                                }
                                val mirrorVersionsToDisplay = when {
                                    mirrorIndexVersions.isNotEmpty() -> mirrorIndexVersions
                                    liveVersions == null && supportedAppVersions.isEmpty() -> listOf("Loading…")
                                    supportedAppVersions.isEmpty() -> listOf("Any version")
                                    else -> supportedAppVersions
                                }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(colors.surfaceVariant.copy(alpha = 0.22f))
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AsyncAvatar(
                                            url = MorpheArchive.avatarUrlFor(mirror),
                                            fallbackText = mirror.repo
                                                .firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                            size = 28.dp,
                                            cornerRadius = 8.dp
                                        )
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(
                                                text = mirror.repo,
                                                color = colors.onSurface,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = mirrorMeta,
                                                color = colors.onSurfaceVariant.copy(alpha = 0.78f),
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    ExpandableVersionRow(
                                        versions = mirrorVersionsToDisplay,
                                        chipShape = CircleShape,
                                        chipHorizontalPadding = 8.dp,
                                        chipVerticalPadding = 3.dp,
                                        chipSpacing = 5.dp,
                                        expanded = expandedVersionKeys["${source.repo}->${mirror.repo}"] == true,
                                        onExpandedChange = { expandedVersionKeys["${source.repo}->${mirror.repo}"] = it },
                                        containerColorFor = { ver ->
                                            if (ver == "Any version" || ver == "Loading…") {
                                                colors.surfaceVariant.copy(alpha = 0.45f)
                                            } else {
                                                colors.primaryContainer
                                            }
                                        },
                                        contentColorFor = { ver ->
                                            if (ver == "Any version" || ver == "Loading…") {
                                                colors.onSurfaceVariant
                                            } else {
                                                colors.onPrimaryContainer
                                            }
                                        }
                                    )

                                    val mirrorActions = buildList {
                                        mirror.webUrl?.takeIf { it.isNotBlank() }?.let { webUrl ->
                                            add(SourceActionSpec("Repo", Icons.Outlined.OpenInNew, "Open ${mirror.repo}", false) { onOpenUrl(webUrl) })
                                        }
                                        add(SourceActionSpec("Patches", Icons.Outlined.Extension, "View ${mirror.repo} patches", false) { onViewMirrorPatches(mirror) })
                                        mirror.addUrl?.takeIf { it.isNotBlank() }?.let { addUrl ->
                                            add(SourceActionSpec("Add", Icons.Outlined.Add, "Add ${mirror.repo} to Morphe", true) { onAddToMorphe(addUrl) })
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        mirrorActions.forEachIndexed { idx, spec ->
                                            val pillShape = segmentedShape(idx, mirrorActions.size)
                                            val containerColor = if (spec.isPrimary) {
                                                SemanticTone.Primary.container
                                            } else {
                                                colors.surfaceVariant.copy(alpha = 0.55f)
                                            }
                                            val contentColor = if (spec.isPrimary) {
                                                SemanticTone.Primary.content
                                            } else {
                                                colors.onSurface
                                            }
                                            Surface(
                                                onClick = spec.onClick,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(34.dp),
                                                shape = pillShape,
                                                color = containerColor,
                                                contentColor = contentColor
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .padding(horizontal = 6.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(
                                                            imageVector = spec.icon,
                                                            contentDescription = spec.contentDescription,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                        Text(
                                                            text = spec.label,
                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                platformStyle = androidx.compose.ui.text.PlatformTextStyle(
                                                                    includeFontPadding = false
                                                                )
                                                            ),
                                                            fontWeight = FontWeight.SemiBold,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceMetaItem(
    icon: ImageVector,
    text: String,
    shape: RoundedCornerShape,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .height(34.dp)
            .clip(shape)
            .background(colors.surfaceVariant.copy(alpha = 0.24f))
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.onSurfaceVariant.copy(alpha = 0.85f),
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(
                    platformStyle = androidx.compose.ui.text.PlatformTextStyle(
                        includeFontPadding = false
                    )
                ),
                fontWeight = FontWeight.Medium,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SourcePatchesScreen(
    packageName: String,
    source: ArchiveSource,
    mirrors: List<ArchiveSource>,
    accentColor: Color,
    onOpenUrl: (String) -> Unit,
    onAddToMorphe: (String) -> Unit,
    onBack: () -> Unit
) {
    var liveVersions by remember(source.repo, packageName) {
        mutableStateOf<Map<String, List<String>>?>(null)
    }
    LaunchedEffect(source.repo, packageName) {
        liveVersions = MorpheArchive.fetchPatchVersions(source, packageName)
    }
    var patchQuery by remember { mutableStateOf("") }
    var searchExpanded by remember { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()
    val headerHaze = remember { HazeState() }
    val density = LocalDensity.current
    var headerHeightDp by remember { mutableStateOf(0.dp) }
    var footerHeightDp by remember { mutableStateOf(0.dp) }
    val colors = MaterialTheme.colorScheme
    val monochrome = LocalMonochromeTheme.current

    val expandedPatchVersionKeys = remember(source.repo, packageName) {
        androidx.compose.runtime.mutableStateMapOf<String, Boolean>()
    }

    val isScrolled by remember(listState) {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
        }
    }

    LaunchedEffect(searchExpanded) {
        if (searchExpanded) {
            runCatching { searchFocusRequester.requestFocus() }
        }
    }

    BackHandler(enabled = searchExpanded) {
        patchQuery = ""
        searchExpanded = false
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
    }

    val filteredPatches = remember(source.patches, patchQuery, liveVersions) {
        val indexed = source.patches.mapIndexed { idx, patch -> (idx + 1) to patch }
        if (patchQuery.isBlank()) {
            indexed
        } else {
            indexed.filter { (_, patch) ->
                val resolvedVersions = liveVersions
                    ?.get(patch.name.trim())
                    ?.takeIf { it.isNotEmpty() }
                    ?: patch.versions
                patch.name.contains(patchQuery, ignoreCase = true) ||
                        patch.description?.contains(patchQuery, ignoreCase = true) == true ||
                        resolvedVersions.any { it.contains(patchQuery, ignoreCase = true) }
            }
        }
    }

    val headerShape = RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = headerHaze),
            contentPadding = PaddingValues(
                top = headerHeightDp + 10.dp,
                start = MorpheDefaults.ContentPadding,
                end = MorpheDefaults.ContentPadding,
                bottom = footerHeightDp + 24.dp
            )
        ) {
            if (filteredPatches.isEmpty()) {
                item(key = "no_patches") {
                    InfoCard(
                        if (patchQuery.isBlank()) {
                            stringResource(R.string.no_patches_listed_for_this_source)
                        } else {
                            "No patches match \"$patchQuery\"."
                        }
                    )
                }
            } else {
                itemsIndexed(
                    filteredPatches,
                    key = { _, (originalNumber, patch) -> "${patch.name}_$originalNumber" }
                ) { index, (originalNumber, patch) ->
                    val isFirst = index == 0
                    val isLast = index == filteredPatches.lastIndex

                    val outerRadius = 22.dp
                    val innerRadius = 10.dp
                    val cardSpacing = 3.dp

                    val cardShape = RoundedCornerShape(
                        topStart = if (isFirst) outerRadius else innerRadius,
                        topEnd = if (isFirst) outerRadius else innerRadius,
                        bottomStart = if (isLast) outerRadius else innerRadius,
                        bottomEnd = if (isLast) outerRadius else innerRadius
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = if (isLast) 0.dp else cardSpacing)
                            .clip(cardShape)
                            .background(colors.surfaceColorAtElevation(2.dp))
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.surfaceVariant.copy(alpha = 0.75f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = originalNumber.toString(),
                                color = colors.onSurface,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = patch.name,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.onSurface,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            patch.description?.takeIf { it.isNotBlank() }?.let { desc ->
                                Text(
                                    text = desc,
                                    color = colors.onSurfaceVariant.copy(alpha = 0.9f),
                                    style = MaterialTheme.typography.bodySmall,
                                    lineHeight = 17.sp
                                )
                            }

                            val resolvedVersions = liveVersions?.get(patch.name.trim())
                                ?.takeIf { it.isNotEmpty() }
                                ?: patch.versions.takeIf { it.isNotEmpty() }

                            val versionsToShow = when {
                                liveVersions == null && resolvedVersions.isNullOrEmpty() -> listOf("Loading…")
                                resolvedVersions.isNullOrEmpty() -> listOf("Any version")
                                else -> resolvedVersions.orEmpty()
                            }

                            val patchKey = "${patch.name}_$originalNumber"
                            ExpandableVersionRow(
                                versions = versionsToShow,
                                chipShape = CircleShape,
                                chipHorizontalPadding = 10.dp,
                                chipVerticalPadding = 4.dp,
                                chipSpacing = 6.dp,
                                expanded = expandedPatchVersionKeys[patchKey] == true,
                                onExpandedChange = { expandedPatchVersionKeys[patchKey] = it },
                                containerColorFor = { ver ->
                                    if (ver == "Any version") {
                                        colors.surfaceVariant.copy(alpha = 0.45f)
                                    } else {
                                        colors.primaryContainer
                                    }
                                },
                                contentColorFor = { ver ->
                                    if (ver == "Any version") {
                                        colors.onSurfaceVariant
                                    } else {
                                        colors.onPrimaryContainer
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Top Header: Source Name + Patch Count on Left, Search Button on Right
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .onSizeChanged { size ->
                    headerHeightDp = with(density) { size.height.toDp() }
                }
                .then(
                    if (isScrolled) {
                        Modifier.glassFill(
                            glass = MorpheColors.glassContainerColor(),
                            shape = headerShape,
                            hazeState = headerHaze
                        )
                    } else {
                        Modifier
                    }
                )
                .statusBarsPadding()
                .padding(
                    horizontal = MorpheDefaults.ContentPadding,
                    vertical = 12.dp
                )
        ) {
            AnimatedContent(
                targetState = searchExpanded,
                transitionSpec = {
                    val exitDuration = 100
                    val enterDuration = 200
                    (fadeIn(tween(enterDuration, delayMillis = exitDuration, easing = LinearOutSlowInEasing)) +
                            expandHorizontally(tween(enterDuration, delayMillis = exitDuration, easing = LinearOutSlowInEasing)))
                        .togetherWith(
                            fadeOut(tween(exitDuration, easing = FastOutLinearInEasing)) +
                                    shrinkHorizontally(tween(exitDuration, easing = FastOutLinearInEasing))
                        )
                },
                label = "patches_search_transition",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) { isSearchActive ->
                if (isSearchActive) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isDark = colors.background.luminance() < 0.05f
                        val fieldContainer by animateColorAsState(
                            targetValue = colors.surfaceVariant.copy(
                                alpha = if (isDark) 0.35f else 0.6f
                            ),
                            animationSpec = tween(200),
                            label = "patch_search_field_container"
                        )
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            shape = CircleShape,
                            color = fieldContainer
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Search,
                                    contentDescription = null,
                                    tint = colors.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (patchQuery.isEmpty()) {
                                        Text(
                                            text = "Search patches or versions",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = colors.onSurfaceVariant.copy(alpha = 0.7f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    BasicTextField(
                                        value = patchQuery,
                                        onValueChange = { patchQuery = it },
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                                            color = colors.onSurface
                                        ),
                                        cursorBrush = SolidColor(colors.primary),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .focusRequester(searchFocusRequester)
                                    )
                                }
                                if (patchQuery.isNotEmpty()) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = "Clear search",
                                        tint = colors.onSurfaceVariant,
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .clickable { patchQuery = "" }
                                    )
                                }
                            }
                        }

                        GlassHeaderButton(
                            icon = Icons.Outlined.SearchOff,
                            contentDescription = "Close search",
                            active = true,
                            onClick = {
                                patchQuery = ""
                                searchExpanded = false
                                focusManager.clearFocus(force = true)
                                keyboardController?.hide()
                            }
                        )
                    }
                } else {
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
                            Text(
                                text = source.repo,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.onBackground,
                                maxLines = 1,
                                modifier = Modifier.basicMarquee()
                            )
                            Text(
                                text = "${source.patches.size} " +
                                        if (source.patches.size == 1) "patch" else "patches",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        GlassHeaderButton(
                            icon = Icons.Outlined.Search,
                            contentDescription = "Search patches",
                            onClick = { searchExpanded = true }
                        )
                    }
                }
            }
        }

        // Telegram-style bottom dim scrim (above scrolling list, behind Close pill & system nav bar)
        val patchesNavHeight = with(density) {
            androidx.compose.foundation.layout.WindowInsets.navigationBars.getBottom(this).toDp()
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(patchesNavHeight + 76.dp)
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.45f to colors.background.copy(alpha = 0.55f),
                        0.80f to colors.background.copy(alpha = 0.85f),
                        1.0f to colors.background.copy(alpha = 0.96f)
                    )
                )
        )

        // Pinned Bottom "Close" Frosted Outlined Pill (matching AppDetailView)
        val closeInteraction = remember { MutableInteractionSource() }
        val closeBorderColor = if (monochrome) {
            colors.outlineVariant.copy(alpha = 0.5f)
        } else {
            accentColor.copy(alpha = 0.35f)
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .onSizeChanged { size ->
                    footerHeightDp = with(density) { size.height.toDp() }
                }
                .navigationBarsPadding()
                .padding(
                    horizontal = MorpheDefaults.ContentPadding,
                    vertical = 12.dp
                )
        ) {
            androidx.compose.material3.OutlinedButton(
                onClick = {
                    focusManager.clearFocus(force = true)
                    keyboardController?.hide()
                    onBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .pressScale(closeInteraction)
                    .glassFill(
                        glass = Color.Transparent,
                        shape = CircleShape,
                        hazeState = headerHaze
                    ),
                interactionSource = closeInteraction,
                shape = CircleShape,
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = colors.onSurface.copy(alpha = 0.85f)
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Text(
                    text = stringResource(R.string.close),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}