package com.lg.monkeymusicplayer.ui.components.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
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

@Composable
fun ArtistList(artists: Map<String, List<Song>>, onArtistClick: (String) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(artists.keys.sorted()) { artist ->
            ListItem(
                modifier = Modifier.clickable { onArtistClick(artist) },
                headlineContent = { Text(artist) },
                supportingContent = { Text(stringResource(com.lg.monkeymusicplayer.R.string.stats_artist_songs, artists[artist]?.size ?: 0)) },
                leadingContent = {
                    Box(modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.secondaryContainer, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Person, contentDescription = null)
                    }
                }
            )
        }
    }
}
