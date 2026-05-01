package com.lg.monkeymusicplayer.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lg.monkeymusicplayer.ui.components.core.MonkeyCard

@Composable
fun QuickPlaylistCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    MonkeyCard(
        modifier = Modifier
            .width(170.dp)
            .height(120.dp),
        glow = true,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                Icons.Rounded.QueueMusic,
                null,
                tint = MaterialTheme.colorScheme.primary
            )

            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}