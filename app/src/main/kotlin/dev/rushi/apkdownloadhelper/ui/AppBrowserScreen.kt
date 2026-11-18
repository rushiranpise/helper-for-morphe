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
        val filtered = remember(loaded, query, tab, sort, favourites, installedPackages) {
            loaded.orEmpty()
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
                        AppSort.ZA -> matches.sortedByDescending { it.name.lowercase(Locale.US) }
                        AppSort.Sources -> matches.sortedByDescending { it.sourceCount }
                        AppSort.Newest -> matches
                            .map { app -> app.newestReleaseDate().orEmpty() to app }
                            .sortedByDescending { it.first }
                            .map { it.second }
                    }
                }
        }

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
            // Always show Disclaimer, Filter, and Sort immediately
            item(key = "controls") {
                Column(
                    modifier = Modifier.padding(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppDisclaimerBanner()

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
                                            .height(36.dp)
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
                                        AppListTab.entries.forEach { itemTab ->
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
                                            .height(36.dp)
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
                            modifier = Modifier.padding(bottom = if (isLast) 0.dp else 3.dp)
                        )
                    }
                }

                filtered.isEmpty() -> {
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

                        AppBrowserRow(
                            app = app,
                            installed = app.packageName in installedPackages,
                            favourite = app.packageName in favourites,
                            shape = cardShape,
                            onToggleFavourite = {
                                favourites = MorpheFavourites.toggle(context, app.packageName)
                            },
                            onClick = { onOpenApp(app) },
                            modifier = Modifier
                                .animatedListItem(this)
                                .padding(bottom = if (isLast) 0.dp else 3.dp)
                        )
                    }
                }
            }
        }

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


        // Rounded bottom header with Haze glass blur when scrolled
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
    installed: Boolean,
    favourite: Boolean,
    shape: RoundedCornerShape,
    onToggleFavourite: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val monochrome = LocalMonochromeTheme.current
    val palette = remember(app.iconColor, app.packageName) { app.findAppCardColors() }
    val scheme = MaterialTheme.colorScheme
    val titleColor = if (monochrome) scheme.onSurface else palette.title
    val subtitleColor = if (monochrome) scheme.onSurfaceVariant else palette.subtitle
    val interactionSource = remember { MutableInteractionSource() }

    val titleStyle = MaterialTheme.typography.titleMedium.copy(
        fontWeight = FontWeight.SemiBold,
        shadow = if (monochrome) {
            null
        } else {
            Shadow(Color.Black.copy(alpha = 0.4f), Offset(0f, 2f), 4f)
        }
    )
    val subtitleStyle = MaterialTheme.typography.bodySmall.copy(
        shadow = if (monochrome) {
            null
        } else {
            Shadow(Color.Black.copy(alpha = 0.35f), Offset(0f, 1f), 2f)
        }
    )
    val released = formatReleaseDate(app.newestReleaseDate())
    var contentVisible by remember(app.packageName) { mutableStateOf(false) }
    LaunchedEffect(app.packageName) { contentVisible = true }
    val contentAlpha by animateFloatAsState(
        targetValue = if (contentVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 280),
        label = "card_content_fade"
    )

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

                val fill: Brush = if (monochrome) {
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
                .graphicsLayer { alpha = contentAlpha }
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
                                text = "Updated $released",
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

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (favourite) Color.White.copy(alpha = 0.92f) else Color.White.copy(alpha = 0.16f)
                    )
                    .clickable(role = Role.Button, onClick = onToggleFavourite),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (favourite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = if (favourite) "Unlike" else "Like",
                    tint = if (favourite) {
                        Color(0xFFD32F2F)
                    } else if (monochrome) {
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
    modifier: Modifier = Modifier
) {
    val monochrome = LocalMonochromeTheme.current
    val scheme = MaterialTheme.colorScheme
    val palette = FindAppPlaceholderGradients[index % FindAppPlaceholderGradients.size]
    val skeletonColor = if (monochrome) scheme.onSurface else Color.White

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
                if (monochrome) {
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
private fun FindAppIcon(app: ArchiveApp, gradient: List<Color>, installed: Boolean) {
    val initial = app.name.firstOrNull()?.uppercaseChar() ?: '?'
    if (installed) {
        val icon = rememberInstalledAppIcon(app.packageName)
        if (icon != null) {
            Image(
                bitmap = icon,
                contentDescription = null,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
            )
        } else {
            AsyncAvatar(
                url = null,
                fallbackText = initial.toString(),
                size = 56.dp,
                cornerRadius = 16.dp
            )
        }
        return
    }
    val iconUrl = app.iconUrl?.takeIf { it.isNotBlank() }
    AsyncAvatar(
        url = iconUrl,
        fallbackText = initial.toString(),
        size = 56.dp,
        cornerRadius = 16.dp,
        fallbackGradient = gradient.takeIf { iconUrl == null }
    )
}

private val archiveIconCache = ConcurrentHashMap<String, ImageBitmap>()

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
        archiveIconCache[iconUrl]?.let {
            bitmap = it
            return@LaunchedEffect
        }
        bitmap = withContext(Dispatchers.IO) {
            runCatching {
                val request = Request.Builder()
                    .url(iconUrl)
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
        }?.also { archiveIconCache[iconUrl] = it }
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