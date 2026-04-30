package com.lg.monkeymusicplayer.ui.components.core

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/** Simple wrapper around AlertDialog for consistent usage. */
@Composable
fun MonkeyDialog(
    onDismiss: () -> Unit,
    title: String? = null,
    text: String? = null,
    confirmButtonText: String = "OK",
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { if (title != null) Text(title) },
        text = { if (text != null) androidx.compose.material3.Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmButtonText)
            }
        }
    )
}
