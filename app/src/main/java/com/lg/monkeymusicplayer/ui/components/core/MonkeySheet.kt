package com.lg.monkeymusicplayer.ui.components.core

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable

/** Simple wrapper for a Modal Bottom Sheet with explicit show flag. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonkeySheet(
    show: Boolean,
    onDismiss: () -> Unit,
    sheetContent: @Composable () -> Unit
) {
    if (!show) return
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        sheetContent()
    }
}
