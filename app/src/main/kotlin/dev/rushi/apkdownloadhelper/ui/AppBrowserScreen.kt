package dev.rushi.apkdownloadhelper.ui

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Check
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
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SortByAlpha
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import dev.rushi.apkdownloadhelper.MORPHE_MANAGER_PACKAGES
import dev.rushi.apkdownloadhelper.R
import dev.rushi.apkdownloadhelper.download.openPlayStoreListing
import dev.rushi.apkdownloadhelper.download.playStoreUrl
import dev.rushi.apkdownloadhelper.updater.ArchiveApp
import dev.rushi.apkdownloadhelper.updater.ArchiveFreshness
import dev.rushi.apkdownloadhelper.updater.ArchiveSource
import dev.rushi.apkdownloadhelper.updater.MorpheArchive
import dev.rushi.apkdownloadhelper.updater.MorpheArchiveIndex
import dev.rushi.apkdownloadhelper.updater.archiveFreshness
import dev.rushi.apkdownloadhelper.updater.formatBundleRelease
import dev.rushi.apkdownloadhelper.updater.formatReleaseDate
import dev.rushi.apkdownloadhelper.updater.groupArchiveSources
import dev.rushi.apkdownloadhelper.updater.newestReleaseDate
import kotlinx.coroutines.CancellationException
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
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume


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

/** How the archive list is ordered. */
internal enum class AppSort(val label: String, val icon: ImageVector, val rotation: Float = 0f) {
    AZ("A–Z", Icons.Outlined.SortByAlpha),
    // Same glyph flipped 180° so it reads descending (mirror of A–Z).
    ZA("Z–A", Icons.Outlined.SortByAlpha, 180f),
    Sources("Sources", Icons.Outlined.Extension),
    // Newest first, by the most recent release any of the app's sources declares.
    Newest("Newest", Icons.Outlined.History)
}

/**
 * Persisted favourites for the "Find New Apps" browser. Survives app
 * launches so a pinned app stays pinned even as the live index grows.
 */
internal object MorpheFavourites {
    private const val PREFS = "morphe_favourites"
    private const val KEY = "packages"

    fun load(context: Context): Set<String> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY, emptySet())
            .orEmpty()

    /** Toggles [packageName] and returns the new set. */
    fun toggle(context: Context, packageName: String): Set<String> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val current = prefs.getStringSet(KEY, emptySet()).orEmpty().toMutableSet()
        if (!current.add(packageName)) current.remove(packageName)
        prefs.edit().putStringSet(KEY, current).apply()
        return current
    }
}

/** One number in the archive dashboard. */
private data class FindAppsStat(
    val label: String,
    val value: Int,
    val icon: ImageVector
)

