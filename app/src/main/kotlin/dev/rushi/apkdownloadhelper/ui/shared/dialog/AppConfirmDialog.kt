package dev.rushi.apkdownloadhelper.ui.shared.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import dev.rushi.apkdownloadhelper.ui.shared.button.DialogButton
import dev.rushi.apkdownloadhelper.ui.shared.text.fittedTextAutoSize

/**
 * Icon circle pinned to the top edge, then title, message and two buttons centred on the page.
 * Edit this file to restyle every confirm popup.
 *
 * @param destructiveConfirm Only turns the confirm button's TEXT red (Stop, Delete).
 */
@Composable
internal fun AppConfirmDialog(
    title: String,
    message: String,
    dismissText: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    icon: ImageVector? = null,
    iconContainerColor: Color = AppDialogDefaults.defaultIconContainer(),
    iconTint: Color = AppDialogDefaults.defaultIconTint(),
    destructiveConfirm: Boolean = false,
    confirmButtonColor: Color =
        if (destructiveConfirm) AppDialogDefaults.defaultButtonContainer()
        else AppDialogDefaults.confirmButtonContainer(),
    confirmButtonContentColor: Color = MaterialTheme.colorScheme.onSurface
) {
    val scheme = MaterialTheme.colorScheme
    DialogWindow(onDismissRequest = onDismiss) {
        DialogSurface {
            Column(
                modifier = Modifier.padding(DialogContentPadding),
                verticalArrangement = Arrangement.spacedBy(AppDialogDefaults.SectionSpacing),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (icon != null) {
                    Box(
                        modifier = Modifier
                            .padding(bottom = AppDialogDefaults.IconBottomSpacing)
                            .size(AppDialogDefaults.IconCircleSize)
                            .clip(CircleShape)
                            .background(iconContainerColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(AppDialogDefaults.IconGlyphSize)
                        )
                    }
                }
                // Title is as wide as the button row; a long title shrinks to fit one line
                val titleStyle = MaterialTheme.typography.headlineSmall
                Text(
                    text = title,
                    style = titleStyle,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = scheme.onSurface,
                    maxLines = 1,
                    autoSize = fittedTextAutoSize(
                        maxFontSize = titleStyle.fontSize,
                        minFontSize = AppDialogDefaults.TitleMinFontSize
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = scheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppDialogDefaults.ButtonSpacing)
                ) {
                    DialogButton(
                        text = dismissText,
                        onClick = onDismiss,
                        filled = false,
                        modifier = Modifier.weight(1f)
                    )
                    DialogButton(
                        text = confirmText,
                        onClick = onConfirm,
                        isDestructive = destructiveConfirm,
                        buttonColor = confirmButtonColor,
                        buttonContentColor = confirmButtonContentColor,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}