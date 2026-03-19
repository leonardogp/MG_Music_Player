package com.lg.monkeymusicplayer.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lg.monkeymusicplayer.ui.PlayerState
import kotlinx.coroutines.delay

@Composable
fun MediaProgressSlider(
    playerState: PlayerState,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val progress by remember(playerState.currentPosition, playerState.duration) {
        derivedStateOf {
            if (playerState.duration > 0L) {
                playerState.currentPosition.toFloat() / playerState.duration
            } else 0f
        }
    }

    Column {
        Slider(
            value = progress,
            onValueChange = { newProgress ->
                val newPosition = (newProgress * playerState.duration).toLong()
                onSeekTo(newPosition)
            },
            modifier = modifier
                .fillMaxWidth()
                .height(20.dp),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = formatDuration(playerState.currentPosition),
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = formatDuration(playerState.duration),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val seconds = (durationMs / 1000) % 60
    val minutes = (durationMs / (1000 * 60)) % 60
    val hours = (durationMs / (1000 * 60 * 60)) % 24
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}
