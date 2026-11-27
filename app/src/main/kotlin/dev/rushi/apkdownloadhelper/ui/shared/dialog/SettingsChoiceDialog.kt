package dev.rushi.apkdownloadhelper.ui.shared.dialog

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import dev.chrisbanes.haze.HazeState
import dev.rushi.apkdownloadhelper.ui.model.DownloadSource
import dev.rushi.apkdownloadhelper.ui.shared.button.BottomCloseBar
import dev.rushi.apkdownloadhelper.ui.shared.settings.SettingsGroup
import dev.rushi.apkdownloadhelper.ui.shared.settings.SettingsItem
import dev.rushi.apkdownloadhelper.ui.shared.source.SourceAvatar
import dev.rushi.apkdownloadhelper.ui.theme.AppDefaults
import dev.rushi.apkdownloadhelper.ui.theme.hazeSourceIfEnabled
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics

internal data class SettingsChoice(
    val icon: ImageVector?,
    val title: String,
    val description: String = "",
    val source: DownloadSource? = null
)

/** Subtitle every source option card shows. Change it here and all source pickers follow. */
internal const val SOURCE_CHOICE_DESCRIPTION = "Prefer this source when it has the file"

internal fun DownloadSource.toSettingsChoice(
    description: String = SOURCE_CHOICE_DESCRIPTION
) = SettingsChoice(icon = null, title = label, description = description, source = this)

/**
 * THE option-picker popup: Default source, Save downloads, Switch source, Shizuku mode...
 *
 * @param showClose Shows the same pinned Close bar the full screens use.
 * @param maxListHeight How tall the option list may get before it scrolls.
 */
@Composable
internal fun SettingsChoiceDialog(
    title: String,
    choices: List<SettingsChoice>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
    showClose: Boolean = true,
    maxListHeight: Dp = AppDialogDefaults.ChoiceListMaxHeight
) {
    // Height reported by BottomCloseBar, same as footerHeightDp on the full screens
    var footerHeightDp by remember { mutableStateOf(AppDialogDefaults.BottomBarHeight) }
    val listScroll = rememberScrollState()
    val headerHaze = remember { HazeState() }
    // Typed explicitly so `Modifier.align` resolves (BoxScope receiver)
    val closeBar: @Composable BoxScope.() -> Unit = {
        BottomCloseBar(
            onClose = onDismiss,
            hazeState = headerHaze,
            showBackground = true,
            onHeightChanged = { footerHeightDp = it },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxListHeight)
                    .topScrollFade(listScroll)
                    .hazeSourceIfEnabled(headerHaze)
                    .verticalScroll(listScroll)
            ) {
                SettingsGroup {
                    choices.forEachIndexed { index, choice ->
                        SettingsItem(
                            onClick = { onSelect(index) },
                            icon = if (choice.source == null) choice.icon else null,
                            leadingContent = choice.source?.let { src ->
                                { SourceAvatar(source = src, size = AppDefaults.IconSize) }
                            },
                            title = choice.title,
                            subtitle = choice.description.takeIf { it.isNotBlank() },
                            trailingContent = { SelectCircle(selected = index == selectedIndex) },
                            modifier = Modifier.semantics { selected = index == selectedIndex }
                        )
                    }
                }
                // Lets the last card scroll clear of the pinned Close bar
                if (showClose) Spacer(Modifier.height(footerHeightDp))
            }
        },
        stackButtons = true,
        confirmButton = {},
        // Same BottomCloseBar call the full screens use (CloseButton.kt). The list runs
        // behind it, so no space is reserved here.
        bottomBar = if (showClose) closeBar else null
    )
}

/** Fades the top edge of a scrolling list once it has scrolled, instead of cutting it hard. */
private fun Modifier.topScrollFade(
    state: ScrollState,
    fadeHeight: Dp = AppDialogDefaults.TopFadeHeight
): Modifier = graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val h = fadeHeight.toPx()
        val progress = (state.value / h).coerceIn(0f, 1f)
        if (progress > 0f) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Black.copy(alpha = 1f - progress), Color.Black),
                    startY = 0f,
                    endY = h
                ),
                size = Size(size.width, h),
                blendMode = BlendMode.DstIn
            )
        }
    }

@Composable
internal fun SelectCircle(selected: Boolean) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(AppDialogDefaults.SelectCircleSize)
            .clip(CircleShape)
            .then(
                if (selected) Modifier.background(Color(0xFF22C55E))
                else Modifier.border(
                    AppDialogDefaults.SelectCircleBorder,
                    scheme.onSurfaceVariant.copy(alpha = 0.5f),
                    CircleShape
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(
                Icons.Outlined.Check,
                null,
                tint = Color.White,
                modifier = Modifier.size(AppDialogDefaults.SelectCheckSize)
            )
        }
    }
}