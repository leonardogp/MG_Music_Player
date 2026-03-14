package com.mg.mgmusicplayer.ui

import android.app.Application
import androidx.annotation.OptIn
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.mg.mgmusicplayer.core.player.MusicPlayerManager
import com.mg.mgmusicplayer.data.database.MusicDatabase
import com.mg.mgmusicplayer.data.database.PlaylistEntity
import com.mg.mgmusicplayer.data.model.Song
import com.mg.mgmusicplayer.data.repository.MusicRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class SortOrder {
    NAME, ARTIST, ALBUM, DATE_ADDED
}

@UnstableApi
class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val database = MusicDatabase.getDatabase(application)
    private val repository = MusicRepository(application, database.musicDao())
    private val playerManager = MusicPlayerManager(application)

    private val _searchQuery = MutableStateFlow("")
    private val _sortOrder = MutableStateFlow(SortOrder.NAME)
    private val _currentPlaylistSongs = MutableStateFlow<List<Song>>(emptyList())

    val favorites = repository.favorites.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val history = repository.history.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val playlists = repository.playlists.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val filteredSongs = combine(
        repository.allSongsFlow, 
        _searchQuery, 
        _sortOrder, 
        favorites
    ) { songs, query, order, favs ->
        val enrichedSongs = songs.map { it.copy(isFavorite = favs.contains(it.id)) }
        
        val filtered = if (query.isBlank()) {
            enrichedSongs
        } else {
            enrichedSongs.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.artist.contains(query, ignoreCase = true) ||
                it.album.contains(query, ignoreCase = true)
            }
        }
        
        when (order) {
            SortOrder.NAME -> filtered.sortedBy { it.title }
            SortOrder.ARTIST -> filtered.sortedBy { it.artist }
            SortOrder.ALBUM -> filtered.sortedBy { it.album }
            SortOrder.DATE_ADDED -> filtered.reversed()
        }
    }.flowOn(Dispatchers.Default)

    val playerState: StateFlow<PlayerState> = combine(
        playerManager.currentSong,
        playerManager.isPlaying,
        playerManager.isShuffleMode,
        playerManager.repeatMode,
        playerManager.currentPosition,
        playerManager.duration,
        playerManager.currentQueue
    ) { params ->
        @Suppress("UNCHECKED_CAST")
        PlayerState(
            currentSong = params[0] as? Song,
            isPlaying = params[1] as Boolean,
            isShuffleMode = params[2] as Boolean,
            repeatMode = params[3] as Int,
            currentPosition = params[4] as Long,
            duration = params[5] as Long,
            currentQueue = params[6] as List<Song>,
            audioSessionId = playerManager.getAudioSessionId()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlayerState())

    val uiState: StateFlow<LibraryUiState> = combine(
        filteredSongs,
        playlists,
        history,
        _currentPlaylistSongs,
        _searchQuery,
        _sortOrder,
        playerState
    ) { params ->
        @Suppress("UNCHECKED_CAST")
        val songs = params[0] as List<Song>
        LibraryUiState(
            songs = songs,
            genres = songs.groupBy { it.genre },
            artists = songs.groupBy { it.artist },
            albums = songs.groupBy { it.album },
            folders = songs.groupBy { it.folder },
            playlists = params[1] as List<PlaylistEntity>,
            history = params[2] as List<com.mg.mgmusicplayer.data.database.HistoryEntity>,
            currentPlaylistSongs = params[3] as List<Song>,
            searchQuery = params[4] as String,
            sortOrder = params[5] as SortOrder,
            playerState = params[6] as PlayerState
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LibraryUiState())

    init {
        scanMusic()
        viewModelScope.launch {
            playerManager.currentSong.collect { song ->
                song?.let { repository.addToHistory(it.id) }
            }
        }
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    fun scanMusic() {
        viewModelScope.launch(Dispatchers.Default) {
            repository.refreshMusicDatabase()
        }
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.toggleFavorite(song.id, !favorites.value.contains(song.id))
        }
    }

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

    fun playSong(song: Song, playlist: List<Song>) {
        viewModelScope.launch(Dispatchers.Default) {
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

    fun updateSongTags(song: Song, title: String, artist: String, album: String, genre: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateSongTags(song, title, artist, album, genre)
        }
    }

    fun getAudioSessionId() = playerManager.getAudioSessionId()

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
    }
}
