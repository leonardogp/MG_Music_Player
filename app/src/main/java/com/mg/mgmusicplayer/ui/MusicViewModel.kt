package com.mg.mgmusicplayer.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mg.mgmusicplayer.core.player.MusicPlayerManager
import com.mg.mgmusicplayer.core.scanner.MusicScanner
import com.mg.mgmusicplayer.data.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val scanner = MusicScanner(application)
    private val playerManager = MusicPlayerManager(application)

    private val _allSongs = MutableStateFlow<List<Song>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery

    val songs: StateFlow<List<Song>> = combine(_allSongs, _searchQuery) { songs, query ->
        if (query.isBlank()) {
            songs
        } else {
            songs.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.artist.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentSong = playerManager.currentSong
    val isPlaying = playerManager.isPlaying
    val isShuffleMode = playerManager.isShuffleMode

    init {
        scanMusic()
    }

    fun scanMusic() {
        viewModelScope.launch {
            val scannedSongs = scanner.scan()
            _allSongs.value = scannedSongs
            playerManager.setPlaylist(scannedSongs)
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun playSong(song: Song) {
        playerManager.play(song)
    }

    fun togglePlayPause() {
        playerManager.togglePlayPause()
    }

    fun skipNext() {
        playerManager.skipNext()
    }

    fun skipPrevious() {
        playerManager.skipPrevious()
    }

    fun toggleShuffle() {
        playerManager.toggleShuffle()
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
    }
}