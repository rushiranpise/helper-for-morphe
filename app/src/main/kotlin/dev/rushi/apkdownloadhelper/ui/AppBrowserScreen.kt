package dev.rushi.apkdownloadhelper.ui

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CheckCircle
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
import androidx.compose.material.icons.outlined.SortByAlpha
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
import kotlinx.coroutines.CancellationException
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
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Top
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
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
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
    // When the archive built the index, so a build that predates a source's own
    // changes is visible rather than silently misleading.
    var freshness by remember { mutableStateOf<ArchiveFreshness?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<ArchiveApp?>(null) }
    var loadKey by remember { mutableIntStateOf(0) }
    var tab by remember { mutableStateOf(AppListTab.All) }
    var sort by remember { mutableStateOf(AppSort.AZ) }
    val context = LocalContext.current
    var favourites by remember { mutableStateOf<Set<String>>(emptySet()) }
    var installedPackages by remember { mutableStateOf<Set<String>>(emptySet()) }
    // Hoisted above the list/detail branch so the scroll position survives
    // opening an app's details and coming back — remember inside the list
    // branch would be discarded when the detail view replaces the list.
    val listState = rememberLazyListState()
    // Read as a boolean rather than the raw scroll offset, so scrolling recomposes
    // the chrome once per crossing instead of once per scrolled pixel.
    val listAtTop by remember(listState) {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
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
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message ?: "Failed to load the app index"
        }
    }

    // System back (including the TalkBack back gesture) should first leave the
    // app detail back to the list; only from the list does it close the browser.
    BackHandler(enabled = selected != null) {
        selected = null
    }

    // Search is the one control reachable while the selector rows are collapsed, so
    // filtering from deep in the list would otherwise leave the results mid-scroll
    // with the controls still hidden. Jumping to the top brings them back, and also
    // puts the best matches where the eye already is.
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
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = stringResource(R.string.find_new_apps),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                // No app name here when a detail is open: the card below already
                // headings with it, so it would read as the same name twice.
                if (selected == null) {
                    val fresh = freshness
                    Text(
                        text = buildString {
                            append(apps?.let { "${it.size} apps with patches" } ?: "Morphe patch archive")
                            // Age goes in the subtitle because a stale index is otherwise
                            // indistinguishable from a current one.
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
            }
            HelperHeaderIconButton(
                icon = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = stringResource(R.string.back),
                onClick = {
                    if (selected != null) selected = null else onBack?.invoke()
                },
                modifier = Modifier.align(Alignment.CenterStart)
            )
            if (selected == null) {
                HelperHeaderIconButton(
                    icon = Icons.Outlined.Refresh,
                    contentDescription = stringResource(R.string.refresh),
                    onClick = { loadKey++ },
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }
        }

        val current = selected
        if (current != null) {
            AppDetailView(
                app = current,
                onBack = { selected = null },
                modifier = Modifier.weight(1f)
            )
        } else {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                placeholder = {
                    Text(
                        stringResource(R.string.search_apps_or_packages),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null
                    )
                }
            )

            val loaded = apps
            when {
                error != null -> {
                    InfoCard(error!!)
                    HelperButton(
                        text = "Retry",
                        onClick = { loadKey++ },
                        icon = Icons.Outlined.Refresh,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                loaded == null -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
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
                    val filtered = loaded
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
                                // Keyed once per app and then sorted, rather than
                                // recomputing the newest release on every comparison.
                                AppSort.Newest -> matches
                                    .map { app -> app.newestReleaseDate().orEmpty() to app }
                                    .sortedByDescending { it.first }
                                    .map { it.second }
                            }
                        }
                    // The disclaimer and the two selector rows are the tallest thing on
                    // this screen, so they step aside once the list is scrolled and come
                    // back at the top, which the "Go to top" button reaches in one tap.
                    // An empty result keeps them regardless: with nothing left to scroll
                    // there would be no way back to the controls.
                    val chromeVisible = filtered.isEmpty() || listAtTop
                    AnimatedVisibility(
                        visible = chromeVisible,
                        enter = MorpheAnimations.expandFadeEnter,
                        exit = MorpheAnimations.shrinkFadeExit
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(MorpheDefaults.ItemSpacing)
                        ) {
                            AppDisclaimerBanner()
                            // The same selector row the version-type and settings tabs use,
                            // so the browser has no pill of its own to drift out of sync.
                            MorpheSelectorRow(
                                options = AppListTab.entries.map {
                                    MorpheSelectorOption(
                                        label = it.label,
                                        icon = it.icon,
                                        contentDescription = it.contentDescription
                                    )
                                },
                                selectedIndex = AppListTab.entries.indexOf(tab),
                                onSelect = { tab = AppListTab.entries[it] },
                                // Four labels across, so use the smaller step.
                                labelStyle = MaterialTheme.typography.labelMedium
                            )
                            MorpheSelectorRow(
                                options = AppSort.entries.map {
                                    MorpheSelectorOption(
                                        label = it.label,
                                        icon = it.icon,
                                        iconRotation = it.rotation
                                    )
                                },
                                selectedIndex = AppSort.entries.indexOf(sort),
                                onSelect = { sort = AppSort.entries[it] },
                                labelStyle = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                    if (filtered.isEmpty()) {
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
                    } else {
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
                                    items(filtered, key = { it.packageName }) { app ->
                                        AppBrowserRow(
                                            app = app,
                                            favourite = app.packageName in favourites,
                                            onToggleFavourite = {
                                                favourites = MorpheFavourites.toggle(context, app.packageName)
                                            },
                                            onClick = { selected = app },
                                            // Rows settle into their new places with a spring, so
                                            // re-sorting or filtering glides instead of jumping.
                                            modifier = Modifier.animatedListItem(this)
                                        )
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
}

@Composable
internal fun AppBrowserRow(
    app: ArchiveApp,
    favourite: Boolean,
    onToggleFavourite: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MorpheDefaults.SettingsCornerRadius))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(MorpheDefaults.SettingsCornerRadius),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(start = MorpheDefaults.ContentPadding, end = 6.dp, top = 10.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppAvatar(
                packageName = app.packageName,
                initial = app.name.firstOrNull()?.uppercaseChar() ?: '?'
            )
            Column(modifier = Modifier.weight(1f)) {
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
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                // When the sources last published patches for this app, so an index
                // row that has fallen behind shows it before the app is opened.
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
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (app.sourceCount == 1) "source" else "sources",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
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
}

/**
 * Small async image loader for source avatars: fetches the URL off the main
 * thread and falls back to a letter tile while loading / on failure.
 */
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

@Composable
internal fun AppDetailView(
    app: ArchiveApp,
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
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
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
                    // Open the Play Store listing, same as the Play source's action, so
                    // users can see the official page for the app.
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
            items(
                items = sourceGroups,
                key = { group ->
                    group.primary.patches.map { it.name }.sorted().joinToString("\u0000")
                }
            ) { group ->
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
                    // The release is what ranks this card against the mirrors, so it
                    // is shown rather than left for the user to infer from the order.
                    Text(
                        text = buildString {
                            append("${source.patches.size} ")
                            append(if (source.patches.size == 1) "patch" else "patches")
                            formatBundleRelease(source.latestChanges)?.let { release ->
                                append(" \u00b7 ")
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





