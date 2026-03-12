package com.mg.mgmusicplayer.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mg.mgmusicplayer.core.player.MusicPlayerManager
import com.mg.mgmusicplayer.data.database.MusicDatabase
import com.mg.mgmusicplayer.data.model.Song
import com.mg.mgmusicplayer.data.repository.MusicRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class SortOrder {
    NAME, ARTIST, ALBUM, DATE_ADDED
}

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val database = MusicDatabase.getDatabase(application)
    private val repository = MusicRepository(application, database.musicDao())
    private val playerManager = MusicPlayerManager(application)

    private val _allSongs = MutableStateFlow<List<Song>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery

    private val _sortOrder = MutableStateFlow(SortOrder.NAME)
    val sortOrder = _sortOrder

    val favorites = repository.favorites.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val playlists = repository.playlists.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentPlaylistSongs = MutableStateFlow<List<Song>>(emptyList())
    val currentPlaylistSongs = _currentPlaylistSongs.asStateFlow()

    private val filteredSongs = combine(_allSongs, _searchQuery, _sortOrder, favorites) { songs, query, order, favs ->
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

    val songs: StateFlow<List<Song>> = filteredSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val genres: StateFlow<Map<String, List<Song>>> = filteredSongs.map { it.groupBy { s -> s.genre } }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val artists: StateFlow<Map<String, List<Song>>> = filteredSongs.map { it.groupBy { s -> s.artist } }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val albums: StateFlow<Map<String, List<Song>>> = filteredSongs.map { it.groupBy { s -> s.album } }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val folders: StateFlow<Map<String, List<Song>>> = filteredSongs.map { it.groupBy { s -> s.folder } }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val currentSong = playerManager.currentSong
    val isPlaying = playerManager.isPlaying
    val isShuffleMode = playerManager.isShuffleMode
    val repeatMode = playerManager.repeatMode
    val currentPosition = playerManager.currentPosition
    val duration = playerManager.duration

    init {
        scanMusic()
        viewModelScope.launch {
            currentSong.collect { song ->
                song?.let { repository.addToHistory(it.id) }
            }
        }
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    fun scanMusic() {
        viewModelScope.launch {
            val scannedSongs = repository.getSongs()
            _allSongs.value = scannedSongs
            playerManager.setPlaylist(scannedSongs)
        }
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            repository.toggleFavorite(song.id, !favorites.value.contains(song.id))
        }
    }

    fun createPlaylist(name: String) = viewModelScope.launch { repository.createPlaylist(name) }
    
    fun addSongToPlaylist(playlistId: String, song: Song) = viewModelScope.launch { 
        repository.addSongToPlaylist(playlistId.toLong(), song.id) 
    }

    fun addSongsToPlaylist(playlistId: String, songs: List<Song>) = viewModelScope.launch {
        repository.addSongsToPlaylist(playlistId.toLong(), songs)
    }

    fun loadPlaylistSongs(playlistId: String) {
        viewModelScope.launch {
            _currentPlaylistSongs.value = repository.getSongsInPlaylist(playlistId.toLong())
        }
    }

    fun onSearchQueryChanged(query: String) { _searchQuery.value = query }

    fun playSong(song: Song, playlist: List<Song>) {
        playerManager.setPlaylist(playlist)
        playerManager.play(song)
    }

    fun togglePlayPause() = playerManager.togglePlayPause()
    fun skipNext() = playerManager.skipNext()
    fun skipPrevious() = playerManager.skipPrevious()
    fun seekTo(position: Long) = playerManager.seekTo(position)
    fun seekForward() = playerManager.seekForward()
    fun seekBack() = playerManager.seekBack()
    fun toggleShuffle() = playerManager.toggleShuffle()
    fun cycleRepeatMode() = playerManager.cycleRepeatMode()

    fun updateSongTags(song: Song, title: String, artist: String, album: String, genre: String) {
        viewModelScope.launch {
            if (repository.updateSongTags(song, title, artist, album, genre)) {
                scanMusic()
            }
        }
    }

    fun getAudioSessionId() = playerManager.getAudioSessionId()

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
    }
}