/** Four archive totals, shown as one compact panel before the list warning. */
@Composable
private fun FindAppsStatsDashboard(index: MorpheArchiveIndex) {
    val scheme = MaterialTheme.colorScheme
    val stats = listOf(
        FindAppsStat("SOURCES", index.repoCount, Icons.Outlined.Dns),
        FindAppsStat("APPS", index.appCount, Icons.Outlined.Apps),
        FindAppsStat("PATCHES", index.patchCount, Icons.Outlined.Extension),
        FindAppsStat("UNIVERSAL", index.universalPatchCount, Icons.Outlined.Public)
    )
    SurfaceCard(
        cornerRadius = MorpheDefaults.SectionCornerRadius,
        borderWidth = 1.dp,
        borderColor = scheme.primary.copy(alpha = 0.20f),
        color = scheme.surfaceColorAtElevation(2.dp)
    ) {
        Column {
            stats.chunked(2).forEachIndexed { rowIndex, row ->
                if (rowIndex > 0) {
                    MorpheDivider(fullWidth = true)
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    row.forEachIndexed { columnIndex, stat ->
                        if (columnIndex > 0) {
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(54.dp)
                                    .align(Alignment.CenterVertically)
                                    .background(scheme.outlineVariant.copy(alpha = 0.45f))
                            )
                        }
                        FindAppsStatItem(stat = stat, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun FindAppsStatItem(stat: FindAppsStat, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier.padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(scheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = stat.icon,
                contentDescription = null,
                tint = scheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = stat.value.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = scheme.onSurface,
                maxLines = 1
            )
            Text(
                text = stat.label,
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

/** Sticky disclaimer pinned above the archive list. */
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
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ThemedIcon(
                icon = Icons.Outlined.Warning,
                size = 20.dp,
                tint = SemanticTone.Warning.accent
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.patch_index_maintained_by_the_community),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Community bundles are maintained by their respective authors and " +
                        "are not individually verified. Morphe and the developer of this app are " +
                        "not responsible for third-party patches.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

/**
 * "Find New Apps" browser: fetches the Morphe archive index live on every
 * open (never cached), offers a searchable list of all patched apps, and a
 * per-app detail view with its versions, patches, and source repos.
 */
@Composable
internal fun AppBrowserScreen(
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var apps by remember { mutableStateOf<List<ArchiveApp>?>(null) }
    var archiveIndex by remember { mutableStateOf<MorpheArchiveIndex?>(null) }
    // When the archive built the index, so a build that predates a source's own
    // changes is visible rather than silently misleading.
    var freshness by remember { mutableStateOf<ArchiveFreshness?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<ArchiveApp?>(null) }
    var loadKey by remember { mutableIntStateOf(0) }
    var tab by remember { mutableStateOf(AppListTab.All) }
    var sort by remember { mutableStateOf(AppSort.AZ) }
    var optionsOpen by remember { mutableStateOf(false) }
    var searchOpen by remember { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    var favourites by remember { mutableStateOf<Set<String>>(emptySet()) }
    var installedPackages by remember { mutableStateOf<Set<String>>(emptySet()) }
    // Hoisted above the list/detail branch so the scroll position survives
    // opening an app's details and coming back — remember inside the list
    // branch would be discarded when the detail view replaces the list.
    val listState = rememberLazyListState()
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
        archiveIndex = null
        freshness = null
        error = null
        try {
            val index = MorpheArchive.fetchIndex()
            apps = index.apps.sortedBy { it.name.lowercase(Locale.US) }
            archiveIndex = index
            freshness = archiveFreshness(index.generatedAt)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message ?: "Failed to load the app index"
        }
    }

    // Back steps out of the innermost layer first: app detail, then the list
    // options, then search, and only from the list itself does it close the browser.
    BackHandler(enabled = selected != null || optionsOpen || searchOpen) {
        when {
            selected != null -> selected = null
            optionsOpen -> optionsOpen = false
            searchOpen -> searchOpen = false
        }
    }

    LaunchedEffect(searchOpen) {
        if (searchOpen) {
            searchFocusRequester.requestFocus()
            keyboardController?.show()
        } else {
            query = ""
        }
    }

    // A query narrows the list from wherever it is scrolled, so bring the best
    // matches to the top where the eye already is.
    LaunchedEffect(query) {
        if (listState.firstVisibleItemIndex != 0 || listState.firstVisibleItemScrollOffset != 0) {
            listState.scrollToItem(0)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(
                horizontal = MorpheDefaults.ContentPadding,
                vertical = MorpheDefaults.ContentPadding
            ),
        verticalArrangement = Arrangement.spacedBy(MorpheDefaults.ItemSpacing)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (searchOpen) {
                FindAppsSearchField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(searchFocusRequester)
                )
                FindAppsHeaderAction(
                    icon = Icons.Outlined.Close,
                    contentDescription = "Close search",
                    onClick = { searchOpen = false }
                )
            } else {
                // The list is a bottom-tab root, so only show a back arrow when it
                // came from another screen or is leaving an app detail.
                if (selected != null || onBack != null) {
                    FindAppsHeaderAction(
                        icon = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        onClick = {
                            if (selected != null) selected = null else onBack?.invoke()
                        }
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = if (selected == null) {
                            stringResource(R.string.find_new_apps)
                        } else {
                            stringResource(R.string.app_details)
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    // No app name here when a detail is open: the card below already
                    // headings with it, so it would read as the same name twice.
                    if (selected == null) {
                        val fresh = freshness
                        Text(
                            text = when {
                                fresh != null -> "Updated ${fresh.label}"
                                apps == null -> "Loading ..."
                                else -> "Community patch index"
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
                }
                if (selected == null) {
                    FindAppsHeaderAction(
                        icon = Icons.Outlined.Tune,
                        contentDescription = "List options",
                        onClick = { optionsOpen = !optionsOpen },
                        selected = optionsOpen,
                        marked = tab != AppListTab.All,
                        enabled = apps != null
                    )
                    FindAppsHeaderAction(
                        icon = Icons.Outlined.Search,
                        contentDescription = stringResource(R.string.search_apps_or_packages),
                        onClick = { searchOpen = true },
                        enabled = apps != null
                    )
                }
            }
        }

        val current = selected
        if (current != null) {
            AppDetailView(
                app = current,
                installed = current.packageName in installedPackages,
                onBack = { selected = null },
                modifier = Modifier.weight(1f)
            )
        } else {
            val loaded = apps
            when {
                error != null -> {
                    MorpheEmptyState(
                        message = error!!,
                        icon = Icons.Outlined.Warning,
                        actionLabel = "Retry",
                        onAction = { loadKey++ }
                    )
                }
                loaded == null -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(MorpheDefaults.ItemSpacing)
                    ) {
                        repeat(5) { FindAppCardLoading(index = it) }
                    }
                }
                else -> {
                    val index = archiveIndex
                    // Filter and sort the catalogue only when one of its inputs
                    // changes, not when scroll state toggles the header.
                    val filtered = remember(
                        loaded,
                        query,
                        tab,
                        sort,
                        favourites,
                        installedPackages
                    ) {
                        loaded
                            .asSequence()
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
                            .toList()
                            .let { matches ->
                                when (sort) {
                                    AppSort.AZ -> matches.sortedBy { it.name.lowercase(Locale.US) }
                                    AppSort.ZA ->
                                        matches.sortedByDescending { it.name.lowercase(Locale.US) }
                                    AppSort.Sources -> matches.sortedByDescending { it.sourceCount }
                                    // Keyed once per app and then sorted, rather than
                                    // recomputing the newest release on every comparison.
                                    AppSort.Newest -> matches
                                        .map { app -> app.newestReleaseDate().orEmpty() to app }
                                        .sortedByDescending { it.first }
                                        .map { it.second }
                                }
                            }
                    }
                    // The archive totals and warning scroll with the list instead of
                    // collapsing on the first drag, so the list viewport stays put.
                    AnimatedVisibility(
                        visible = optionsOpen,
                        enter = MorpheAnimations.expandFadeEnter,
                        exit = MorpheAnimations.shrinkFadeExit
                    ) {
                        FindAppsOptionsPanel(
                            tab = tab,
                            sort = sort,
                            onTabSelect = { tab = it },
                            onSortSelect = { sort = it },
                            onReset = {
                                tab = AppListTab.All
                                sort = AppSort.AZ
                            },
                            onRefresh = { loadKey++ },
                            onClose = { optionsOpen = false }
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                // Keep the last row clear of the floating
                                // "Go to top" button.
                                contentPadding = PaddingValues(bottom = 64.dp),
                                verticalArrangement = Arrangement.spacedBy(MorpheDefaults.ItemSpacing)
                            ) {
                                if (index != null) {
                                    item(key = "find-apps-stats", contentType = "header") {
                                        FindAppsStatsDashboard(index)
                                    }
                                }
                                item(key = "find-apps-warning", contentType = "header") {
                                    AppDisclaimerBanner()
                                }
                                if (filtered.isEmpty()) {
                                    item(key = "find-apps-empty", contentType = "empty") {
                                        MorpheEmptyState(
                                            message = when (tab) {
                                                AppListTab.Favourites ->
                                                    "No liked apps yet. Tap the heart on any app to pin it here."
                                                AppListTab.Installed ->
                                                    "No patched apps in the index are installed."
                                                AppListTab.NotInstalled ->
                                                    "Every app in the index is installed on this device."
                                                AppListTab.All -> "No apps match \"$query\"."
                                            },
                                            icon = when (tab) {
                                                AppListTab.Favourites -> Icons.Outlined.FavoriteBorder
                                                AppListTab.Installed -> Icons.Outlined.CheckCircle
                                                AppListTab.NotInstalled -> Icons.Outlined.Block
                                                AppListTab.All -> Icons.Outlined.Search
                                            }
                                        )
                                    }
                                } else {
                                    items(
                                        items = filtered,
                                        key = { it.packageName },
                                        contentType = { "app" }
                                    ) { app ->
                                        FindAppCard(
                                            app = app,
                                            installed = app.packageName in installedPackages,
                                            favourite = app.packageName in favourites,
                                            onToggleFavourite = {
                                                favourites = MorpheFavourites.toggle(context, app.packageName)
                                            },
                                            onClick = {
                                                optionsOpen = false
                                                selected = app
                                            }
                                        )
                                    }
                                }
                            }
                            LazyListScrollbar(
                                listState = listState,
                                modifier = Modifier.fillMaxHeight()
                            )
                        }
                        // Quick jump back to the top for long catalogs; only
                        // shown once the user has scrolled down, and it pops in
                        // with the manager's floating-button motion. A clickable
                        // Surface is natively exposed to TalkBack via the
                        // icon's contentDescription.
                        MorpheFab(
                            visible = listState.firstVisibleItemIndex > 0,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 32.dp, bottom = 20.dp)
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
            }
        }
    }
}

/** Colours a glass app card draws itself in, resolved from the app's own accent. */
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

private val FindAppPlaceholderGradients = listOf(
    listOf(Color(0xFF1E5AA8), Color(0xFF00AFAE)),
    listOf(Color(0xFF6A3AB2), Color(0xFF3D5AFE)),
    listOf(Color(0xFFB3541E), Color(0xFFE0A030)),
    listOf(Color(0xFF2E7D32), Color(0xFF00AFAE))
)

/**
 * The manager's card palette: the app's accent carried through a three-stop gradient and
 * normalised so white card text always has a dark enough middle to read against.
 */
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

/** Launcher icons for installed apps, kept so scrolling does not reload them. */
private val installedAppIconCache = ConcurrentHashMap<String, ImageBitmap>()

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
            // An installed app must never fall back to the bundle placeholder.
            AsyncAvatar(
                url = null,
                fallbackText = initial.toString(),
                size = 56.dp,
                cornerRadius = 16.dp
            )
        }
        return
    }
    // The archive stores missing icons as empty strings, not nulls.
    val iconUrl = app.iconUrl?.takeIf { it.isNotBlank() }
    AsyncAvatar(
        url = iconUrl,
        fallbackText = initial.toString(),
        size = 56.dp,
        cornerRadius = 16.dp,
        // The glass placeholder is only for an app with no icon URL at all.
        fallbackGradient = gradient.takeIf { iconUrl == null }
    )
}

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

/**
 * The manager's glass placeholder for an app with no icon: a frosted sweep of the app's
 * own palette and a lit edge, rather than a letter tile. Most apps in the archive have no
 * icon URL, so this is what an uninstalled app normally shows.
 */
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

/**
 * One app in the archive, as a manager-style glass card: a tinted gradient, the app icon,
 * name with a source-count tag, a single state line, and the favourite action at the end.
 */
@Composable
private fun FindAppCard(
    app: ArchiveApp,
    installed: Boolean,
    favourite: Boolean,
    onToggleFavourite: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val monochrome = LocalMonochromeTheme.current
    val palette = remember(app.iconColor, app.packageName) { app.findAppCardColors() }
    val scheme = MaterialTheme.colorScheme
    val titleColor = if (monochrome) scheme.onSurface else palette.title
    val subtitleColor = if (monochrome) scheme.onSurfaceVariant else palette.subtitle
    val shape = RoundedCornerShape(24.dp)
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
    val released = remember(app.packageName, app.sources) {
        formatReleaseDate(app.newestReleaseDate())
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 88.dp)
            .clip(shape)
            .drawWithCache {
                val corner = CornerRadius(24.dp.toPx())
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
                val border: Brush = if (monochrome) {
                    SolidColor(scheme.outline.copy(alpha = 0.22f))
                } else {
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.55f),
                            palette.gradient[1].copy(alpha = 0.25f),
                            palette.gradient[2].copy(alpha = 0.14f),
                            Color.White.copy(alpha = 0.18f)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, size.height)
                    )
                }
                onDrawWithContent {
                    drawRoundRect(brush = fill, cornerRadius = corner)
                    drawContent()
                    drawRoundRect(
                        brush = border,
                        cornerRadius = corner,
                        style = Stroke(1.dp.toPx())
                    )
                }
            }
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(
                    color = if (monochrome) {
                        scheme.primary.copy(alpha = 0.18f)
                    } else {
                        Color.White.copy(alpha = 0.28f)
                    }
                ),
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
                // The name gets the whole column. The source count is metadata below it,
                // so a long app name never gives way to a tag on a narrow screen.
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

/** Placeholder shaped and coloured like [FindAppCard] while the archive loads. */
@Composable
private fun FindAppCardLoading(index: Int) {
    val monochrome = LocalMonochromeTheme.current
    val scheme = MaterialTheme.colorScheme
    val palette = FindAppPlaceholderGradients[index % FindAppPlaceholderGradients.size]
    val skeletonColor = if (monochrome) scheme.onSurface else Color.White
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
            .clip(shape)
            .background(
                if (monochrome) {
                    SolidColor(scheme.surfaceContainerHigh)
                } else {
                    Brush.linearGradient(
                        colors = palette.map { it.copy(alpha = 0.5f) },
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
            ShimmerBox(
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(16.dp),
                baseColor = skeletonColor.copy(alpha = 0.24f),
                shimmerColor = skeletonColor.copy(alpha = 0.16f)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                ShimmerLine(
                    height = 18.dp,
                    widthFraction = 0.58f,
                    baseColor = skeletonColor.copy(alpha = 0.30f)
                )
                ShimmerLine(
                    height = 14.dp,
                    widthFraction = 0.82f,
                    baseColor = skeletonColor.copy(alpha = 0.20f)
                )
            }
        }
    }
}

/** Round 40dp title action shared by the browser header and its options panel. */
@Composable
private fun FindAppsHeaderAction(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    marked: Boolean = false,
    enabled: Boolean = true
) {
    val scheme = MaterialTheme.colorScheme
    val contentColor = when {
        !enabled -> scheme.onSurface.copy(alpha = 0.38f)
        selected -> scheme.primary
        else -> scheme.onSurface.copy(alpha = 0.9f)
    }
    val containerColor = if (selected) {
        scheme.primary.copy(alpha = 0.24f)
    } else {
        scheme.surfaceColorAtElevation(2.dp)
    }
    Box(modifier = modifier.size(40.dp)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(containerColor)
                .border(
                    width = 1.dp,
                    color = scheme.primary.copy(alpha = if (selected) 0.52f else 0.22f),
                    shape = CircleShape
                )
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
        }
        if (marked) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 7.dp, end = 7.dp)
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(scheme.primary)
            )
        }
    }
}

/** Compact pill search field, the manager's list search drawn into the header. */
@Composable
private fun FindAppsSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val keyboardController = LocalSoftwareKeyboardController.current
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.height(40.dp),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = scheme.onSurface),
        cursorBrush = SolidColor(scheme.primary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(scheme.surfaceColorAtElevation(2.dp))
                    .border(1.dp, scheme.primary.copy(alpha = 0.28f), CircleShape)
                    .padding(start = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = scheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text(
                            text = stringResource(R.string.search_apps_or_packages),
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant.copy(alpha = 0.5f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    innerTextField()
                }
                if (value.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable(role = Role.Button) { onValueChange("") },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Clear search",
                            tint = scheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    Spacer(Modifier.width(12.dp))
                }
            }
        }
    )
}

/**
 * The manager's docked list options: Show and Sort by as rows, each opening its own menu,
 * with reset and close actions in the panel header.
 */
@Composable
private fun FindAppsOptionsPanel(
    tab: AppListTab,
    sort: AppSort,
    onTabSelect: (AppListTab) -> Unit,
    onSortSelect: (AppSort) -> Unit,
    onReset: () -> Unit,
    onRefresh: () -> Unit,
    onClose: () -> Unit
) {
    SurfaceCard(
        cornerRadius = MorpheDefaults.SectionCornerRadius,
        borderWidth = 1.dp,
        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
    ) {
        Column {
            Row(
                modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "List options",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                FindAppsHeaderAction(
                    icon = Icons.Outlined.History,
                    contentDescription = "Reset list options",
                    onClick = onReset,
                    enabled = tab != AppListTab.All || sort != AppSort.AZ
                )
                FindAppsHeaderAction(
                    icon = Icons.Outlined.Close,
                    contentDescription = "Close list options",
                    onClick = onClose
                )
            }
            MorpheDivider(fullWidth = true)
            FindAppsOptionRow(
                title = "Filter",
                icon = Icons.Outlined.ViewList,
                value = tab,
                options = AppListTab.entries,
                optionLabel = { it.label },
                onSelect = onTabSelect,
                changed = tab != AppListTab.All
            )
            FindAppsOptionRow(
                title = "Sort",
                icon = Icons.Outlined.SortByAlpha,
                value = sort,
                options = AppSort.entries,
                optionLabel = { it.label },
                onSelect = onSortSelect,
                changed = sort != AppSort.AZ
            )
            MorpheDivider(fullWidth = true)
            FindAppsPanelAction(
                title = "Refresh",
                icon = Icons.Outlined.Refresh,
                onClick = {
                    onRefresh()
                    onClose()
                }
            )
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun FindAppsPanelAction(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ThemedIcon(
            icon = icon,
            size = 20.dp,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun <T> FindAppsOptionRow(
    title: String,
    icon: ImageVector,
    value: T,
    options: List<T>,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
    changed: Boolean
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button) { expanded = true }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ThemedIcon(
            icon = icon,
            size = 20.dp,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = optionLabel(value),
            style = MaterialTheme.typography.bodyMedium,
            color = if (changed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Box {
            Icon(
                imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(optionLabel(option)) },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        },
                        leadingIcon = if (option == value) {
                            {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else {
                            null
                        }
                    )
                }
            }
        }
    }
}

/** Decoded archive icons, bounded so a long scroll cannot fill the heap. */
private const val ARCHIVE_ICON_CACHE_BYTES = 12 * 1024 * 1024

private val archiveIconCache = object : LruCache<String, ImageBitmap>(ARCHIVE_ICON_CACHE_BYTES) {
    override fun sizeOf(key: String, value: ImageBitmap): Int =
        value.width * value.height * 4
}

/** Icon fetch that is cancelled when the row leaves the viewport. */
private suspend fun fetchArchiveIcon(iconUrl: String): ImageBitmap? {
    // The archive is third-party data, so a malformed icon URL must only leave
    // the row on its letter tile rather than fail the composition.
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
                        it.body.bytes().let { bytes ->
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        }?.asImageBitmap()
                    }
                }.getOrNull()
                if (continuation.isActive) continuation.resume(bitmap)
            }
        })
    }
}

