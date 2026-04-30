package com.lg.monkeymusicplayer.ui.components.core

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.ui.graphics.vector.ImageVector
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.lg.monkeymusicplayer.data.model.Song
import com.lg.monkeymusicplayer.ui.PlayerState
import com.lg.monkeymusicplayer.R
import androidx.compose.ui.unit.Dp
import com.lg.monkeymusicplayer.ui.theme.MonkeyElevation
import androidx.compose.foundation.BorderStroke

@Composable
fun MonkeyPlayerBottomBar(
    playerState: PlayerState,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onClick: () -> Unit
) {
    val song: Song? = playerState.currentSong ?: playerState.lastPlayedSong
    if (song == null) return
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 8.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            AsyncImage(
                model = song.albumArtUri,
                contentDescription = null,
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Crop,
                error = painterResource(R.drawable.ic_monkey_head)
            )
            // Text area
            Column(modifier = Modifier.weight(1f)) {
                Text(text = song.title, style = MaterialTheme.typography.titleSmall)
                Text(text = song.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }
            IconButton(onClick = onPlayPause) {
                if (playerState.isPlaying) {
                    Icon(imageVector = Icons.Filled.Pause, contentDescription = null)
                } else {
                    Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null)
                }
            }
            IconButton(onClick = onSkipNext) {
                Icon(imageVector = Icons.Filled.SkipNext, contentDescription = null)
            }
        }
    }
}
