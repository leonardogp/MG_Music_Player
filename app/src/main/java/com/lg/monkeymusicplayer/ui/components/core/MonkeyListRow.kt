package com.lg.monkeymusicplayer.ui.components.core

import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun MonkeyListRow(
    leadingContent: (@Composable () -> Unit)? = null,
    title: String,
    subtitle: String? = null,
    trailingContent: (@Composable () -> Unit)? = null
) {
    ListItem(
        leadingContent = { leadingContent?.invoke() },
        headlineContent = { Text(title) },
        supportingContent = if (subtitle != null) { { Text(subtitle) } } else null,
        trailingContent = { trailingContent?.invoke() }
    )
}
