package com.lg.monkeymusicplayer.ui

import com.lg.monkeymusicplayer.data.database.HistoryEntity
import com.lg.monkeymusicplayer.data.database.PlaylistEntity
import com.lg.monkeymusicplayer.data.model.Song

// ── CORRECCIÓN: LayoutMode eliminado ──
// El enum LayoutMode { LIST, GRID } y el campo layoutMode en LibraryUiState
// nunca fueron leídos por ningún composable. Dead code eliminado.
// Si se implementa vista de grilla en el futuro, añadir aquí con su lógica completa.

data class LibraryUiState(
    val isLoading: Boolean = true,
    val isScanning: Boolean = false,
    val scanProgress: Int = 0,
    val scanTotal: Int = 0,
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
    val playerState: PlayerState = PlayerState()
)
