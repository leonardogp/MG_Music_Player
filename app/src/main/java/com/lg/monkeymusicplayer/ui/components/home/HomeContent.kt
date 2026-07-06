package com.lg.monkeymusicplayer.ui.components.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lg.monkeymusicplayer.R
import com.lg.monkeymusicplayer.data.model.SmartPlaylist
import com.lg.monkeymusicplayer.data.model.SmartPlaylistType
import com.lg.monkeymusicplayer.data.model.Song
import com.lg.monkeymusicplayer.ui.components.library.SongItem

@Composable
fun HomeContent(
    favoriteSongs: List<Song>,
    recentSongs: List<Song>,
    smartPlaylists: List<SmartPlaylist>,
    onSongClick: (Song) -> Unit,
    onSongMoreClick: (Song) -> Unit,
    onSmartPlaylistClick: (SmartPlaylist) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Smart Playlists (Daily Mix, etc.)
        item {
            Column {
                Text(
                    text = stringResource(R.string.smart_playlists_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    items(smartPlaylists) { smart ->
                        SmartPlaylistCardSmall(smart, onClick = { onSmartPlaylistClick(smart) })
                    }
                }
            }
        }

        // Favoritos
        if (favoriteSongs.isNotEmpty()) {
            item {
                HomeSection(
                    title = stringResource(R.string.favorites),
                    songs = favoriteSongs.take(5),
                    onSongClick = onSongClick,
                    onSongMoreClick = onSongMoreClick
                )
            }
        }

        // Recientes
        if (recentSongs.isNotEmpty()) {
            item {
                HomeSection(
                    title = stringResource(R.string.recently_added),
                    songs = recentSongs,
                    onSongClick = onSongClick,
                    onSongMoreClick = onSongMoreClick
                )
            }
        }
    }
}


@Composable
private fun HomeSection(
    title: String,
    songs: List<Song>,
    onSongClick: (Song) -> Unit,
    onSongMoreClick: (Song) -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
        )
        songs.forEach { song ->
            SongItem(
                song = song,
                isSelected = false,
                isPlaying = false,
                onClick = { onSongClick(song) },
                onMoreClick = { onSongMoreClick(song) }
            )
        }
    }
}


@Composable
private fun SmartPlaylistCardSmall(smart: SmartPlaylist, onClick: () -> Unit) {
    val title = when (smart.type) {
        SmartPlaylistType.DAILY_MIX -> stringResource(R.string.smart_daily_mix_title)
        SmartPlaylistType.REDISCOVER -> stringResource(R.string.smart_rediscover_title)
        SmartPlaylistType.TOP_SONGS -> stringResource(R.string.smart_top_songs_title)
    }
    val icon = when (smart.type) {
        SmartPlaylistType.DAILY_MIX -> Icons.Default.AutoAwesome
        SmartPlaylistType.REDISCOVER -> Icons.Default.History
        SmartPlaylistType.TOP_SONGS -> Icons.Default.Star
    }

    val gradients = listOf(
        listOf(Color(0xFFFF8C00), Color(0xFFFF5500)),
        listOf(Color(0xFF7B2FBE), Color(0xFFFF8C00)),
        listOf(Color(0xFF1DB954), Color(0xFF0D7A38))
    )
    val colors = when (smart.type) {
        SmartPlaylistType.DAILY_MIX -> gradients[0]
        SmartPlaylistType.REDISCOVER -> gradients[1]
        SmartPlaylistType.TOP_SONGS -> gradients[2]
    }

    Card(
        modifier = Modifier
            .width(160.dp)
            .height(100.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Brush.linearGradient(colors))) {
            Icon(
                icon, null,
                modifier = Modifier.align(Alignment.Center).size(48.dp).alpha(0.15f),
                tint = Color.White
            )
            Text(
                title,
                modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}