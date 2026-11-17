package dev.rushi.apkdownloadhelper.ui

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Favorite
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.util.Locale


internal enum class AppListTab(
    val label: String,
    val icon: ImageVector,
    val contentDescription: String
) {
    All("All", Icons.Outlined.ViewList, "All"),
    Favourites("Liked", Icons.Outlined.FavoriteBorder, "Liked"),
    Installed("Installed", Icons.Outlined.CheckCircle, "Installed"),
    NotInstalled("Not installed", Icons.Outlined.Block, "Not installed")
}

internal enum class AppSort(val label: String, val icon: ImageVector, val rotation: Float = 0f) {
    AZ("A–Z", Icons.Outlined.SortByAlpha),
    ZA("Z–A", Icons.Outlined.SortByAlpha, 180f),
    Sources("Sources", Icons.Outlined.Extension),
    Newest("Newest", Icons.Outlined.History)
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
internal fun AppDisclaimerBanner() {
    SurfaceCard(
        cornerRadius = MorpheDefaults.CompactCornerRadius,
        borderWidth = 1.dp,
        borderColor = SemanticTone.Warning.accent.copy(alpha = 0.35f),
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
private fun CompactFilterChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconRotation: Float = 0f
) {
    val bgColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    }

    Surface(
        onClick = onClick,
        modifier = modifier.height(36.dp),
        shape = CircleShape,
        color = bgColor,
        contentColor = contentColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
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
    onBack: (() -> Unit)? = null,
    onOpenApp: (ArchiveApp) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var apps by remember { mutableStateOf<List<ArchiveApp>?>(null) }
    var freshness by remember { mutableStateOf<ArchiveFreshness?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var searchExpanded by remember { mutableStateOf(false) }
    var loadKey by remember { mutableIntStateOf(0) }
    var tab by remember { mutableStateOf(AppListTab.All) }
    var sort by remember { mutableStateOf(AppSort.AZ) }
    val context = LocalContext.current
    var favourites by remember { mutableStateOf<Set<String>>(emptySet()) }
    var installedPackages by remember { mutableStateOf<Set<String>>(emptySet()) }
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

    LaunchedEffect(Unit) {
        favourites = MorpheFavourites.load(context)
        installedPackages = withContext(Dispatchers.IO) {
            context.packageManager.getInstalledApplications(0)
                .mapNotNull { it.packageName.takeIf(String::isNotBlank) }
                .toSet()
        }
    }

    LaunchedEffect(loadKey) {
        apps = null
        error = null
        try {
            val index = MorpheArchive.fetchIndex()
            apps = index.apps.sortedBy { it.name.lowercase(Locale.US) }
            freshness = archiveFreshness(index.generatedAt)
        } catch (e: Exception) {
            error = e.message ?: "Failed to load the app index"
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
            .background(MaterialTheme.colorScheme.background)
    ) {
        val loaded = apps
        when {
            error != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .hazeSource(state = headerHaze)
                        .padding(
                            top = headerHeightDp + 12.dp,
                            start = MorpheDefaults.ContentPadding,
                            end = MorpheDefaults.ContentPadding,
                            bottom = 110.dp
                        ),
                    verticalArrangement = Arrangement.spacedBy(MorpheDefaults.ItemSpacing)
                ) {
                    InfoCard(error!!)
                    HelperButton(
                        text = "Retry",
                        onClick = { loadKey++ },
                        icon = Icons.Outlined.Refresh,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            loaded == null -> {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .hazeSource(state = headerHaze)
                        .padding(top = headerHeightDp + 32.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.Top
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(MorpheDefaults.ContentPaddingSmall))
                    Text(
                        stringResource(R.string.loading_app_index),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            else -> {
                val filtered =
                    remember(loaded, query, tab, sort, favourites, installedPackages) {
                        loaded
                            .filter { app ->
                                query.isBlank() ||
                                        app.name.contains(query, ignoreCase = true) ||
                                        app.packageName.contains(query, ignoreCase = true)
                            }
                            .filter { app ->
                                when (tab) {
                                    AppListTab.All -> true
                                    AppListTab.Favourites -> app.packageName in favourites
                                    AppListTab.Installed -> app.packageName in installedPackages
                                    AppListTab.NotInstalled -> app.packageName !in installedPackages
                                }
                            }
                            .let { matches ->
                                when (sort) {
                                    AppSort.AZ -> matches.sortedBy { it.name.lowercase(Locale.US) }
                                    AppSort.ZA -> matches.sortedByDescending {
                                        it.name.lowercase(
                                            Locale.US
                                        )
                                    }

                                    AppSort.Sources -> matches.sortedByDescending { it.sourceCount }
                                    AppSort.Newest -> matches
                                        .map { app -> app.newestReleaseDate().orEmpty() to app }
                                        .sortedByDescending { it.first }
                                        .map { it.second }
                                }
                            }
                    }

                // Full-width LazyColumn without scrollbar
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .hazeSource(state = headerHaze),
                    contentPadding = PaddingValues(
                        top = headerHeightDp + 10.dp,
                        start = MorpheDefaults.ContentPadding,
                        end = MorpheDefaults.ContentPadding,
                        bottom = 110.dp
                    )
                ) {
                    item(key = "controls") {
                        Column(
                            modifier = Modifier.padding(bottom = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AppDisclaimerBanner()

                            var filterMenuOpen by remember { mutableStateOf(false) }
                            var sortMenuOpen by remember { mutableStateOf(false) }

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
                                        modifier = Modifier.fillMaxWidth().height(36.dp),
                                        onClick = { filterMenuOpen = true }
                                    )
                                    DropdownMenu(
                                        expanded = filterMenuOpen,
                                        onDismissRequest = { filterMenuOpen = false }
                                    ) {
                                        AppListTab.entries.forEach { itemTab ->
                                            DropdownMenuItem(
                                                text = { Text(itemTab.label) },
                                                leadingIcon = { Icon(itemTab.icon, contentDescription = null) },
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
                                        modifier = Modifier.fillMaxWidth().height(36.dp),
                                        onClick = { sortMenuOpen = true }
                                    )
                                    DropdownMenu(
                                        expanded = sortMenuOpen,
                                        onDismissRequest = { sortMenuOpen = false }
                                    ) {
                                        AppSort.entries.forEach { itemSort ->
                                            DropdownMenuItem(
                                                text = { Text(itemSort.label) },
                                                leadingIcon = {
                                                    Icon(
                                                        itemSort.icon,
                                                        contentDescription = null,
                                                        modifier = Modifier.rotate(itemSort.rotation)
                                                    )
                                                },
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

                    if (filtered.isEmpty()) {
                        item(key = "empty") {
                            InfoCard(
                                when (tab) {
                                    AppListTab.Favourites ->
                                        "No liked apps yet. Tap the heart on any app to pin it here."

                                    AppListTab.Installed -> "No patched apps in the index are installed."
                                    AppListTab.NotInstalled ->
                                        "Every app in the index is installed on this device."

                                    AppListTab.All -> "No apps match \"$query\"."
                                }
                            )
                        }
                    } else {
                        itemsIndexed(
                            filtered,
                            key = { _, app -> app.packageName }) { index, app ->
                            val isFirst = index == 0
                            val isLast = index == filtered.lastIndex

//===================================================================================
// outerRadius = top of first card & bottom of last card
// innerRadius = all corners between middle cards (increase/decrease 12.dp to adjust)
//===================================================================================
                            val outerRadius = 22.dp
                            val innerRadius = 5.dp

                            val cardShape = RoundedCornerShape(
                                topStart = if (isFirst) outerRadius else innerRadius,
                                topEnd = if (isFirst) outerRadius else innerRadius,
                                bottomStart = if (isLast) outerRadius else innerRadius,
                                bottomEnd = if (isLast) outerRadius else innerRadius
                            )

                            Box(
                                modifier = Modifier
                                    .animatedListItem(this)
                                    .padding(bottom = if (isLast) 0.dp else 2.dp)
                                    .clip(cardShape)
                                    .background(
                                        MaterialTheme.colorScheme.surfaceColorAtElevation(
                                            2.dp
                                        )
                                    )
                            ) {
                                AppBrowserRow(
                                    app = app,
                                    favourite = app.packageName in favourites,
                                    onToggleFavourite = {
                                        favourites =
                                            MorpheFavourites.toggle(context, app.packageName)
                                    },
                                    onClick = { onOpenApp(app) }
                                )
                            }
                        }
                    }
                }

                MorpheFab(
                    visible = listState.firstVisibleItemIndex > 0,
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
            }
        }


        // Rounded bottom header with Haze glass blur when scrolled
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .onSizeChanged { size ->
                        headerHeightDp = with(density) { size.height.toDp() }
                    }
                    .glassFill(
                        glass = MorpheColors.glassContainerColor(),
                        shape = headerShape,
                        hazeState = if (isScrolled || searchExpanded) headerHaze else null
                    )
                    .statusBarsPadding()
                    .padding(
                        horizontal = MorpheDefaults.ContentPadding,
                        vertical = 12.dp
                    )
            ) {
                AnimatedContent(
                    targetState = searchExpanded,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "header_search_transition"
                ) { isSearchActive ->
                    if (isSearchActive) {
                        // Expanded inline pill search bar on header (like image 4)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
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

                            // Circular close search button on right side
                            Surface(
                                onClick = {
                                    query = ""
                                    searchExpanded = false
                                },
                                modifier = Modifier.size(48.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.SearchOff,
                                        contentDescription = "Close search",
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        // Normal left-aligned header matching Helper & Settings tabs
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp),
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
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.find_new_apps),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val fresh = freshness
                                Text(
                                    text = buildString {
                                        append(
                                            apps?.let { "${it.size} apps with patches" }
                                                ?: "Morphe patch archive"
                                        )
                                        fresh?.let { append(" · index ${it.label}") }
                                    },
                                    color = if (fresh?.stale == true) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Surface(
                                onClick = { loadKey++ },
                                modifier = Modifier.size(48.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.Refresh,
                                        contentDescription = stringResource(R.string.refresh),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Surface(
                                onClick = { searchExpanded = true },
                                modifier = Modifier.size(48.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.Search,
                                        contentDescription = stringResource(R.string.search_apps_or_packages),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
    }
}

@Composable
internal fun AppBrowserRow(
    app: ArchiveApp,
    favourite: Boolean,
    onToggleFavourite: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppAvatar(
            packageName = app.packageName,
            initial = app.name.firstOrNull()?.uppercaseChar() ?: '?'
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = app.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = app.packageName,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            formatReleaseDate(app.newestReleaseDate())?.let { released ->
                Text(
                    text = "Updated $released",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = app.sourceCount.toString(),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = if (app.sourceCount == 1) "source" else "sources",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall
            )
        }
        Icon(
            imageVector = if (favourite) {
                Icons.Filled.Favorite
            } else {
                Icons.Outlined.FavoriteBorder
            },
            contentDescription = if (favourite) "Unlike" else "Like",
            tint = if (favourite) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier
                .size(36.dp)
                .clip(MorpheDefaults.PillShape)
                .clickable(onClick = onToggleFavourite)
                .padding(6.dp)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
internal fun AsyncAvatar(
    url: String?,
    fallbackText: String,
    size: Dp = 20.dp,
    cornerRadius: Dp = 6.dp
) {
    var bitmap by remember(url) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(url) {
        bitmap = if (url == null) {
            null
        } else {
            withContext(Dispatchers.IO) {
                runCatching {
                    val request = Request.Builder()
                        .url(url)
                        .header(
                            "User-Agent",
                            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0 Mobile Safari/537.36"
                        )
                        .build()
                    MorpheArchive.http.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) return@use null
                        response.body?.bytes()?.let { bytes ->
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        }?.asImageBitmap()
                    }
                }.getOrNull()
            }
        }
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap!!,
            contentDescription = null,
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(cornerRadius))
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

@Composable
internal fun AppDetailView(
    app: ArchiveApp,
    onBack: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = onBack != null) {
        onBack?.invoke()
    }
    val context = LocalContext.current
    val openUrl: (String) -> Unit = { url ->
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }
    val sourceGroups = remember(app.sources) { groupArchiveSources(app.sources) }
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(MorpheDefaults.ItemSpacing)
    ) {
        item {
            SectionCard {
                IconTextRow(
                    modifier = Modifier.padding(MorpheDefaults.ContentPadding),
                    leadingContent = {
                        AppAvatar(
                            packageName = app.packageName,
                            initial = app.name.firstOrNull()?.uppercaseChar() ?: '?'
                        )
                    },
                    title = app.name,
                    description = buildString {
                        append(app.packageName)
                        append(" · ${app.sourceCount} ")
                        append(if (app.sourceCount == 1) "source" else "sources")
                        if (app.versions.isNotEmpty()) {
                            append(" · ${app.versions.size} ")
                            append(if (app.versions.size == 1) "version" else "versions")
                        }
                    },
                    titleStyle = MaterialTheme.typography.titleLarge,
                    titleWeight = FontWeight.Bold,
                    trailingContent = {
                        HelperIconButton(
                            icon = Icons.Outlined.Storefront,
                            contentDescription = stringResource(R.string.open_in_play_store),
                            onClick = {
                                context.openPlayStoreListing(app.packageName, playStoreUrl(app.packageName))
                            }
                        )
                    }
                )
            }
        }
        if (app.versions.isNotEmpty()) {
            item {
                MorpheSectionTitle(text = "Versions", icon = Icons.Outlined.History)
                Text(
                    text = app.versions.joinToString(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        if (sourceGroups.isEmpty()) {
            item { MorpheEmptyState(message = "No patch sources listed for this app.") }
        } else {
            item {
                MorpheSectionTitle(text = "Sources", icon = Icons.Outlined.Dns)
            }
            items(sourceGroups, key = { it.primary.repo }) { group ->
                AppSourceCard(
                    source = group.primary,
                    mirrors = group.mirrors,
                    onOpenUrl = openUrl,
                    onAddToMorphe = { openAddSource(context, it) }
                )
            }
        }
    }
}

@Composable
internal fun AppSourceCard(
    source: ArchiveSource,
    onOpenUrl: (String) -> Unit,
    onAddToMorphe: (String) -> Unit,
    mirrors: List<ArchiveSource> = emptyList()
) {
    var expanded by remember(source.repo) { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    SettingsItemCard(
        onClick = null,
        borderWidth = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncAvatar(
                    url = MorpheArchive.avatarUrlFor(source),
                    fallbackText = source.repo.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    size = 28.dp,
                    cornerRadius = 8.dp
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = source.repo,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = buildString {
                            append("${source.patches.size} ")
                            append(if (source.patches.size == 1) "patch" else "patches")
                            formatBundleRelease(source.latestChanges)?.let { release ->
                                append(" · ")
                                append(release)
                            }
                        },
                        color = colors.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (mirrors.isNotEmpty()) {
                        Text(
                            text = "Same patches in ${mirrors.size} other " +
                                    (if (mirrors.size == 1) "repo" else "repos"),
                            color = colors.onSurfaceVariant.copy(alpha = 0.75f),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = if (expanded) "Collapse patches" else "Expand patches",
                    tint = colors.onSurfaceVariant
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(MorpheDefaults.ContentPaddingSmall)
            ) {
                source.addUrl?.takeIf { it.isNotBlank() }?.let { addUrl ->
                    MorphePillButton(
                        onClick = { onAddToMorphe(addUrl) },
                        icon = Icons.Outlined.Add,
                        contentDescription = stringResource(R.string.add_to_morphe),
                        label = stringResource(R.string.add_to_morphe),
                        tone = SemanticTone.Primary,
                        modifier = Modifier.weight(1f)
                    )
                }
                source.webUrl?.takeIf { it.isNotBlank() }?.let { webUrl ->
                    MorphePillButton(
                        onClick = { onOpenUrl(webUrl) },
                        icon = Icons.Outlined.OpenInNew,
                        contentDescription = stringResource(R.string.open_repo),
                        label = stringResource(R.string.open_repo),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            AnimatedExpand(visible = expanded) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (source.patches.isEmpty()) {
                        Text(
                            stringResource(R.string.no_patches_listed_for_this_source),
                            color = colors.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        val longList = source.patches.size > 5
                        val patchesScroll = rememberScrollState()
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(max = if (longList) 240.dp else Dp.Unspecified)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .then(
                                            if (longList) Modifier.verticalScroll(patchesScroll) else Modifier
                                        ),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    source.patches.forEach { patch ->
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Extension,
                                                    contentDescription = null,
                                                    tint = colors.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = patch.name,
                                                    fontWeight = FontWeight.Bold,
                                                    color = colors.onSurface,
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                            patch.description?.takeIf { it.isNotBlank() }?.let { desc ->
                                                Text(
                                                    text = desc,
                                                    color = colors.onSurfaceVariant,
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            if (longList) {
                                ScrollStateScrollbar(
                                    scrollState = patchesScroll,
                                    modifier = Modifier.fillMaxHeight()
                                )
                            }
                        }
                    }

                    if (mirrors.isNotEmpty()) {
                        MorpheDivider(fullWidth = true)
                        Text(
                            text = stringResource(R.string.same_patches_also_provided_by),
                            color = colors.onSurfaceVariant,
                            style = MaterialTheme.typography.labelMedium
                        )
                        mirrors.forEach { mirror ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncAvatar(
                                    url = MorpheArchive.avatarUrlFor(mirror),
                                    fallbackText = mirror.repo
                                        .firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                    size = 24.dp,
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
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    formatBundleRelease(mirror.latestChanges)?.let { release ->
                                        Text(
                                            text = release,
                                            color = colors.onSurfaceVariant.copy(alpha = 0.75f),
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                mirror.addUrl?.takeIf { it.isNotBlank() }?.let { addUrl ->
                                    MorphePillButton(
                                        onClick = { onAddToMorphe(addUrl) },
                                        icon = Icons.Outlined.Add,
                                        contentDescription = "Add ${mirror.repo} to Morphe"
                                    )
                                }
                                mirror.webUrl?.takeIf { it.isNotBlank() }?.let { webUrl ->
                                    MorphePillButton(
                                        onClick = { onOpenUrl(webUrl) },
                                        icon = Icons.Outlined.OpenInNew,
                                        contentDescription = "Open ${mirror.repo}"
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