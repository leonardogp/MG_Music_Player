package com.lg.monkeymusicplayer.ui.components.core

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.lg.monkeymusicplayer.data.model.Song

@Composable
fun SongCard(
    song: Song,
    onClick: () -> Unit
) {
    MonkeyCard(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        elevation = com.lg.monkeymusicplayer.ui.theme.MonkeyElevation.Raised,
        onClick = onClick,
        content = {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                AsyncImage(
                    model = song.albumArtUri,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop
                )
                Column(modifier = Modifier.padding(start = 8.dp)) {
                    Text(text = song.title)
                    Text(text = song.artist, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                }
            }
        }
    )
}
