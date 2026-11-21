package dev.rushi.apkdownloadhelper.ui.shared.dialog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.rushi.apkdownloadhelper.ui.theme.AppAnimations
import dev.rushi.apkdownloadhelper.ui.theme.AppDefaults

/** Text colour inside Morphe-style dialogs (the manager resolves this per dialog surface). */
internal val LocalDialogTextColor = compositionLocalOf { Color.Unspecified }

/** Dialog content padding matching the manager's dialog chrome. */
internal val DialogContentPadding = PaddingValues(
    horizontal = AppDefaults.ContentPaddingMedium,
    vertical = AppDefaults.ContentPadding
)

/**
 * Shared dialog properties: dismiss on back/outside, sized by the caller's own surface.
 */
internal val AppDialogProperties = DialogProperties(usePlatformDefaultWidth = false)

/**
 * Dialog chrome shared by the helper's dialogs: manager surface, title, content and optional
 * actions, so call sites stop hand-assembling their own dialog layout.
 */
@Composable
internal fun AppDialog(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    actions: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    // Dialogs arrive with the manager's scale-fade rather than snapping in. The flag only
    // flips on the way in: callers dismiss by dropping the dialog from composition, which
    // takes the window with it, so the exit spec is here for symmetry and for callers that
    // animate the dialog out before removing it.
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Dialog(onDismissRequest = onDismiss, properties = AppDialogProperties) {
        AnimatedVisibility(
            visible = visible,
            enter = AppAnimations.dialogEnter,
            exit = AppAnimations.dialogExit,
            modifier = Modifier.fillMaxWidth()
        ) {
            DialogSurface(modifier = modifier.fillMaxWidth(0.92f)) {
                Column(
                    modifier = Modifier.padding(DialogContentPadding),
                    verticalArrangement = Arrangement.spacedBy(AppDefaults.ContentPadding)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = LocalDialogTextColor.current
                    )
                    content()
                    actions?.let { actions ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(AppDefaults.ItemSpacing),
                            verticalAlignment = Alignment.CenterVertically,
                            content = actions
                        )
                    }
                }
            }
        }
    }
}

/** Wraps dialog content so nested rows resolve their text colour from the surface. */
@Composable
internal fun DialogSurface(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDefaults.SectionCornerRadius),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
    ) {
        CompositionLocalProvider(
            LocalDialogTextColor provides MaterialTheme.colorScheme.onSurface
        ) {
            Column(content = content)
        }
    }
}
