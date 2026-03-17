package com.lg.monkeymusicplayer.ui

import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.lg.monkeymusicplayer.core.player.MusicPlayerManager
import com.lg.monkeymusicplayer.data.database.HistoryEntity
import com.lg.monkeymusicplayer.data.database.PlaylistEntity
import com.lg.monkeymusicplayer.data.model.Song
import com.lg.monkeymusicplayer.data.repository.MusicRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class MusicViewModel(
    private val repository: MusicRepository,
    private val playerManager: MusicPlayerManager
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.NAME)
    val sortOrder = _sortOrder.asStateFlow()

    private val _currentPlaylistSongs = MutableStateFlow<List<Song>>(emptyList())
    val currentPlaylistSongs = _currentPlaylistSongs.asStateFlow()

    val songs: StateFlow<List<Song>> = repository.allSongsFlow
        .combine(searchQuery) { songs, query ->
            if (query.isBlank()) songs
            else songs.filter { it.title.contains(query, ignoreCase = true) || it.artist.contains(query, ignoreCase = true) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlists = repository.playlists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<LibraryUiState> = combine(
        songs,
        playlists,
        repository.history,
        currentPlaylistSongs,
        searchQuery,
        sortOrder,
        playerManager.currentSong,
        playerManager.isPlaying,
        playerManager.isShuffleMode,
        playerManager.repeatMode,
        playerManager.currentPosition,
        playerManager.duration,
        playerManager.currentQueue
    ) { args: Array<Any?> ->
        val songsList = args[0] as List<Song>
        val playlistsList = args[1] as List<PlaylistEntity>
        val historyList = args[2] as List<HistoryEntity>
        val currentPlaylistSongsList = args[3] as List<Song>
        val query = args[4] as String
        val order = args[5] as SortOrder
        val currentSong = args[6] as Song?
        val isPlaying = args[7] as Boolean
        val isShuffleMode = args[8] as Boolean
        val repeatMode = args[9] as Int
        val currentPosition = args[10] as Long
        val duration = args[11] as Long
        val currentQueue = args[12] as List<Song>
        
        val playerState = PlayerState(
            currentSong = currentSong,
            isPlaying = isPlaying,
            isShuffleMode = isShuffleMode,
            repeatMode = repeatMode,
            currentPosition = currentPosition,
            duration = duration,
            currentQueue = currentQueue
        )

        LibraryUiState(
            songs = songsList,
            genres = songsList.groupBy { it.genre },
            artists = songsList.groupBy { it.artist },
            albums = songsList.groupBy { it.album },
            folders = songsList.groupBy { it.folder },
            playlists = playlistsList,
            history = historyList,
            currentPlaylistSongs = currentPlaylistSongsList,
            searchQuery = query,
            sortOrder = order,
            playerState = playerState
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LibraryUiState())

    fun createPlaylist(name: String) = viewModelScope.launch(Dispatchers.IO) {
        repository.createPlaylist(name)
    }

    fun deletePlaylist(playlist: PlaylistEntity) = viewModelScope.launch(Dispatchers.IO) {
        repository.deletePlaylist(playlist)
    }

    fun addSongToPlaylist(playlistId: String, song: Song) = viewModelScope.launch(Dispatchers.IO) {
        playlistId.toLongOrNull()?.let { repository.addSongToPlaylist(it, song.id) }
    }

    fun addSongsToPlaylist(playlistId: String, songs: List<Song>) = viewModelScope.launch(Dispatchers.IO) {
        playlistId.toLongOrNull()?.let { repository.addSongsToPlaylist(it, songs) }
    }

    fun removeSongFromPlaylist(playlistId: String, songId: Long) = viewModelScope.launch(Dispatchers.IO) {
        val id = playlistId.toLongOrNull() ?: return@launch
        repository.removeSongFromPlaylist(id, songId)
        loadPlaylistSongs(playlistId)
    }

    fun loadPlaylistSongs(playlistId: String) {
        viewModelScope.launch(Dispatchers.Default) {
            val id = playlistId.toLongOrNull() ?: return@launch
            val songs = repository.getSongsInPlaylist(id)
            _currentPlaylistSongs.value = songs
        }
    }

    fun onSearchQueryChanged(query: String) { _searchQuery.value = query }
    
    fun setSortOrder(order: SortOrder) { _sortOrder.value = order }

    fun scanMusic() = viewModelScope.launch {
        repository.refreshMusicDatabase()
    }

    fun toggleFavorite(song: Song) = viewModelScope.launch {
        repository.toggleFavorite(song.id, !song.isFavorite)
    }

    fun updateSongTags(song: Song, title: String, artist: String, album: String, genre: String) = viewModelScope.launch {
        repository.updateSongTags(song, title, artist, album, genre)
    }

    fun playSong(song: Song, playlist: List<Song>) {
        viewModelScope.launch {
            playerManager.setPlaylist(playlist)
            playerManager.play(song)
        }
    }
    
    fun addToQueue(song: Song) = playerManager.addToQueue(song)

    fun togglePlayPause() = playerManager.togglePlayPause()
    fun skipNext() = playerManager.skipNext()
    fun skipPrevious() = playerManager.skipPrevious()
    fun seekTo(position: Long) = playerManager.seekTo(position)
    fun seekForward() = playerManager.seekForward()
    fun seekBack() = playerManager.seekBack()
    fun toggleShuffle() = playerManager.toggleShuffle()
    fun cycleRepeatMode() = playerManager.cycleRepeatMode()
}
