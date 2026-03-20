package com.lg.monkeymusicplayer.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.media.audiofx.AudioEffect
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.lg.monkeymusicplayer.core.player.MusicPlayerManager
import com.lg.monkeymusicplayer.data.database.HistoryEntity
import com.lg.monkeymusicplayer.data.database.PlaylistEntity
import com.lg.monkeymusicplayer.data.model.LyricLine
import com.lg.monkeymusicplayer.data.model.Song
import com.lg.monkeymusicplayer.data.repository.MusicRepository
import com.lg.monkeymusicplayer.ui.theme.PrimaryOrange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(UnstableApi::class)
class MusicViewModel(
    private val repository: MusicRepository,
    private val playerManager: MusicPlayerManager,
    val context: Context
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    private val _isScanning = MutableStateFlow(false)
    private val _scanProgress = MutableStateFlow(0)
    private val _scanTotal = MutableStateFlow(0)
    private val _searchQuery = MutableStateFlow("")
    private val _sortOrder = MutableStateFlow(SortOrder.NAME)
    private val _currentPlaylistSongs = MutableStateFlow<List<Song>>(emptyList())
    private val _sleepTimerMinutes = MutableStateFlow(0)
    private val _sleepTimerRemaining = MutableStateFlow(0L)
    private var sleepTimerJob: Job? = null
    private val _accentColor = MutableStateFlow(PrimaryOrange)
    private val _lyrics = MutableStateFlow<List<LyricLine>>(emptyList())

    val currentSong: StateFlow<Song?> = playerManager.currentSong
    val searchQuery = _searchQuery.asStateFlow()
    val equalizerData = playerManager.equalizerData

    // Optimizamos la cola mapeada para que no se recalcule cada segundo con el progreso
    private val mappedQueueFlow = combine(
        playerManager.currentQueue,
        repository.favorites
    ) { queue, favorites ->
        queue.map { it.copy(isFavorite = favorites.contains(it.id)) }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val playerStateFlow = combine(
        playerManager.currentSong,
        playerManager.isPlaying,
        playerManager.isShuffleMode,
        playerManager.repeatMode,
        playerManager.currentPosition,
        playerManager.duration,
        mappedQueueFlow,
        playerManager.audioSessionId,
        _accentColor,
        _lyrics,
        _sleepTimerMinutes,
        _sleepTimerRemaining,
        repository.favorites
    ) { args: Array<Any?> ->
        val currentSong = args[0] as Song?
        val currentQueue = args[6] as List<Song>
        @Suppress("UNCHECKED_CAST")
        val favorites = args[12] as List<Long>
        
        val isFavorite = currentSong?.let { favorites.contains(it.id) } ?: false
        
        PlayerState(
            currentSong = currentSong?.copy(isFavorite = isFavorite),
            isPlaying = args[1] as Boolean,
            isShuffleMode = args[2] as Boolean,
            repeatMode = args[3] as Int,
            currentPosition = args[4] as Long,
            duration = args[5] as Long,
            currentQueue = currentQueue,
            audioSessionId = args[7] as Int,
            accentColor = args[8] as Color,
            lyrics = (args[9] as? List<*>)?.filterIsInstance<LyricLine>() ?: emptyList(),
            sleepTimerMinutes = args[10] as Int,
            sleepTimerRemainingMillis = args[11] as Long,
            shuffleEnabled = args[2] as Boolean,
            isFavorite = isFavorite
        )
    }

    private val libraryDataFlow = combine(
        repository.allSongsFlow,
        _searchQuery,
        _sortOrder,
        repository.playlists,
        repository.history,
        _currentPlaylistSongs,
        repository.favorites
    ) { args: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val songs = args[0] as List<Song>
        val query = args[1] as String
        val order = args[2] as SortOrder
        @Suppress("UNCHECKED_CAST")
        val playlists = args[3] as List<PlaylistEntity>
        @Suppress("UNCHECKED_CAST")
        val history = args[4] as List<HistoryEntity>
        @Suppress("UNCHECKED_CAST")
        val playlistSongs = args[5] as List<Song>
        @Suppress("UNCHECKED_CAST")
        val favorites = args[6] as List<Long>

        val mappedSongs = songs.map { it.copy(isFavorite = favorites.contains(it.id)) }
        val mappedPlaylistSongs = playlistSongs.map { it.copy(isFavorite = favorites.contains(it.id)) }

        val filtered = if (query.isBlank()) mappedSongs
                      else mappedSongs.filter { it.title.contains(query, ignoreCase = true) || it.artist.contains(query, ignoreCase = true) }
        
        LibraryData(
            songs = filtered,
            playlists = playlists.filter { it.name.contains(query, ignoreCase = true) },
            history = history,
            currentPlaylistSongs = mappedPlaylistSongs,
            genres = filtered.groupBy { it.genre },
            artists = filtered.groupBy { it.artist },
            albums = filtered.groupBy { it.album },
            folders = filtered.groupBy { it.folder }
        )
    }.flowOn(Dispatchers.Default)

    val uiState: StateFlow<LibraryUiState> = combine(
        libraryDataFlow,
        playerStateFlow,
        _isLoading,
        _isScanning,
        _scanProgress,
        _scanTotal,
        _searchQuery,
        _sortOrder
    ) { args: Array<Any?> ->
        val data = args[0] as LibraryData
        val player = args[1] as PlayerState
        LibraryUiState(
            isLoading = args[2] as Boolean,
            isScanning = args[3] as Boolean,
            scanProgress = args[4] as Int,
            scanTotal = args[5] as Int,
            songs = data.songs,
            genres = data.genres,
            artists = data.artists,
            albums = data.albums,
            folders = data.folders,
            playlists = data.playlists,
            history = data.history,
            currentPlaylistSongs = data.currentPlaylistSongs,
            searchQuery = args[6] as String,
            sortOrder = args[7] as SortOrder,
            playerState = player
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, LibraryUiState())

    private data class LibraryData(
        val songs: List<Song>,
        val playlists: List<PlaylistEntity>,
        val history: List<HistoryEntity>,
        val currentPlaylistSongs: List<Song>,
        val genres: Map<String, List<Song>>,
        val artists: Map<String, List<Song>>,
        val albums: Map<String, List<Song>>,
        val folders: Map<String, List<Song>>
    )

    init {
        viewModelScope.launch {
            repository.allSongsFlow.take(1).collect {
                _isLoading.value = false
            }
        }
        
        viewModelScope.launch {
            playerManager.currentSong.collect { song ->
                song?.let { 
                    updateAccentColor(it)
                    loadLyrics(it)
                } ?: run {
                    _lyrics.value = emptyList()
                }
            }
        }
    }

    private fun loadLyrics(song: Song) {
        viewModelScope.launch(Dispatchers.IO) {
            val lyricsFile = File(song.path.replaceAfterLast(".", "lrc", "lrc"))
            if (lyricsFile.exists()) {
                val lines = parseLrc(lyricsFile.readText())
                _lyrics.value = lines
            } else {
                _lyrics.value = emptyList()
            }
        }
    }

    private fun parseLrc(content: String): List<LyricLine> {
        val lines = mutableListOf<LyricLine>()
        val regex = Regex("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})](.*)")
        content.lines().forEach { line ->
            val match = regex.find(line)
            if (match != null) {
                val min = match.groupValues[1].toLong()
                val sec = match.groupValues[2].toLong()
                val ms = match.groupValues[3].toLong().let { if (it < 100) it * 10 else it }
                val time = (min * 60 * 1000) + (sec * 1000) + ms
                val text = match.groupValues[4].trim()
                if (text.isNotBlank()) lines.add(LyricLine(time, text))
            }
        }
        return lines.sortedBy { it.timeMs }
    }

    private suspend fun updateAccentColor(song: Song) {
        withContext(Dispatchers.IO) {
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(song.albumArtUri)
                .allowHardware(false)
                .build()

            val result = (loader.execute(request) as? SuccessResult)?.drawable
            val bitmap = (result as? BitmapDrawable)?.bitmap

            if (bitmap != null) {
                Palette.from(bitmap).generate { palette ->
                    palette?.vibrantSwatch?.rgb?.let { color ->
                        _accentColor.value = Color(color)
                    } ?: palette?.dominantSwatch?.rgb?.let { color ->
                        _accentColor.value = Color(color)
                    }
                }
            }
        }
    }

    fun setSleepTimer(minutes: Int) {
        _sleepTimerMinutes.value = minutes
        sleepTimerJob?.cancel()
        if (minutes > 0) {
            _sleepTimerRemaining.value = minutes * 60 * 1000L
            sleepTimerJob = viewModelScope.launch {
                while (_sleepTimerRemaining.value > 0) {
                    delay(1000)
                    _sleepTimerRemaining.value -= 1000
                }
                playerManager.pause()
                _sleepTimerMinutes.value = 0
            }
        } else {
            _sleepTimerRemaining.value = 0L
        }
    }

    fun openEqualizer(context: Context) {
        val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
        intent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, playerManager.getAudioSessionId())
        intent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
        intent.putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        }
    }

    fun setEqualizerBand(band: Short, level: Short) {
        playerManager.setEqualizerBand(band, level)
    }

    fun fetchEqualizerData() {
        playerManager.fetchEqualizerData()
    }

    fun createPlaylist(name: String) = viewModelScope.launch(Dispatchers.IO) { repository.createPlaylist(name) }
    fun deletePlaylist(playlist: PlaylistEntity) = viewModelScope.launch(Dispatchers.IO) { repository.deletePlaylist(playlist) }
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
            _currentPlaylistSongs.value = repository.getSongsInPlaylist(id)
        }
    }

    fun onSearchQueryChanged(query: String) { _searchQuery.value = query }
    fun setSortOrder(order: SortOrder) { _sortOrder.value = order }
    
    fun scanMusic() = viewModelScope.launch {
        if (_isScanning.value) return@launch
        _isScanning.value = true
        _scanProgress.value = 0
        _scanTotal.value = 0
        repository.refreshMusicDatabase { current, total ->
            _scanProgress.value = current
            _scanTotal.value = total
        }
        _isScanning.value = false
    }

    fun toggleFavorite(song: Song) = viewModelScope.launch {
        val currentFavorites = repository.favorites.first()
        val isCurrentlyFavorite = currentFavorites.contains(song.id)
        repository.toggleFavorite(song.id, !isCurrentlyFavorite)
    }

    fun updateSongTags(song: Song, title: String, artist: String, album: String, genre: String) = viewModelScope.launch {
        repository.updateSongTags(song, title, artist, album, genre)
    }

    fun playSong(song: Song, playlist: List<Song> = uiState.value.songs) {
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
