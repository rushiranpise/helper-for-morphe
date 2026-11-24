package dev.rushi.apkdownloadhelper.ui.shared.appcard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.rushi.apkdownloadhelper.R
import dev.rushi.apkdownloadhelper.ui.shared.layout.AnimatedExpand
import dev.rushi.apkdownloadhelper.ui.shared.shape.rememberPressShape
import dev.rushi.apkdownloadhelper.ui.shared.version.ExpandableVersionRow
import dev.rushi.apkdownloadhelper.ui.theme.SemanticTone
import dev.rushi.apkdownloadhelper.ui.theme.pressScale
import dev.rushi.apkdownloadhelper.updater.ArchiveSource
import dev.rushi.apkdownloadhelper.updater.MorpheArchive
import dev.rushi.apkdownloadhelper.updater.bundleVersionOf
import dev.rushi.apkdownloadhelper.updater.formatReleaseDate
import dev.rushi.apkdownloadhelper.updater.releaseDateOf

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
    var liveVersionsUnavailable by remember(source.repo, packageName) {
        mutableStateOf(false)
    }
    LaunchedEffect(source.repo, packageName) {
        val fetched = MorpheArchive.fetchPatchVersions(source, packageName)
        liveVersionsUnavailable = fetched == null
        liveVersions = fetched.orEmpty()
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
        val patchNames = source.patches.map { it.name.trim() }.toSet()
        val fromLive = liveVersions.orEmpty()
            .filterKeys { it.trim() in patchNames }
            .values
            .flatten()
        val fromPatchIndex = source.patches.flatMap { it.versions }
        (fromLive + fromPatchIndex)
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
                supportedAppVersions.isNotEmpty() -> supportedAppVersions
                liveVersions == null -> listOf("Loading…")
                liveVersionsUnavailable -> listOf("Unavailable")
                else -> listOf("Any version")
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
                    val pillInteraction = remember { MutableInteractionSource() }
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
                            .height(42.dp)
                            .pressScale(pillInteraction, pressedScale = 0.96f),
                        shape = rememberPressShape(pillInteraction, pillShape),
                        color = containerColor,
                        interactionSource = pillInteraction,
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
                                var liveMirrorVersions by remember(mirror.repo, packageName) {
                                    mutableStateOf<Map<String, List<String>>?>(null)
                                }
                                var mirrorVersionsUnavailable by remember(mirror.repo, packageName) {
                                    mutableStateOf(false)
                                }
                                LaunchedEffect(mirror.repo, packageName) {
                                    val fetched = MorpheArchive.fetchPatchVersions(mirror, packageName)
                                    mirrorVersionsUnavailable = fetched == null
                                    liveMirrorVersions = fetched.orEmpty()
                                }
                                val mirrorSupportedVersions = remember(mirror.patches, liveMirrorVersions) {
                                    val patchNames = mirror.patches.map { it.name.trim() }.toSet()
                                    val fromLive = liveMirrorVersions.orEmpty()
                                        .filterKeys { it.trim() in patchNames }
                                        .values
                                        .flatten()
                                    val fromPatchIndex = mirror.patches.flatMap { it.versions }
                                    (fromLive + fromPatchIndex)
                                        .map { it.trim() }
                                        .filter { it.isNotEmpty() && !it.equals("null", ignoreCase = true) }
                                        .distinct()
                                }
                                val mirrorVersionsToDisplay = when {
                                    mirrorSupportedVersions.isNotEmpty() -> mirrorSupportedVersions
                                    liveMirrorVersions == null -> listOf("Loading…")
                                    mirrorVersionsUnavailable && supportedAppVersions.isNotEmpty() ->
                                        supportedAppVersions
                                    mirrorVersionsUnavailable -> listOf("Unavailable")
                                    else -> listOf("Any version")
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
                                            val pillInteraction = remember { MutableInteractionSource() }
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
                                                    .height(34.dp)
                                                    .pressScale(pillInteraction, pressedScale = 0.96f),
                                                shape = rememberPressShape(pillInteraction, pillShape),
                                                color = containerColor,
                                                interactionSource = pillInteraction,
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
