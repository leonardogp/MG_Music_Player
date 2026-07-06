package com.lg.monkeymusicplayer.ui.components.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.lg.monkeymusicplayer.data.model.Song

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongMenuSheet(
    song: Song,
    onDismiss: () -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onEditTags: (Song) -> Unit,
    onToggleFavorite: (Song) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // Header con info de la canción
            ListItem(
                headlineContent = { Text(song.title, fontWeight = FontWeight.Bold) },
                supportingContent = { Text("${song.artist} • ${song.album}") },
                leadingContent = {
                    AsyncImage(
                        model = song.albumArtUri,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop,
                        error = painterResource(com.lg.monkeymusicplayer.R.drawable.ic_monkey_head)
                    )
                }
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            ListItem(
                modifier = Modifier.clickable { onPlayNext(song) },
                headlineContent = { Text(stringResource(com.lg.monkeymusicplayer.R.string.add_to_queue)) },
                leadingContent = { Icon(Icons.Default.QueueMusic, contentDescription = null) }
            )
            ListItem(
                modifier = Modifier.clickable { onAddToPlaylist(song) },
                headlineContent = { Text(stringResource(com.lg.monkeymusicplayer.R.string.add_to_playlist)) },
                leadingContent = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null) }
            )
            ListItem(
                modifier = Modifier.clickable { onToggleFavorite(song) },
                headlineContent = {
                    Text(if (song.isFavorite) stringResource(com.lg.monkeymusicplayer.R.string.remove_from_favorites) else stringResource(com.lg.monkeymusicplayer.R.string.add_to_favorites))
                },
                leadingContent = {
                    Icon(if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = null)
                }
            )
            ListItem(
                modifier = Modifier.clickable { onEditTags(song) },
                headlineContent = { Text(stringResource(com.lg.monkeymusicplayer.R.string.edit_tags)) },
                leadingContent = { Icon(Icons.Default.Edit, contentDescription = null) }
            )
        }
    }
}