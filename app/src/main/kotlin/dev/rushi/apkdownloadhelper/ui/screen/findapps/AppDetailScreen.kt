package dev.rushi.apkdownloadhelper.ui.screen.findapps

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import dev.chrisbanes.haze.HazeState
import dev.rushi.apkdownloadhelper.R
import dev.rushi.apkdownloadhelper.download.openPlayStoreListing
import dev.rushi.apkdownloadhelper.download.playStoreUrl
import dev.rushi.apkdownloadhelper.ui.shared.appcard.AppSourceCard
import dev.rushi.apkdownloadhelper.ui.shared.appcard.FindAppIcon
import dev.rushi.apkdownloadhelper.ui.shared.appcard.findAppCardColors
import dev.rushi.apkdownloadhelper.ui.shared.button.BottomCloseBar
import dev.rushi.apkdownloadhelper.ui.shared.chip.StatusBadge
import dev.rushi.apkdownloadhelper.ui.shared.layout.PushedScreen
import dev.rushi.apkdownloadhelper.ui.shared.state.EmptyState
import dev.rushi.apkdownloadhelper.ui.theme.AppAnimations
import dev.rushi.apkdownloadhelper.ui.theme.AppColors
import dev.rushi.apkdownloadhelper.ui.theme.AppDefaults
import dev.rushi.apkdownloadhelper.ui.theme.hazeSourceIfEnabled
import dev.rushi.apkdownloadhelper.ui.theme.LocalMonochromeTheme
import dev.rushi.apkdownloadhelper.ui.theme.glassFill
import dev.rushi.apkdownloadhelper.ui.theme.pressScale
import dev.rushi.apkdownloadhelper.ui.theme.rememberMorphePillShape
import dev.rushi.apkdownloadhelper.ui.util.openAddSource
import dev.rushi.apkdownloadhelper.updater.ArchiveApp
import dev.rushi.apkdownloadhelper.updater.formatReleaseDate
import dev.rushi.apkdownloadhelper.updater.groupArchiveSources
import dev.rushi.apkdownloadhelper.updater.newestReleaseDate
import kotlin.math.roundToInt

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
internal fun AppDetailScreen(
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
                .hazeSourceIfEnabled(detailHeaderHaze),
            contentPadding = PaddingValues(
                top = unfoldedHeaderHeightDp + 12.dp,
                start = AppDefaults.ContentPadding,
                end = AppDefaults.ContentPadding,
                bottom = footerHeightDp + 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(AppDefaults.ItemSpacing)
        ) {
            // Sources section
            if (sourceGroups.isEmpty()) {
                item(key = "no_sources") {
                    EmptyState(message = "No patch sources listed for this app.")
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
                    glass = AppColors.glassContainerColor(),
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
                        horizontal = AppDefaults.ContentPadding,
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
                        StatusBadge(
                            text = "${app.sourceCount} " +
                                    if (app.sourceCount == 1) "source" else "sources",
                            icon = Icons.Outlined.Dns,
                            containerColor = badgeFill,
                            contentColor = scheme.onSurface
                        )
                        if (app.versions.isNotEmpty()) {
                            StatusBadge(
                                text = "${app.versions.size} " +
                                        if (app.versions.size == 1) "version" else "versions",
                                icon = Icons.Outlined.History,
                                containerColor = badgeFill,
                                contentColor = scheme.onSurface
                            )
                        }
                        released?.let { date ->
                            StatusBadge(
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

        // Telegram-style bottom dim scrim
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
        val closeShape = rememberMorphePillShape(closeInteraction)
        val playInteraction = remember { MutableInteractionSource() }
        val playShape = rememberMorphePillShape(playInteraction)

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
                    interactionSource = playInteraction,
                    modifier = Modifier
                        .weight(if (onBack != null) 1.4f else 1f)
                        .heightIn(min = 48.dp)
                        .pressScale(playInteraction, pressedScale = 0.96f)
                        .glassFill(
                            glass = scheme.surfaceVariant.copy(alpha = 0.65f),
                            shape = playShape,
                            hazeState = detailHeaderHaze
                        ),
                    shape = playShape,
                    color = Color.Transparent,
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
                    BottomCloseBar(
                        onClose = onBack,
                        hazeState = detailHeaderHaze,
                        showBackground = false,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 4. Smooth bottom-to-top animated Patches overlay
        AnimatedVisibility(
            visible = activePatchGroup != null,
            enter = AppAnimations.pushEnter,
            exit = AppAnimations.pushExit
        ) {
            val group = lastPatchGroup
            if (group != null) {
                PushedScreen {
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
