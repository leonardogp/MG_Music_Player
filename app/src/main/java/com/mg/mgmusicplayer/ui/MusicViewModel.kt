package com.mg.mgmusicplayer.ui

import android.app.Application
import android.content.ContentValues
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mg.mgmusicplayer.core.player.MusicPlayerManager
import com.mg.mgmusicplayer.core.scanner.MusicScanner
import com.mg.mgmusicplayer.data.model.Playlist
import com.mg.mgmusicplayer.data.model.Song
import com.mpatric.mp3agic.ID3v24Tag
import com.mpatric.mp3agic.Mp3File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

enum class SortOrder {
    NAME, ARTIST, ALBUM, DATE_ADDED
}

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val scanner = MusicScanner(application)
    private val playerManager = MusicPlayerManager(application)

    private val _allSongs = MutableStateFlow<List<Song>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery

    private val _sortOrder = MutableStateFlow(SortOrder.NAME)
    val sortOrder = _sortOrder

    private val _recentSongs = MutableStateFlow<List<Song>>(emptyList())
    val recentSongs = _recentSongs

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists

    private val filteredSongs = combine(_allSongs, _searchQuery, _sortOrder) { songs, query, order ->
        val filtered = if (query.isBlank()) {
            songs
        } else {
            songs.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.artist.contains(query, ignoreCase = true) ||
                it.album.contains(query, ignoreCase = true) ||
                it.genre.contains(query, ignoreCase = true) ||
                it.folder.contains(query, ignoreCase = true)
            }
        }
        
        when (order) {
            SortOrder.NAME -> filtered.sortedBy { it.title }
            SortOrder.ARTIST -> filtered.sortedBy { it.artist }
            SortOrder.ALBUM -> filtered.sortedBy { it.album }
            SortOrder.DATE_ADDED -> filtered.reversed() // Simple approach
        }
    }

    val songs: StateFlow<List<Song>> = filteredSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val genres: StateFlow<Map<String, List<Song>>> = filteredSongs.combine(MutableStateFlow(Unit)) { songs, _ ->
        songs.groupBy { it.genre }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val artists: StateFlow<Map<String, List<Song>>> = filteredSongs.combine(MutableStateFlow(Unit)) { songs, _ ->
        songs.groupBy { it.artist }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val albums: StateFlow<Map<String, List<Song>>> = filteredSongs.combine(MutableStateFlow(Unit)) { songs, _ ->
        songs.groupBy { it.album }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val folders: StateFlow<Map<String, List<Song>>> = filteredSongs.combine(MutableStateFlow(Unit)) { songs, _ ->
        songs.groupBy { it.folder }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val currentSong = playerManager.currentSong
    val isPlaying = playerManager.isPlaying
    val isShuffleMode = playerManager.isShuffleMode
    val repeatMode = playerManager.repeatMode

    init {
        scanMusic()
        viewModelScope.launch {
            currentSong.collect { song ->
                song?.let { addRecentSong(it) }
            }
        }
    }

    private fun addRecentSong(song: Song) {
        val currentList = _recentSongs.value.toMutableList()
        currentList.remove(song)
        currentList.add(0, song)
        if (currentList.size > 20) currentList.removeAt(20)
        _recentSongs.value = currentList
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    fun scanMusic() {
        viewModelScope.launch {
            val scannedSongs = scanner.scan()
            _allSongs.value = scannedSongs
            playerManager.setPlaylist(scannedSongs)
        }
    }

    fun createPlaylist(name: String) {
        val newPlaylist = Playlist(id = UUID.randomUUID().toString(), name = name)
        _playlists.value = _playlists.value + newPlaylist
    }

    fun addSongToPlaylist(playlistId: String, song: Song) {
        _playlists.value = _playlists.value.map { playlist ->
            if (playlist.id == playlistId) {
                if (!playlist.songs.contains(song)) {
                    playlist.copy(songs = playlist.songs + song)
                } else playlist
            } else playlist
        }
    }

    fun updateSongTags(song: Song, newTitle: String, newArtist: String, newAlbum: String, newGenre: String) {
        _allSongs.value = _allSongs.value.map { s ->
            if (s.id == song.id) {
                s.copy(title = newTitle, artist = newArtist, album = newAlbum, genre = newGenre)
            } else s
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val filePath = getFilePathFromUri(song.id)
                if (filePath != null) {
                    val file = File(filePath)
                    if (file.exists()) {
                        val mp3file = Mp3File(file.absolutePath)
                        val id3v2Tag = if (mp3file.hasId3v2Tag()) mp3file.id3v2Tag else ID3v24Tag()
                        id3v2Tag.title = newTitle
                        id3v2Tag.artist = newArtist
                        id3v2Tag.album = newAlbum
                        id3v2Tag.genreDescription = newGenre
                        mp3file.id3v2Tag = id3v2Tag
                        val tempPath = file.absolutePath + ".tmp"
                        mp3file.save(tempPath)
                        val tempFile = File(tempPath)
                        if (tempFile.exists()) {
                            file.delete()
                            tempFile.renameTo(file)
                        }
                    }
                }
                val values = ContentValues().apply {
                    put(MediaStore.Audio.Media.TITLE, newTitle)
                    put(MediaStore.Audio.Media.ARTIST, newArtist)
                    put(MediaStore.Audio.Media.ALBUM, newAlbum)
                }
                getApplication<Application>().contentResolver.update(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    values,
                    "${MediaStore.Audio.Media._ID} = ?",
                    arrayOf(song.id.toString())
                )
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun getFilePathFromUri(songId: Long): String? {
        val projection = arrayOf(MediaStore.Audio.Media.DATA)
        val cursor = getApplication<Application>().contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            "${MediaStore.Audio.Media._ID} = ?",
            arrayOf(songId.toString()),
            null
        )
        return cursor?.use {
            if (it.moveToFirst()) it.getString(it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)) else null
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun playSong(song: Song, playlist: List<Song>) {
        playerManager.setPlaylist(playlist)
        playerManager.play(song)
    }

    fun togglePlayPause() = playerManager.togglePlayPause()
    fun skipNext() = playerManager.skipNext()
    fun skipPrevious() = playerManager.skipPrevious()
    fun seekForward() = playerManager.seekForward()
    fun seekBack() = playerManager.seekBack()
    fun toggleShuffle() = playerManager.toggleShuffle()
    fun cycleRepeatMode() = playerManager.cycleRepeatMode()

    fun toggleFavorite(song: Song) {
        _allSongs.value = _allSongs.value.map {
            if (it.id == song.id) it.copy(isFavorite = !it.isFavorite) else it
        }
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
    }
}