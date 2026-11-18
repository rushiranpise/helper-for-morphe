package dev.rushi.apkdownloadhelper.ui

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
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
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.05f
    val targetContainer = if (selected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (isDark) 0.55f else 0.72f)
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
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
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
        if (loadKey > 0) MorpheArchive.invalidatePatchListCache()
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
                bottom = 160.dp
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
                            onClick = {
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

        LazyListScrollbar(
            listState = listState,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .fillMaxHeight()
                .padding(top = headerHeightDp)
                .navigationBarsPadding()
        )

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
                    .height(40.dp)
            ) { isSearchActive ->
                if (isSearchActive) {
                    // Search bar matching HeaderControlDefaults.Size = 40.dp
                    Row(
                        modifier = Modifier.fillMaxSize(),
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

                        // Close search — active/selected glass style (primaryContainer tinted)
                        GlassHeaderButton(
                            icon = Icons.Outlined.SearchOff,
                            contentDescription = "Close search",
                            active = true,
                            onClick = {
                                query = ""
                                searchExpanded = false
                            }
                        )
                    }
                } else {
                    // Normal left-aligned header — glass controls matching HeaderControlDefaults
                    Row(
                        modifier = Modifier.fillMaxSize(),
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
    modifier: Modifier = Modifier
) {
    var expanded by remember(versions) { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val hasOverflow by remember {
        derivedStateOf { scrollState.maxValue > 0 }
    }
    val showToggle = expanded || hasOverflow

    if (expanded) {
        Row(
            modifier = modifier.fillMaxWidth(),
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
                        borderColor = borderColorFor?.invoke(ver)
                    )
                }
            }
            VersionExpandToggleButton(
                expanded = true,
                onClick = { expanded = false }
            )
        }
    } else {
        Row(
            modifier = modifier.fillMaxWidth(),
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
                    onClick = { expanded = true }
                )
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
    borderColor: Color? = null
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
            maxLines = 1
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

@Composable
internal fun AppDetailView(
    app: ArchiveApp,
    installed: Boolean = false,
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
    var headerHeightDp by remember { mutableStateOf(0.dp) }

    val openUrl: (String) -> Unit = { url ->
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }
    val sourceGroups = remember(app.sources) { groupArchiveSources(app.sources) }
    val appPalette = remember(app.iconColor, app.packageName) { app.findAppCardColors() }
    val sourceAccent = if (monochrome) scheme.primary else appPalette.gradient[1]
    val released = formatReleaseDate(app.newestReleaseDate())

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.background)
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(scheme.background)
        ) {
            // 1. Base App Details Layer
            LazyColumn(
                state = detailListState,
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(state = detailHeaderHaze),
                contentPadding = PaddingValues(
                    top = headerHeightDp + 10.dp,
                    start = MorpheDefaults.ContentPadding,
                    end = MorpheDefaults.ContentPadding,
                    bottom = 32.dp
                ),
                verticalArrangement = Arrangement.spacedBy(MorpheDefaults.ItemSpacing)
            ) {
                // 1. Sources, Versions & Updated pills directly below the header
                item(key = "meta_pills") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MorpheStatusBadge(
                            text = "${app.sourceCount} " +
                                    if (app.sourceCount == 1) "source" else "sources",
                            icon = Icons.Outlined.Dns,
                            containerColor = scheme.surfaceVariant.copy(alpha = 0.55f),
                            contentColor = scheme.onSurfaceVariant
                        )
                        if (app.versions.isNotEmpty()) {
                            MorpheStatusBadge(
                                text = "${app.versions.size} " +
                                        if (app.versions.size == 1) "version" else "versions",
                                icon = Icons.Outlined.History,
                                containerColor = scheme.surfaceVariant.copy(alpha = 0.55f),
                                contentColor = scheme.onSurfaceVariant
                            )
                        }
                        released?.let { date ->
                            MorpheStatusBadge(
                                text = "Updated $date",
                                containerColor = scheme.surfaceVariant.copy(alpha = 0.55f),
                                contentColor = scheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 2. Versions section with plain icon
                if (app.versions.isNotEmpty()) {
                    item(key = "versions") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.History,
                                    contentDescription = null,
                                    tint = scheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Versions",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = scheme.onSurface
                                )
                            }
                            AppVersionStrip(app.versions)
                        }
                    }
                }

                // 3. Sources section with plain icon
                if (sourceGroups.isEmpty()) {
                    item(key = "no_sources") {
                        MorpheEmptyState(message = "No patch sources listed for this app.")
                    }
                } else {
                    item(key = "sources_header") {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Dns,
                                contentDescription = null,
                                tint = scheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Sources",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = scheme.onSurface
                            )
                        }
                    }
                    items(
                        items = sourceGroups,
                        key = { group ->
                            group.primary.patches.map { it.name }.sorted().joinToString("\u0000")
                        }
                    ) { group ->
                        AppSourceCard(
                            source = group.primary,
                            accentColor = sourceAccent,
                            mirrors = group.mirrors,
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

            // 2. Telegram-style top dark gradient scrim so status bar & floating pills stay crisp on scroll
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(headerHeightDp + 16.dp)
                    .background(
                        Brush.verticalGradient(
                            0.0f to scheme.background.copy(alpha = 0.96f),
                            0.68f to scheme.background.copy(alpha = 0.84f),
                            1.0f to Color.Transparent
                        )
                    )
            )

            // 3. Telegram-style floating 3-pill header (all 40.dp height matching Find Apps header)
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .onSizeChanged { size ->
                        headerHeightDp = with(density) { size.height.toDp() }
                    }
                    .statusBarsPadding()
                    .padding(
                        horizontal = MorpheDefaults.ContentPadding,
                        vertical = 12.dp
                    ),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular Blurred Back button (40.dp)
                if (onBack != null) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .glassFill(
                                glass = MorpheColors.glassContainerColor(),
                                shape = CircleShape,
                                hazeState = detailHeaderHaze
                            )
                            .clickable(role = Role.Button, onClick = onBack),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = scheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Center Blurred + Gradient App Pill (40.dp height, full-bleed 40.dp logo)
                val titleColor = if (monochrome) scheme.onSurface else appPalette.title
                val subtitleColor = if (monochrome) scheme.onSurfaceVariant else appPalette.subtitle

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(CircleShape)
                        .glassFill(
                            glass = MorpheColors.glassContainerColor(),
                            shape = CircleShape,
                            hazeState = detailHeaderHaze
                        )
                        .drawWithCache {
                            val corner = CornerRadius(size.height / 2f)
                            val fill: Brush = if (monochrome) {
                                SolidColor(scheme.surfaceContainerHigh.copy(alpha = 0.65f))
                            } else {
                                Brush.linearGradient(
                                    colors = listOf(
                                        appPalette.gradient[0].copy(alpha = 0.78f),
                                        appPalette.gradient[1].copy(alpha = 0.66f),
                                        appPalette.gradient[2].copy(alpha = 0.74f)
                                    ),
                                    start = Offset(0f, size.height),
                                    end = Offset(size.width, 0f)
                                )
                            }
                            onDrawBehind {
                                drawRoundRect(brush = fill, cornerRadius = corner)
                            }
                        }
                        .padding(end = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Exact 40.dp x 40.dp logo matching the 40.dp pill height (zero top/bottom gap)
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                        ) {
                            FindAppIcon(
                                app = app,
                                gradient = appPalette.gradient,
                                installed = installed,
                                size = 40.dp,
                                cornerRadius = 20.dp
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = app.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = titleColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = app.packageName,
                                style = MaterialTheme.typography.labelSmall,
                                color = subtitleColor.copy(alpha = 0.85f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Circular Blurred Play Store button (40.dp)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .glassFill(
                            glass = MorpheColors.glassContainerColor(),
                            shape = CircleShape,
                            hazeState = detailHeaderHaze
                        )
                        .clickable(role = Role.Button) {
                            context.openPlayStoreListing(
                                app.packageName,
                                playStoreUrl(app.packageName)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_src_play),
                        contentDescription = stringResource(R.string.open_in_play_store),
                        tint = Color.Unspecified,
                        modifier = Modifier.size(18.dp)
                    )
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
}

@Composable
internal fun AppSourceCard(
    source: ArchiveSource,
    accentColor: Color,
    onOpenUrl: (String) -> Unit,
    onAddToMorphe: (String) -> Unit,
    onViewPatches: () -> Unit,
    onViewMirrorPatches: (ArchiveSource) -> Unit,
    mirrors: List<ArchiveSource> = emptyList()
) {
    var mirrorsExpanded by remember(source.repo) { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    val surface = colors.surfaceColorAtElevation(2.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .drawWithCache {
                val corner = CornerRadius(24.dp.toPx())
                val fill = Brush.linearGradient(
                    colors = listOf(
                        lerp(surface, accentColor, 0.12f),
                        lerp(surface, accentColor, 0.035f),
                        surface
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height)
                )
                onDrawBehind {
                    drawRoundRect(brush = fill, cornerRadius = corner)
                    drawRoundRect(
                        brush = SolidColor(accentColor.copy(alpha = 0.28f)),
                        cornerRadius = corner,
                        style = Stroke(1.dp.toPx())
                    )
                }
            }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(accentColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncAvatar(
                        url = MorpheArchive.avatarUrlFor(source),
                        fallbackText = source.repo.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        size = 34.dp,
                        cornerRadius = 10.dp
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = source.repo,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    MorpheStatusBadgeRow {
                        MorpheStatusBadge(
                            text = "${source.patches.size} " +
                                    if (source.patches.size == 1) "patch" else "patches",
                            icon = Icons.Outlined.Extension
                        )
                        formatBundleRelease(source.latestChanges)?.let { release ->
                            MorpheStatusBadge(
                                text = release,
                                icon = Icons.Outlined.History
                            )
                        }
                    }
                }
            }

            // Three buttons: Add | Patches | Repo
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                source.addUrl?.takeIf { it.isNotBlank() }?.let { addUrl ->
                    MorphePillButton(
                        onClick = { onAddToMorphe(addUrl) },
                        icon = Icons.Outlined.Add,
                        contentDescription = stringResource(R.string.add_to_morphe),
                        label = "Add",
                        tone = SemanticTone.Primary,
                        modifier = Modifier.weight(1f)
                    )
                }
                MorphePillButton(
                    onClick = onViewPatches,
                    icon = Icons.Outlined.Extension,
                    contentDescription = "Patches",
                    label = "Patches",
                    modifier = Modifier.weight(1f)
                )
                source.webUrl?.takeIf { it.isNotBlank() }?.let { webUrl ->
                    MorphePillButton(
                        onClick = { onOpenUrl(webUrl) },
                        icon = Icons.Outlined.OpenInNew,
                        contentDescription = stringResource(R.string.open_repo),
                        label = "Repo",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Expandable "Same patches also provided by" section below buttons
            if (mirrors.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { mirrorsExpanded = !mirrorsExpanded }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Same patches in ${mirrors.size} other " +
                                    (if (mirrors.size == 1) "repo" else "repos"),
                            color = colors.onSurfaceVariant,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = if (mirrorsExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                            contentDescription = if (mirrorsExpanded) "Collapse mirrors" else "Expand mirrors",
                            tint = colors.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    AnimatedExpand(visible = mirrorsExpanded) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            MorpheDivider(fullWidth = true)
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
                                            fontWeight = FontWeight.Medium,
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
                                    MorphePillButton(
                                        onClick = { onViewMirrorPatches(mirror) },
                                        icon = Icons.Outlined.Extension,
                                        contentDescription = "View ${mirror.repo} patches"
                                    )
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
    val colors = MaterialTheme.colorScheme

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

    val filteredPatches = remember(source.patches, patchQuery) {
        if (patchQuery.isBlank()) {
            source.patches
        } else {
            source.patches.filter { patch ->
                patch.name.contains(patchQuery, ignoreCase = true) ||
                        patch.description?.contains(patchQuery, ignoreCase = true) == true ||
                        patch.versions.any { it.contains(patchQuery, ignoreCase = true) }
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
                bottom = 32.dp
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
                itemsIndexed(filteredPatches, key = { index, patch -> "${patch.name}_$index" }) { index, patch ->
                    val isFirst = index == 0
                    val isLast = index == filteredPatches.lastIndex

                    // =========================================================================
                    // Patch Card Corners & Spacing (matches Find Apps cards)
                    // outerRadius  = top corners of first card & bottom corners of last card
                    // innerRadius  = corners between middle cards
                    // cardSpacing  = vertical gap between patch cards
                    // =========================================================================
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
                        // Neutral high-contrast index badge (no dark accentColor tint)
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.surfaceVariant.copy(alpha = 0.75f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (index + 1).toString(),
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

                            ExpandableVersionRow(
                                versions = versionsToShow,
                                chipShape = CircleShape,
                                chipHorizontalPadding = 10.dp,
                                chipVerticalPadding = 4.dp,
                                chipSpacing = 6.dp,
                                containerColorFor = { ver ->
                                    if (ver == "Any version") {
                                        colors.surfaceVariant.copy(alpha = 0.45f)
                                    } else {
                                        colors.primaryContainer.copy(alpha = 0.55f)
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

        // Top Header: Source Name as title & Patch Count as subtitle (40.dp controls matching Find Apps)
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
                    .height(40.dp)
            ) { isSearchActive ->
                if (isSearchActive) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
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
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GlassHeaderButton(
                            icon = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            onClick = {
                                focusManager.clearFocus(force = true)
                                keyboardController?.hide()
                                onBack()
                            }
                        )

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
                                overflow = TextOverflow.Ellipsis
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
    }
}