package dev.rushi.apkdownloadhelper.ui.shared.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.DialogProperties
import dev.rushi.apkdownloadhelper.ui.theme.AppDefaults

/** Text colour inside Morphe-style dialogs. */
internal val LocalDialogTextColor = compositionLocalOf { Color.Unspecified }

/** Vertical padding of dialog content (the screen-edge inset lives in [AppDialogDefaults]). */
internal val DialogContentPadding = PaddingValues(vertical = AppDefaults.ContentPadding)

/** Only used by the fallback path in [DialogWindow]. */
internal val AppDialogProperties = DialogProperties(
    usePlatformDefaultWidth = false,
    decorFitsSystemWindows = false
)

/** Title + content + optional action row. */
@Composable
internal fun AppDialog(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    actions: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    DialogWindow(onDismissRequest = onDismiss) {
        DialogSurface(modifier = modifier) {
            Column(
                modifier = Modifier.padding(DialogContentPadding),
                verticalArrangement = Arrangement.spacedBy(AppDialogDefaults.SectionSpacing)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = LocalDialogTextColor.current,
                    modifier = Modifier.fillMaxWidth()
                )
                content()
                actions?.let { actions ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AppDialogDefaults.ButtonSpacing),
                        verticalAlignment = Alignment.CenterVertically,
                        content = actions
                    )
                }
            }
        }
    }
}

/** Width, inset and text colours shared by every dialog. */
@Composable
internal fun DialogSurface(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    CompositionLocalProvider(
        LocalDialogTextColor provides MaterialTheme.colorScheme.onSurface,
        LocalContentColor provides MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = AppDialogDefaults.MaxWidth)
                .fillMaxWidth()
                .padding(horizontal = AppDialogDefaults.HorizontalInset)
                .then(modifier),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content
        )
    }
}