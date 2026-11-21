package dev.rushi.apkdownloadhelper.ui.screen.findapps

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.rushi.apkdownloadhelper.R
import dev.rushi.apkdownloadhelper.ui.shared.button.GlassHeaderButton
import dev.rushi.apkdownloadhelper.ui.shared.card.InfoCard
import dev.rushi.apkdownloadhelper.ui.shared.input.SearchField
import dev.rushi.apkdownloadhelper.ui.shared.version.ExpandableVersionRow
import dev.rushi.apkdownloadhelper.ui.theme.AppColors
import dev.rushi.apkdownloadhelper.ui.theme.AppDefaults
import dev.rushi.apkdownloadhelper.ui.theme.LocalMonochromeTheme
import dev.rushi.apkdownloadhelper.ui.theme.glassFill
import dev.rushi.apkdownloadhelper.ui.theme.pressScale
import dev.rushi.apkdownloadhelper.updater.ArchiveSource
import dev.rushi.apkdownloadhelper.updater.MorpheArchive

@Composable
internal fun SourcePatchesScreen(
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
    var liveVersionsUnavailable by remember(source.repo, packageName) {
        mutableStateOf(false)
    }
    LaunchedEffect(source.repo, packageName) {
        val fetched = MorpheArchive.fetchPatchVersions(source, packageName)
        liveVersionsUnavailable = fetched == null
        liveVersions = fetched.orEmpty()
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
                start = AppDefaults.ContentPadding,
                end = AppDefaults.ContentPadding,
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
                                !resolvedVersions.isNullOrEmpty() -> resolvedVersions.orEmpty()
                                liveVersions == null -> listOf("Loading…")
                                liveVersionsUnavailable -> listOf("Unavailable")
                                else -> listOf("Any version")
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
                                    if (ver == "Any version" || ver == "Loading…" || ver == "Unavailable") {
                                        colors.surfaceVariant.copy(alpha = 0.45f)
                                    } else {
                                        colors.primaryContainer
                                    }
                                },
                                contentColorFor = { ver ->
                                    if (ver == "Any version" || ver == "Loading…" || ver == "Unavailable") {
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
                        SearchField(
                            query = patchQuery,
                            onQueryChange = { patchQuery = it },
                            placeholder = "Search patches or versions",
                            focusRequester = searchFocusRequester,
                            modifier = Modifier.weight(1f)
                        )

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

        // Pinned Bottom "Close" Frosted Outlined Pill (matching AppDetailScreen)
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
                    horizontal = AppDefaults.ContentPadding,
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
