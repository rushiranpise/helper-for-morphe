
package dev.rushi.apkdownloadhelper.ui.shared.button

import dev.rushi.apkdownloadhelper.R
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import dev.rushi.apkdownloadhelper.ui.theme.AppDefaults
import dev.rushi.apkdownloadhelper.ui.theme.glassFill
import dev.rushi.apkdownloadhelper.ui.theme.pressScale
import dev.rushi.apkdownloadhelper.ui.theme.rememberMorphePillShape

@Composable
internal fun BottomCloseBar(
    onClose: () -> Unit,
    hazeState: HazeState? = null,
    onHeightChanged: ((Dp) -> Unit)? = null,
    modifier: Modifier = Modifier,
    buttonWidth: Dp = Dp.Unspecified,
    showBackground: Boolean = false
) {
    val density = LocalDensity.current
    val colors = MaterialTheme.colorScheme
    val navBarHeight = with(density) {
        WindowInsets.navigationBars.getBottom(this).toDp()
    }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val closeHaze = remember { HazeState() }
    val effectiveHaze = hazeState ?: closeHaze

    val closeInteraction = remember { MutableInteractionSource() }
    val closeShape = rememberMorphePillShape(closeInteraction)

    Box(modifier = modifier.fillMaxWidth()) {
        // Optional scrim gradient — same stop values used everywhere in the app
        if (showBackground) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(navBarHeight + 76.dp)
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Transparent,
                            0.45f to colors.background.copy(alpha = 0.55f),
                            0.80f to colors.background.copy(alpha = 0.85f),
                            1.0f to colors.background.copy(alpha = 0.96f)
                        )
                    )
            )
        }

        // The pill button, padded away from the system nav bar
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .then(
                    if (buttonWidth != Dp.Unspecified) Modifier.width(buttonWidth)
                    else Modifier.fillMaxWidth()
                )
                .onSizeChanged { size ->
                    val contentHeight = with(density) { size.height.toDp() }
                    onHeightChanged?.invoke(
                        if (showBackground) contentHeight + navBarHeight + 24.dp else contentHeight
                    )
                }
                .then(
                    if (showBackground) {
                        Modifier
                            .navigationBarsPadding()
                            .padding(
                                horizontal = AppDefaults.ContentPadding,
                                vertical = 12.dp
                            )
                    } else {
                        Modifier
                    }
                )
        ) {
            OutlinedButton(
                onClick = {
                    focusManager.clearFocus(force = true)
                    keyboardController?.hide()
                    onClose()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .pressScale(closeInteraction, pressedScale = 0.96f)
                    .glassFill(
                        glass = Color.Transparent,
                        shape = closeShape,
                        hazeState = effectiveHaze
                    ),
                interactionSource = closeInteraction,
                shape = closeShape,
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = colors.onSurface.copy(alpha = 0.85f)
                ),
                contentPadding = PaddingValues(
                    horizontal = 16.dp,
                    vertical = 14.dp
                )
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
