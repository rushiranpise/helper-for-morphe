package dev.rushi.apkdownloadhelper.ui.screen.settings

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material3.*
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.rushi.apkdownloadhelper.R
import dev.rushi.apkdownloadhelper.storage.*
import dev.rushi.apkdownloadhelper.ui.shared.dialog.SettingsChoice
import dev.rushi.apkdownloadhelper.ui.shared.dialog.SettingsChoiceDialog
import dev.rushi.apkdownloadhelper.ui.shared.settings.SettingsGroup
import dev.rushi.apkdownloadhelper.ui.shared.settings.SettingsItem
import dev.rushi.apkdownloadhelper.ui.shared.settings.SettingsSection
import dev.rushi.apkdownloadhelper.ui.shared.settings.SettingsValueText
import dev.rushi.apkdownloadhelper.ui.shared.source.SourceAvatar
import dev.rushi.apkdownloadhelper.ui.model.DownloadSource
import dev.rushi.apkdownloadhelper.ui.theme.AppDefaults
import kotlinx.coroutines.delay

// ==========================================
// SHARED COMPONENTS
// ==========================================
@Composable
internal fun SourcesSettingsScreen(
    settings: HelperSettings,
    onSettingsChange: (HelperSettings) -> Unit,
    parentScrollState: ScrollState? = null
) {
    var sourceDialog by remember { mutableStateOf(false) }
    val latestSettings by rememberUpdatedState(settings)
    val haptics = LocalHapticFeedback.current
    val view = LocalView.current

    val localOrderState = remember(settings.sourceOrder) {
        mutableStateOf(settings.orderedSources)
    }
    var localOrder by localOrderState
    var draggingSource by remember { mutableStateOf<DownloadSource?>(null) }
    var startIndex by remember { mutableIntStateOf(-1) }
    var targetIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var fingerRootY by remember { mutableFloatStateOf(0f) }
    val rowHeights = remember { mutableStateMapOf<DownloadSource, Int>() }

    fun updateTargetIndex() {
        if (startIndex !in localOrder.indices || localOrder.isEmpty()) return
        val draggedHeight = (rowHeights[localOrder[startIndex]] ?: 180).toFloat()
        var accumulated = 0f
        var newTarget = startIndex

        if (dragOffsetY > 0f) {
            for (i in (startIndex + 1)..localOrder.lastIndex) {
                val h = (rowHeights[localOrder[i]] ?: 180).toFloat()
                if (dragOffsetY > accumulated + h * 0.5f) {
                    accumulated += h
                    newTarget = i
                } else break
            }
        } else if (dragOffsetY < 0f) {
            for (i in (startIndex - 1) downTo 0) {
                val h = (rowHeights[localOrder[i]] ?: 180).toFloat()
                if (-dragOffsetY > accumulated + h * 0.5f) {
                    accumulated += h
                    newTarget = i
                } else break
            }
        }

        if (newTarget != targetIndex) {
            targetIndex = newTarget
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    // Auto-scroll loop when dragging near screen edges
    LaunchedEffect(draggingSource, parentScrollState) {
        val activeSource = draggingSource ?: return@LaunchedEffect
        val scroll = parentScrollState ?: return@LaunchedEffect
        val screenHeight = view.height.toFloat().coerceAtLeast(1f)
        val topThreshold = screenHeight * 0.22f
        val bottomThreshold = screenHeight * 0.82f

        while (draggingSource == activeSource) {
            delay(16L)
            val y = fingerRootY
            val delta = when {
                y < topThreshold && scroll.canScrollBackward -> {
                    val ratio = ((topThreshold - y) / topThreshold).coerceIn(0.25f, 1.2f)
                    -20f * ratio
                }
                y > bottomThreshold && scroll.canScrollForward -> {
                    val ratio = ((y - bottomThreshold) / (screenHeight - bottomThreshold)).coerceIn(0.25f, 1.2f)
                    20f * ratio
                }
                else -> 0f
            }

            if (delta != 0f && startIndex in localOrder.indices) {
                val consumed = scroll.scrollBy(delta)
                if (consumed != 0f) {
                    val maxUp = -localOrder.take(startIndex).sumOf { rowHeights[it] ?: 180 }.toFloat()
                    val maxDown = localOrder.drop(startIndex + 1).sumOf { rowHeights[it] ?: 180 }.toFloat()
                    dragOffsetY = (dragOffsetY + consumed).coerceIn(maxUp, maxDown)
                    updateTargetIndex()
                }
            }
        }
    }

    if (sourceDialog) {
        val choices = buildList {
            add(SettingsChoice(Icons.Outlined.AutoAwesome, "Automatic", "Use the first enabled source in the custom order"))
            addAll(localOrder.map { SettingsChoice(icon = null, title = it.label, description = "Prefer this source when it has the file", source = it) })
        }
        SettingsChoiceDialog(
            title = stringResource(R.string.default_source),
            choices = choices,
            selectedIndex = settings.preferredSource?.let { source -> localOrder.indexOf(source) + 1 }?.takeIf { it > 0 } ?: 0,
            onSelect = { index ->
                onSettingsChange(settings.copy(preferredSource = if (index == 0) null else localOrder[index - 1]))
                sourceDialog = false
            },
            onDismiss = { sourceDialog = false }
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SettingsSection(title = stringResource(R.string.default_source), icon = Icons.Outlined.Star) {
            SettingsGroup {
                SettingsItem(
                    icon = Icons.Outlined.Star,
                    title = stringResource(R.string.default_source),
                    subtitle = "Used first when a request is resolved",
                    onClick = { sourceDialog = true },
                    trailingContent = { SettingsValueText(settings.preferredSource?.label ?: "Automatic") }
                )
            }
        }

        SettingsSection(title = "Providers & priority", icon = Icons.Outlined.Dns) {
            SettingsGroup {
                val draggedHeight = if (startIndex in localOrder.indices) {
                    (rowHeights[localOrder[startIndex]] ?: 180).toFloat()
                } else 0f

                localOrder.forEachIndexed { index, source ->
                    key(source) {
                        val enabled = source !in settings.disabledSources
                        val isDragging = draggingSource == source

                        val targetShiftY = when {
                            isDragging || startIndex !in localOrder.indices || targetIndex !in localOrder.indices -> 0f
                            startIndex < targetIndex && index in (startIndex + 1)..targetIndex -> -draggedHeight
                            startIndex > targetIndex && index in targetIndex until startIndex -> draggedHeight
                            else -> 0f
                        }
                        val animatedShiftY by animateFloatAsState(
                            targetValue = targetShiftY,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "neighbor_shift"
                        )

                        val scale by animateFloatAsState(
                            targetValue = if (isDragging) 1.03f else 1f,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "drag_scale"
                        )
                        val elevation by animateDpAsState(
                            targetValue = if (isDragging) 10.dp else 0.dp,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "drag_elevation"
                        )
                        val rowFill = if (isDragging) {
                            MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
                        } else {
                            Color.Transparent
                        }

                        var handleRootY by remember { mutableFloatStateOf(0f) }

                        Box(
                            modifier = Modifier
                                .zIndex(if (isDragging) 10f else 0f)
                                .onSizeChanged { rowHeights[source] = it.height }
                                .graphicsLayer {
                                    translationY = if (isDragging) dragOffsetY else animatedShiftY
                                    scaleX = scale
                                    scaleY = scale
                                    shadowElevation = elevation.toPx()
                                    shape = RoundedCornerShape(16.dp)
                                    clip = isDragging
                                }
                                .background(
                                    color = rowFill,
                                    shape = if (isDragging) RoundedCornerShape(16.dp) else RoundedCornerShape(0.dp)
                                )
                        ) {
                            SettingsItem(
                                leadingContent = { SourceAvatar(source = source, size = AppDefaults.IconSize) },
                                title = source.label,
                                subtitle = when {
                                    !enabled -> "Disabled"
                                    index == 0 && draggingSource == null -> "Tried first"
                                    else -> null
                                },
                                onClick = {
                                    onSettingsChange(
                                        settings.copy(
                                            disabledSources = if (enabled) settings.disabledSources + source else settings.disabledSources - source
                                        )
                                    )
                                },
                                trailingContent = {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Switch(
                                            checked = enabled,
                                            onCheckedChange = { isChecked ->
                                                onSettingsChange(
                                                    settings.copy(
                                                        disabledSources = if (isChecked) settings.disabledSources - source else settings.disabledSources + source
                                                    )
                                                )
                                            },
                                            modifier = Modifier.semantics {
                                                stateDescription = if (enabled) "Enabled" else "Disabled"
                                            },
                                            thumbContent = {
                                                Icon(
                                                    imageVector = if (enabled) Icons.Outlined.Check else Icons.Outlined.Close,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                                )
                                            }
                                        )
                                        Icon(
                                            imageVector = Icons.Outlined.DragHandle,
                                            contentDescription = "Hold and drag to reorder ${source.label}",
                                            tint = if (isDragging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier
                                                .size(28.dp)
                                                .onGloballyPositioned { coords ->
                                                    handleRootY = coords.positionInRoot().y + coords.size.height / 2f
                                                }
                                                .pointerInput(source, localOrderState) {
                                                    detectDragGesturesAfterLongPress(
                                                        onDragStart = {
                                                            val idx = localOrder.indexOf(source)
                                                            if (idx != -1) {
                                                                draggingSource = source
                                                                startIndex = idx
                                                                targetIndex = idx
                                                                dragOffsetY = 0f
                                                                fingerRootY = handleRootY
                                                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                            }
                                                        },
                                                        onDrag = { change, dragAmount ->
                                                            change.consume()
                                                            if (startIndex in localOrder.indices) {
                                                                fingerRootY += dragAmount.y
                                                                val maxUp = -localOrder.take(startIndex).sumOf { rowHeights[it] ?: 180 }.toFloat()
                                                                val maxDown = localOrder.drop(startIndex + 1).sumOf { rowHeights[it] ?: 180 }.toFloat()
                                                                dragOffsetY = (dragOffsetY + dragAmount.y).coerceIn(maxUp, maxDown)
                                                                updateTargetIndex()
                                                            }
                                                        },
                                                        onDragEnd = {
                                                            if (startIndex in localOrder.indices && targetIndex in localOrder.indices && startIndex != targetIndex) {
                                                                val updated = localOrder.toMutableList().apply {
                                                                    val item = removeAt(startIndex)
                                                                    add(targetIndex, item)
                                                                }
                                                                localOrder = updated
                                                                onSettingsChange(latestSettings.copy(sourceOrder = updated))
                                                            }
                                                            draggingSource = null
                                                            startIndex = -1
                                                            targetIndex = -1
                                                            dragOffsetY = 0f
                                                        },
                                                        onDragCancel = {
                                                            draggingSource = null
                                                            startIndex = -1
                                                            targetIndex = -1
                                                            dragOffsetY = 0f
                                                        }
                                                    )
                                                }
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
                SettingsItem(
                    icon = Icons.Outlined.Refresh,
                    title = "Reset source order",
                    subtitle = "Restore default priority.",
                    onClick = {
                        localOrder = DEFAULT_SOURCE_ORDER
                        onSettingsChange(latestSettings.copy(sourceOrder = DEFAULT_SOURCE_ORDER))
                    },
                    trailingContent = { SettingsValueText("Reset") }
                )
            }
        }
    }
}
