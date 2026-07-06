package com.lg.monkeymusicplayer.ui.components.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lg.monkeymusicplayer.data.model.Song
import com.lg.monkeymusicplayer.ui.theme.PrimaryOrange

@Composable
fun GenreList(genres: Map<String, List<Song>>, onGenreClick: (String) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(genres.keys.sorted()) { genre ->
            ListItem(
                modifier = Modifier.clickable { onGenreClick(genre) },
                headlineContent = { Text(genre) },
                supportingContent = { Text(stringResource(com.lg.monkeymusicplayer.R.string.stats_artist_songs, genres[genre]?.size ?: 0)) },
                leadingContent = {
                    Box(
                        modifier = Modifier.size(48.dp)
                            .background(PrimaryOrange.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = PrimaryOrange)
                    }
                }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        }
    }
}