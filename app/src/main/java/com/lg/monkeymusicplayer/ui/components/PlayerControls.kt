package com.lg.monkeymusicplayer.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lg.monkeymusicplayer.R
import com.lg.monkeymusicplayer.ui.PlayerState
import com.lg.monkeymusicplayer.ui.theme.PrimaryOrange

@Composable
fun PlayerControls(
    playerState: PlayerState,
    onPlayPause: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        // Main controls row
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 24.dp)
        ) {
            // Skip previous
            IconButton(onClick = onSkipPrevious) {
                Icon(Icons.Default.SkipPrevious, contentDescription = stringResource(R.string.previous))
            }

            // Seek back 10s
            IconButton(onClick = onSeekBack) {
                Icon(Icons.Default.Replay10, contentDescription = stringResource(R.string.seek_back_10))
            }

            // Play/Pause main button
            Button(
                onClick = onPlayPause,
                modifier = Modifier.size(72.dp)
            ) {
                Icon(
                    imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = stringResource(R.string.play_pause),
                    modifier = Modifier.size(24.dp)
                )
            }

            // Seek forward 10s
            IconButton(onClick = onSeekForward) {
                // ── CORRECCIÓN: Replay30 → Forward10 ──
                // Replay30 es "retroceder 30s", confundía al usuario con la acción opuesta.
                Icon(Icons.Default.Forward10, contentDescription = stringResource(R.string.seek_forward_10))
            }

            // Skip next
            IconButton(onClick = onSkipNext) {
                Icon(Icons.Default.SkipNext, contentDescription = stringResource(R.string.skip_next))
            }
        }

        // Bottom controls
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Shuffle
            IconButton(onClick = onToggleShuffle) {
                Icon(
                    Icons.Default.Shuffle,
                    contentDescription = stringResource(R.string.shuffle),
                    tint = if (playerState.isShuffleMode) PrimaryOrange else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Repeat mode
            IconButton(onClick = onCycleRepeat) {
                val icon = when (playerState.repeatMode) {
                    PlayerState.RepeatMode.ALL -> Icons.Default.RepeatOn
                    PlayerState.RepeatMode.ONE -> Icons.Default.RepeatOneOn
                    else -> Icons.Default.Repeat
                }
                Icon(
                    imageVector = icon,
                    contentDescription = stringResource(R.string.repeat),
                    tint = if (playerState.repeatMode != PlayerState.RepeatMode.NONE) PrimaryOrange else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.weight(1f))

            // Favorite
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (playerState.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = stringResource(R.string.favorites),
                    tint = if (playerState.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
