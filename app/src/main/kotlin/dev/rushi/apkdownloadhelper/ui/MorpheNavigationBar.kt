package dev.rushi.apkdownloadhelper.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
internal fun MorpheNavigationBar(
    pagerState: PagerState,
    covered: Boolean,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    settingsIcon: ImageVector = MainPage.SETTINGS.icon,
    settingsDescription: String? = null,
    onHeightChanged: (Dp) -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    var measuredHeight by remember { mutableStateOf(0.dp) }

    AnimatedVisibility(
        visible = !covered,
        modifier = modifier,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
                .onGloballyPositioned { coords ->
                    measuredHeight = with(density) { coords.size.height.toDp() }
                    onHeightChanged(measuredHeight)
                },
            contentAlignment = Alignment.Center
        ) {
            // Exact Morphe Pill Container
            Surface(
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .height(64.dp)
                    .glassFill(MorpheColors.glassContainerColor(), CircleShape, hazeState),
                shape = CircleShape,
                color = Color.Transparent,
                border = BorderStroke(1.dp, MorpheColors.glassBorderColor())
            ) {
                Box(modifier = Modifier.padding(horizontal = 6.dp)) {
                    val slots = remember { mutableStateMapOf<Int, IntRange>() }

                    // Sliding indicator
                    SelectionIndicator(
                        span = slots[pagerState.currentPage],
                        modifier = Modifier.align(AbsoluteAlignment.CenterLeft)
                    )

                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MainPage.entries.forEachIndexed { index, page ->
                            val isSettings = page == MainPage.SETTINGS
                            val icon = if (isSettings) settingsIcon else page.icon
                            val label = page.label
                            val a11y = if (isSettings && settingsDescription != null) {
                                "$label, $settingsDescription"
                            } else {
                                label
                            }

                            PillAction(
                                icon = icon,
                                label = label,
                                contentDescription = a11y,
                                selected = pagerState.currentPage == index,
                                onClick = {
                                    scope.launch { pagerState.animateScrollToPage(index) }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .onPlaced { coordinates ->
                                        val left = coordinates.positionInParent().x.roundToInt()
                                        slots[index] = left until left + coordinates.size.width
                                    }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectionIndicator(span: IntRange?, modifier: Modifier = Modifier) {
    span ?: return
    val left by animateIntAsState(span.first, MorpheSprings.listSpring(), label = "pill_indicator_left")
    val width by animateIntAsState(span.last + 1 - span.first, MorpheSprings.listSpring(), label = "pill_indicator_width")
    val density = LocalDensity.current

    Box(
        modifier = modifier
            .absoluteOffset { IntOffset(left, 0) }
            .width(with(density) { width.toDp() })
            .height(52.dp)
            .background(MorpheColors.glassContainerColor(selected = true), CircleShape)
            .border(1.dp, MorpheColors.glassBorderColor(selected = true), CircleShape)
    )
}

@Composable
private fun RowScope.PillAction(
    icon: ImageVector,
    label: String,
    contentDescription: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentColor by animateColorAsState(
        MorpheColors.glassContentColor(selected),
        label = "pill_action_content"
    )
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .pressScale(interactionSource, enabled = true)
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab,
                onClick = onClick
            )
            .semantics {
                this.contentDescription = contentDescription
                this.selected = selected
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            maxLines = 1,
            modifier = Modifier.clearAndSetSemantics {}
        )
    }
}