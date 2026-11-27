package dev.rushi.apkdownloadhelper.ui.shared.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.rushi.apkdownloadhelper.ui.theme.AlertDialogStyle

/**
 * The one alert dialog used app-wide. Same parameters as Material's AlertDialog, but the colour
 * and shape come from [AlertDialogStyle], so update, settings and helper dialogs always match.
 */
@Composable
internal fun AppAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    shape: Shape = MaterialTheme.shapes.extraLarge,
    containerColor: Color = AlertDialogStyle.containerColor(),
    properties: DialogProperties = DialogProperties(),
    stackButtons: Boolean = false,
    bottomBar: @Composable (BoxScope.() -> Unit)? = null,
    bottomReserve: Dp = 0.dp
) {
    // Same signature as before, but drawn as a full-screen Morphe-style page. shape and
    // containerColor are kept so call sites compile; the page uses the theme background.
    DialogWindow(
        onDismissRequest = onDismissRequest,
        dismissOnBackPress = properties.dismissOnBackPress,
        bottomBar = bottomBar,
        bottomReserve = bottomReserve
    ) {
        DialogSurface(modifier = modifier) {
            icon?.let { Box(Modifier.padding(bottom = 16.dp)) { it() } }
            title?.let {
                ProvideTextStyle(
                    MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        contentAlignment = Alignment.Center
                    ) { it() }
                }
            }
            text?.let {
                ProvideTextStyle(MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center)) {
                    CompositionLocalProvider(
                        LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                                .padding(bottom = 24.dp),
                            contentAlignment = Alignment.Center
                        ) { it() }
                    }
                }
            }
            if (stackButtons) {
                // Three or more actions: primary first, full width, as Morphe stacks them
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    confirmButton()
                    dismissButton?.invoke()
                }
            } else {
                // Two actions: equal width, secondary left, primary right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    dismissButton?.let { Box(Modifier.weight(1f, fill = false)) { it() } }
                    Box(Modifier.weight(1f, fill = false)) { confirmButton() }
                }
            }
        }
    }
}
