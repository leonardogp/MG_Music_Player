package com.mg.mgmusicplayer.ui

import com.mg.mgmusicplayer.data.database.HistoryEntity
import com.mg.mgmusicplayer.data.database.PlaylistEntity
import com.mg.mgmusicplayer.data.model.Song

enum class LayoutMode {
    LIST, GRID
}

data class LibraryUiState(
    val songs: List<Song> = emptyList(),
    val genres: Map<String, List<Song>> = emptyMap(),
    val artists: Map<String, List<Song>> = emptyMap(),
    val albums: Map<String, List<Song>> = emptyMap(),
    val folders: Map<String, List<Song>> = emptyMap(),
    val playlists: List<PlaylistEntity> = emptyList(),
    val history: List<HistoryEntity> = emptyList(),
    val currentPlaylistSongs: List<Song> = emptyList(),
    val searchQuery: String = "",
    val sortOrder: SortOrder = SortOrder.NAME,
    val playerState: PlayerState = PlayerState(),
    val layoutMode: LayoutMode = LayoutMode.LIST
)