/**
 * Small async image loader for source avatars: fetches the URL off the main
 * thread and falls back to a letter tile while loading / on failure.
 *
 * @param fallbackGradient When set, the letter tile is drawn over this gradient,
 *   so an app card's missing icon still belongs to its own palette.
 */
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
        // A fling composes rows faster than icons can load. Let the row settle
        // first so a fast scroll does not queue an image for every card that
        // passes through the viewport.
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

/**
 * Opens a Morphe "add-source" deep link. The manager's intent filter for
 * https://morphe.software/add-source has no autoVerify, and on Android 12+
 * unverified https apps are hidden from the implicit resolver (so the browser
 * would win). Pinning the package restores the direct handoff to Morphe.
 */
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
 * Manager-style hero for an archive app: its own tinted glass, icon, identity and
 * the totals that describe the catalogue entry.
 */
@Composable
private fun AppDetailHero(
    app: ArchiveApp,
    installed: Boolean,
    onOpenPlayStore: () -> Unit
) {
    val monochrome = LocalMonochromeTheme.current
    val palette = remember(app.iconColor, app.packageName) { app.findAppCardColors() }
    val scheme = MaterialTheme.colorScheme
    val titleColor = if (monochrome) scheme.onSurface else palette.title
    val subtitleColor = if (monochrome) scheme.onSurfaceVariant else palette.subtitle
    val badgeContainer = if (monochrome) scheme.surfaceVariant else Color.White.copy(alpha = 0.14f)
    val badgeContent = if (monochrome) scheme.onSurfaceVariant else Color.White.copy(alpha = 0.92f)
    val shape = RoundedCornerShape(24.dp)
    val released = formatReleaseDate(app.newestReleaseDate())

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .drawWithCache {
                val corner = CornerRadius(24.dp.toPx())
                val fill: Brush = if (monochrome) {
                    SolidColor(scheme.surfaceContainerHigh)
                } else {
                    Brush.linearGradient(
                        colors = listOf(
                            palette.gradient[0].copy(alpha = 0.94f),
                            palette.gradient[1].copy(alpha = 0.78f),
                            palette.gradient[2].copy(alpha = 0.90f)
                        ),
                        start = Offset(0f, size.height),
                        end = Offset(size.width, 0f)
                    )
                }
                val border: Brush = if (monochrome) {
                    SolidColor(scheme.outline.copy(alpha = 0.22f))
                } else {
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.55f),
                            palette.gradient[1].copy(alpha = 0.25f),
                            Color.White.copy(alpha = 0.18f)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, size.height)
                    )
                }
                onDrawBehind {
                    drawRoundRect(brush = fill, cornerRadius = corner)
                    drawRoundRect(
                        brush = border,
                        cornerRadius = corner,
                        style = Stroke(1.dp.toPx())
                    )
                }
            }
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FindAppIcon(app = app, gradient = palette.gradient, installed = installed)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = app.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = titleColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = subtitleColor.copy(alpha = 0.82f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (monochrome) scheme.surfaceVariant else Color.White.copy(alpha = 0.16f)
                        )
                        .clickable(role = Role.Button, onClick = onOpenPlayStore),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Storefront,
                        contentDescription = stringResource(R.string.open_in_play_store),
                        tint = titleColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            MorpheStatusBadgeRow {
                MorpheStatusBadge(
                    text = "${app.sourceCount} " +
                        if (app.sourceCount == 1) "source" else "sources",
                    icon = Icons.Outlined.Dns,
                    containerColor = badgeContainer,
                    contentColor = badgeContent
                )
                if (app.versions.isNotEmpty()) {
                    MorpheStatusBadge(
                        text = "${app.versions.size} " +
                            if (app.versions.size == 1) "version" else "versions",
                        icon = Icons.Outlined.History,
                        containerColor = badgeContainer,
                        contentColor = badgeContent
                    )
                }
                released?.let { date ->
                    MorpheStatusBadge(
                        text = "Updated $date",
                        containerColor = badgeContainer,
                        contentColor = badgeContent
                    )
                }
            }
        }
    }
}

