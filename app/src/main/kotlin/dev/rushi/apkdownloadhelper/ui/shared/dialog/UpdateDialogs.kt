package dev.rushi.apkdownloadhelper.ui.shared.dialog

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import dev.rushi.apkdownloadhelper.ui.shared.button.DialogButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.rushi.apkdownloadhelper.download.PACKAGE_PATTERN
import dev.rushi.apkdownloadhelper.ui.theme.AppDefaults
import dev.rushi.apkdownloadhelper.ui.theme.pressScale
import dev.rushi.apkdownloadhelper.updater.ShizukuHelper
import dev.rushi.apkdownloadhelper.updater.UpdateInfo

@Composable
internal fun ShizukuPackageDialog(
    initial: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var value by remember { mutableStateOf(initial) }
    var error by remember { mutableStateOf<String?>(null) }

    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Shizuku app package", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppDefaults.ItemSpacing)) {
                Text(
                    text = "Leave blank for the default package. Only change this for a Shizuku fork with a different package name.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = value,
                    onValueChange = {
                        value = it
                        error = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = error != null,
                    placeholder = { Text(ShizukuHelper.DEFAULT_PACKAGE) }
                )
                error?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        stackButtons = true,
        confirmButton = {
            DialogButton(
                text = "Save",
                onClick = {
                    val trimmed = value.trim()
                    if (trimmed.isNotEmpty() && !PACKAGE_PATTERN.matches(trimmed)) {
                        error = "Enter a valid package name, for example ${ShizukuHelper.DEFAULT_PACKAGE}."
                    } else {
                        onSave(trimmed)
                    }
                },
                buttonColor = AppDialogDefaults.confirmButtonContainer(),
                modifier = Modifier.fillMaxWidth()
            )
        },
        dismissButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DialogButton(
                    text = "Use default",
                    onClick = { onSave("") },
                    filled = false,
                    modifier = Modifier.fillMaxWidth()
                )
                DialogButton(
                    text = "Cancel",
                    onClick = onDismiss,
                    filled = false,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    )
}

@Composable
internal fun UpdatePromptDialog(
    info: UpdateInfo,
    currentVersion: String,
    onView: () -> Unit,
    onLater: () -> Unit,
    onSkip: () -> Unit,
    onDismiss: () -> Unit
) {
    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update available", fontWeight = FontWeight.Bold) },
        text = {
            Text(
                "Helper v${info.version} is available. You have v$currentVersion."
            )
        },
        stackButtons = true,
        confirmButton = {
            DialogButton(
                text = "View update",
                onClick = onView,
                buttonColor = AppDialogDefaults.confirmButtonContainer(),
                modifier = Modifier.fillMaxWidth()
            )
        },
        dismissButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DialogButton(
                    text = "Later",
                    onClick = onLater,
                    filled = false,
                    modifier = Modifier.fillMaxWidth()
                )
                DialogButton(
                    text = "Skip",
                    onClick = onSkip,
                    filled = false,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    )
}