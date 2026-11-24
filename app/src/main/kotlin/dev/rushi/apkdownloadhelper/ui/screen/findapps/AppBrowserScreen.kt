package dev.rushi.apkdownloadhelper.ui.screen.findapps

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import dev.chrisbanes.haze.HazeState
import dev.rushi.apkdownloadhelper.HelperButton
import dev.rushi.apkdownloadhelper.R
import dev.rushi.apkdownloadhelper.tracking.TrackingCheckScheduler
import dev.rushi.apkdownloadhelper.ui.shared.appcard.AppListCard
import dev.rushi.apkdownloadhelper.ui.shared.appcard.FindAppCardLoading
import dev.rushi.apkdownloadhelper.ui.shared.appcard.FindAppsStatsStrip
import dev.rushi.apkdownloadhelper.ui.shared.banner.AppDisclaimerBanner
import dev.rushi.apkdownloadhelper.ui.shared.button.AnimatedFab
import dev.rushi.apkdownloadhelper.ui.shared.button.GlassHeaderButton
import dev.rushi.apkdownloadhelper.ui.shared.button.HeaderIconButton
import dev.rushi.apkdownloadhelper.ui.shared.card.InfoCard
import dev.rushi.apkdownloadhelper.ui.shared.chip.CompactFilterChip
import dev.rushi.apkdownloadhelper.ui.shared.icon.ThemedIcon
import dev.rushi.apkdownloadhelper.ui.shared.input.SearchField
import dev.rushi.apkdownloadhelper.ui.shared.scroll.LazyListScrollbar
import dev.rushi.apkdownloadhelper.ui.shared.tracking.PendingTrackedPackagesCard
import dev.rushi.apkdownloadhelper.ui.shared.tracking.TrackPackageField
import dev.rushi.apkdownloadhelper.ui.model.FavouriteApps
import dev.rushi.apkdownloadhelper.ui.model.TrackedAppChanges
import dev.rushi.apkdownloadhelper.ui.model.TrackedAppSnapshot
import dev.rushi.apkdownloadhelper.ui.model.TrackingStore
import dev.rushi.apkdownloadhelper.ui.model.trackedChanges
import dev.rushi.apkdownloadhelper.ui.model.trackingPackageKey
import dev.rushi.apkdownloadhelper.ui.screen.home.LocalPagerPageSelected
import dev.rushi.apkdownloadhelper.ui.theme.AppColors
import dev.rushi.apkdownloadhelper.ui.theme.AppDefaults
import dev.rushi.apkdownloadhelper.ui.theme.SemanticTone
import dev.rushi.apkdownloadhelper.ui.theme.hazeSourceIfEnabled
import dev.rushi.apkdownloadhelper.ui.theme.animatedListItem
import dev.rushi.apkdownloadhelper.ui.theme.glassFill
import dev.rushi.apkdownloadhelper.updater.ArchiveApp
import dev.rushi.apkdownloadhelper.updater.ArchiveFreshness
import dev.rushi.apkdownloadhelper.updater.MorpheArchive
import dev.rushi.apkdownloadhelper.updater.MorpheArchiveIndex
import dev.rushi.apkdownloadhelper.updater.archiveFreshness
import dev.rushi.apkdownloadhelper.updater.newestReleaseDate
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    val initialIndex = MorpheArchive.inMemoryIndex
    var apps by remember {
        mutableStateOf<List<ArchiveApp>?>(
            initialIndex?.apps?.sortedBy { it.name.lowercase(Locale.US) }
        )
    }
    var archiveIndex by remember { mutableStateOf(initialIndex) }
    var freshness by remember { mutableStateOf(initialIndex?.let { archiveFreshness(it.generatedAt) }) }
    var error by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var trackPackageQuery by remember { mutableStateOf("") }
    var trackPackageNotice by remember { mutableStateOf<String?>(null) }
    var trackPackageNoticeIsError by remember { mutableStateOf(false) }
    var searchExpanded by remember { mutableStateOf(false) }
    var loadKey by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    var tab by remember(trackedOnly) {
        mutableStateOf(AppBrowserPreferences.loadTab(context, trackedOnly))
    }
    var sort by remember(trackedOnly) {
        mutableStateOf(AppBrowserPreferences.loadSort(context, trackedOnly))
    }
    LaunchedEffect(tab, trackedOnly) {
        AppBrowserPreferences.saveTab(context, trackedOnly, tab)
    }
    LaunchedEffect(sort, trackedOnly) {
        AppBrowserPreferences.saveSort(context, trackedOnly, sort)
    }
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

            tracked = TrackingStore.trackPackage(context, packageKey)
            trackingSnapshots = TrackingStore.loadSnapshots(context, tracked)
            TrackingCheckScheduler.sync(context)
            requestNotificationPermission()
            tab = AppListTab.Tracked
            trackPackageQuery = ""
            if (trackedOnly) query = ""
            trackPackageNotice = "Waiting for $packageKey to appear in the Morphe index."
            trackPackageNoticeIsError = false
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
            return
        }
        if (trackingPackageKey(app.packageName) in tracked) {
            trackPackageNotice = "${app.name} is already tracked."
            trackPackageNoticeIsError = false
            tab = AppListTab.Tracked
            return
        }

        tracked = TrackingStore.toggle(context, app)
        trackingSnapshots = TrackingStore.loadSnapshots(context, tracked)
        TrackingCheckScheduler.sync(context)
        requestNotificationPermission()
        tab = AppListTab.Tracked
        trackPackageQuery = ""
        if (trackedOnly) query = ""
        trackPackageNotice = "Tracking ${app.name}."
        trackPackageNoticeIsError = false
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
    }

    fun removePendingPackage(packageName: String) {
        tracked = TrackingStore.untrack(context, packageName)
        trackingSnapshots = TrackingStore.loadSnapshots(context, tracked)
        TrackingCheckScheduler.sync(context)
        trackPackageNotice = "Stopped waiting for $packageName."
        trackPackageNoticeIsError = false
    }

    LaunchedEffect(Unit) {
        favourites = FavouriteApps.load(context)
        tracked = TrackingStore.load(context)
        trackingSnapshots = TrackingStore.loadSnapshots(context, tracked)
        installedPackages = withContext(Dispatchers.IO) {
            context.packageManager.getInstalledApplications(0)
                .mapNotNull { it.packageName.takeIf(String::isNotBlank) }
                .toSet()
        }
    }

    DisposableEffect(context) {
        val observer = TrackingStore.observe(context) {
            tracked = TrackingStore.load(context)
            trackingSnapshots = TrackingStore.loadSnapshots(context, tracked)
        }
        onDispose { observer.close() }
    }

    // 1. Load cached index immediately for both Find apps and Tracking so local opens are instant
    LaunchedEffect(loadKey, trackedOnly) {
        val cached = withContext(Dispatchers.IO) { MorpheArchive.loadCachedIndex(context) }
        if (cached != null) {
            val sortedCached = cached.apps.sortedBy { it.name.lowercase(Locale.US) }
            if (apps == null || cached.generatedAt != archiveIndex?.generatedAt) {
                apps = sortedCached
                archiveIndex = cached
                freshness = archiveFreshness(cached.generatedAt)
            }
            if (trackedOnly) {
                TrackingStore.cacheTrackedApps(context, sortedCached)
            }
        } else if (apps == null) {
            archiveIndex = null
            error = null
        }

        // forceRefresh = true bypasses the 60-second in-memory cache so the reload
        // button always triggers a real network fetch, not a silent cache hit.
        val forceRefresh = loadKey > 0
        if (forceRefresh) {
            // Reset displayed state so the UI shows a loading indicator while
            // the fresh data arrives, and so the cache-guard below never blocks
            // applying the new result.
            apps = null
            archiveIndex = null
            freshness = null
            error = null
        }
        try {
            val index = MorpheArchive.fetchIndex(context, forceRefresh = forceRefresh)
            val sortedFresh = withContext(Dispatchers.Default) {
                index.apps.sortedBy { it.name.lowercase(Locale.US) }
            }
            withContext(Dispatchers.IO) {
                MorpheArchive.invalidatePatchListCache()
                if (trackedOnly) {
                    TrackingStore.cacheTrackedApps(context, sortedFresh)
                }
            }
            apps = sortedFresh
            archiveIndex = index
            freshness = archiveFreshness(index.generatedAt)
            error = null
        } catch (e: Exception) {
            if (apps == null) {
                val fallbackTracked = if (trackedOnly) {
                    withContext(Dispatchers.IO) { TrackingStore.loadCachedTrackedApps(context) }
                } else {
                    emptyList()
                }
                if (fallbackTracked.isNotEmpty()) {
                    apps = fallbackTracked
                    error = null
                } else {
                    error = e.message ?: "Failed to load the app index"
                }
            }
        }
    }

    LaunchedEffect(searchExpanded) {
        if (searchExpanded) {
            runCatching { searchFocusRequester.requestFocus() }
        }
    }

    // A kept-composed page must not consume Back while another page is shown.
    val pageSelected = LocalPagerPageSelected.current
    BackHandler(enabled = searchExpanded && pageSelected) {
        query = ""
        searchExpanded = false
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
    }

    LaunchedEffect(query) {
        if (query.isNotEmpty() &&
            (listState.firstVisibleItemIndex != 0 || listState.firstVisibleItemScrollOffset != 0)
        ) {
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
                app.packageName to app.trackedChanges(
                    trackingSnapshots[trackingPackageKey(app.packageName)]
                )
            }
        }
        val indexedPackages = remember(loaded) {
            loaded.orEmpty()
                .mapNotNull { it.packageName.takeIf(String::isNotBlank)?.let(::trackingPackageKey) }
                .toSet()
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
                loaded.orEmpty()
                    .asSequence()
                    .filter { app ->
                        (!trackedOnly || trackingPackageKey(app.packageName) in tracked) &&
                                (q.isBlank() || app.matchesQuery(qLower))
                    }
                    .filter { app ->
                        when (tab) {
                            AppListTab.All -> true
                            AppListTab.Favourites -> app.packageName in favourites
                            AppListTab.Tracked -> trackingPackageKey(app.packageName) in tracked
                            AppListTab.Installed -> app.packageName in installedPackages
                            AppListTab.NotInstalled -> app.packageName !in installedPackages
                        }
                    }
                    .toList()
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
                .hazeSourceIfEnabled(headerHaze),
            contentPadding = PaddingValues(
                top = headerHeightDp + 10.dp,
                start = AppDefaults.ContentPadding,
                end = AppDefaults.ContentPadding,
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
                                query = it
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
                        if (!trackedOnly) {
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
                        }

                        // Sort Dropdown Button
                        Box(modifier = if (trackedOnly) Modifier.fillMaxWidth() else Modifier.weight(1f)) {
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
                        Column(verticalArrangement = Arrangement.spacedBy(AppDefaults.ItemSpacing)) {
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
                        AppListCard(
                            app = app,
                            installed = app.packageName in installedPackages,
                            favourite = app.packageName in favourites,
                            tracked = trackingPackageKey(app.packageName) in tracked,
                            updates = changes.updates,
                            newPatches = changes.newPatches,
                            shape = cardShape,
                            useGradient = useGradientCards,
                            onToggleFavourite = {
                                favourites = FavouriteApps.toggle(context, app.packageName)
                            },
                            onToggleTracking = {
                                val adding = trackingPackageKey(app.packageName) !in tracked
                                tracked = TrackingStore.toggle(context, app)
                                trackingSnapshots = TrackingStore.loadSnapshots(context, tracked)
                                TrackingCheckScheduler.sync(context)
                                if (adding) requestNotificationPermission()
                            },
                            onClick = {
                                if (trackingPackageKey(app.packageName) in tracked) {
                                    TrackingStore.markSeen(context, app)
                                    trackingSnapshots = TrackingStore.loadSnapshots(context, tracked)
                                }
                                focusManager.clearFocus(force = true)
                                keyboardController?.hide()
                                onOpenApp(app)
                            },
                            modifier = Modifier
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
        AnimatedFab(
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
                        SearchField(
                            query = query,
                            onQueryChange = { query = it },
                            placeholder = stringResource(R.string.search_apps_or_packages),
                            focusRequester = searchFocusRequester,
                            modifier = Modifier.weight(1f)
                        )

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
                            HeaderIconButton(
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
                                    trackedOnly && tracked.isEmpty() ->
                                        "Track updates and new patches"
                                    trackedOnly ->
                                        "${tracked.size} tracked \u00b7 Background check every 6h"
                                    fresh != null -> "Updated ${fresh.label}"
                                    apps == null -> "Loading…"
                                    else -> "Community patch index"
                                },
                                color = if (fresh?.stale == true && !trackedOnly) {
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
                            onClick = {
                                if (trackedOnly) {
                                    TrackingCheckScheduler.checkNow(context)
                                    trackingSnapshots = TrackingStore.loadSnapshots(context, tracked)
                                }
                                loadKey++
                            }
                        )

                        if (!trackedOnly) {
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
}