/** Horizontally scrollable version pills, instead of one long comma-separated line. */
@Composable
private fun AppVersionStrip(versions: List<String>) {
    SurfaceCard(
        cornerRadius = MorpheDefaults.SettingsCornerRadius,
        borderWidth = 1.dp,
        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            versions.forEach { version ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = version,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
internal fun AppDetailView(
    app: ArchiveApp,
    installed: Boolean = false,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val openUrl: (String) -> Unit = { url ->
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }
    // Forks re-release their upstream's patches under their own name, so the same
    // set can arrive from several repos. Collapsing them keeps a mirror from
    // reading as an independent choice.
    val sourceGroups = remember(app.sources) { groupArchiveSources(app.sources) }
    val appPalette = remember(app.iconColor, app.packageName) { app.findAppCardColors() }
    val sourceAccent = if (LocalMonochromeTheme.current) {
        MaterialTheme.colorScheme.primary
    } else {
        appPalette.gradient[1]
    }
    val detailListState = rememberLazyListState()
    LazyColumn(
        state = detailListState,
        modifier = modifier
            .fillMaxWidth()
            .verticalScrollFade(detailListState),
        verticalArrangement = Arrangement.spacedBy(MorpheDefaults.ItemSpacing)
    ) {
        item {
            AppDetailHero(
                app = app,
                installed = installed,
                onOpenPlayStore = {
                    context.openPlayStoreListing(app.packageName, playStoreUrl(app.packageName))
                }
            )
        }
        if (app.versions.isNotEmpty()) {
            item {
                MorpheSectionTitle(text = "Versions", icon = Icons.Outlined.History)
                Spacer(Modifier.height(8.dp))
                AppVersionStrip(app.versions)
            }
        }
        if (sourceGroups.isEmpty()) {
            item { MorpheEmptyState(message = "No patch sources listed for this app.") }
        } else {
            item {
                MorpheSectionTitle(text = "Sources", icon = Icons.Outlined.Dns)
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
                    onAddToMorphe = { openAddSource(context, it) }
                )
            }
        }
    }
}

/**
 * One patch set, led by the repo that ships it, with its own collapsible patch
 * list (collapsed by default) so patches are clearly attributed to the repo that
 * provides them. The Add-to-Morphe / Open-repo actions stay visible whether or
 * not it's expanded.
 *
 * [mirrors] are the other repos offering the identical set. They stay reachable
 * with their own actions, but folded away, since listing them as equals would
 * present a fork as its own choice.
 */
@Composable
internal fun AppSourceCard(
    source: ArchiveSource,
    accentColor: Color,
    onOpenUrl: (String) -> Unit,
    onAddToMorphe: (String) -> Unit,
    mirrors: List<ArchiveSource> = emptyList()
) {
    var expanded by remember(source.repo) { mutableStateOf(false) }
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
            modifier = Modifier.padding(vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 16.dp),
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
                        // The release is what ranks this card against the mirrors, so it
                        // is shown rather than left for the user to infer from the order.
                        formatBundleRelease(source.latestChanges)?.let { release ->
                            MorpheStatusBadge(
                                text = release,
                                icon = Icons.Outlined.History
                            )
                        }
                    }
                    if (mirrors.isNotEmpty()) {
                        Text(
                            text = "Same patches in ${mirrors.size} other " +
                                (if (mirrors.size == 1) "repo" else "repos"),
                            color = colors.onSurfaceVariant.copy(alpha = 0.75f),
                            style = MaterialTheme.typography.labelSmall,
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
                    .padding(horizontal = 16.dp),
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
                    modifier = Modifier.padding(horizontal = 16.dp),
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
                        MorpheDivider(fullWidth = true)
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
                                            if (longList) {
                                                Modifier
                                                    .verticalScrollFade(patchesScroll)
                                                    .verticalScroll(patchesScroll)
                                            } else Modifier
                                        )
                                ) {
                                    source.patches.forEachIndexed { index, patch ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 9.dp),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(accentColor.copy(alpha = 0.14f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = (index + 1).toString(),
                                                    color = accentColor,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                verticalArrangement = Arrangement.spacedBy(3.dp)
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
                                                        color = colors.onSurfaceVariant.copy(alpha = 0.92f),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        lineHeight = 17.sp
                                                    )
                                                }
                                            }
                                        }
                                        if (index < source.patches.lastIndex) {
                                            MorpheDivider(
                                                modifier = Modifier.padding(start = 34.dp),
                                                fullWidth = true
                                            )
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
                        // Each keeps its own Add / Open actions, so folding the
                        // mirrors away costs the ability to see them, not to use them.
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
                                    // The lead is chosen by release date, so a mirror has to
                                    // show its own for the order to be checkable rather than
                                    // taken on faith.
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





