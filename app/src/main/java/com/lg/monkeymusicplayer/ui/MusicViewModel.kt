package com.lg.monkeymusicplayer.ui

import android.app.Application
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.media.audiofx.AudioEffect
import androidx.annotation.OptIn
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.lg.monkeymusicplayer.core.player.MusicPlayerManager
import com.lg.monkeymusicplayer.core.result.Result
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
    application: Application,
    private val repository: MusicRepository,
    private val playerManager: MusicPlayerManager,
) : AndroidViewModel(application) {

    val context get() = getApplication<Application>()

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

    // ── PUNTO 5: estado del permiso MANAGE_EXTERNAL_STORAGE ──
    // true  → el usuario ya otorgó el permiso, el editor de tags puede escribir archivos.
    // false → hay que pedirlo antes de abrir el diálogo de edición.
    private val _hasManageStoragePermission = MutableStateFlow(false)
    val hasManageStoragePermission: StateFlow<Boolean> = _hasManageStoragePermission.asStateFlow()

    // Evento one-shot: la UI escucha este Flow para saber cuándo abrir la pantalla de Settings.
    // Se usa SharedFlow (no StateFlow) para que el evento no se repita al recomponerse.
    private val _requestManageStorageEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val requestManageStorageEvent: SharedFlow<Unit> = _requestManageStorageEvent.asSharedFlow()

    // Evento one-shot para abrir el editor de tags desde el reproductor.
    // Emitido cuando el usuario toca "Editar" en FullPlayerScreen.
    private val _requestEditSongEvent = MutableSharedFlow<Song>(extraBufferCapacity = 1)
    val requestEditSongEvent: SharedFlow<Song> = _requestEditSongEvent.asSharedFlow()

    fun requestEditSong(song: Song) {
        viewModelScope.launch { _requestEditSongEvent.emit(song) }
    }

    // Llamado desde MainActivity al arrancar y al volver de la pantalla de Settings
    fun onManageStoragePermissionResult(granted: Boolean) {
        _hasManageStoragePermission.value = granted
    }

    // Llamado desde la UI cuando el usuario intenta editar tags sin el permiso.
    // Emite el evento para que MainActivity abra la pantalla de Settings correcta.
    fun requestManageStoragePermission() {
        viewModelScope.launch { _requestManageStorageEvent.emit(Unit) }
    }
    // Esto permite que la UI muestre el mensaje de error específico cuando falla,
    // en lugar de solo saber que "algo salió mal".
    private val _tagUpdateResult = MutableSharedFlow<Result<Unit>>(extraBufferCapacity = 1)
    val tagUpdateResult: SharedFlow<Result<Unit>> = _tagUpdateResult.asSharedFlow()

    val currentSong: StateFlow<Song?> = playerManager.currentSong
    val searchQuery = _searchQuery.asStateFlow()
    val equalizerData = playerManager.equalizerData

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
                       else mappedSongs.filter {
                           it.title.contains(query, ignoreCase = true) ||
                           it.artist.contains(query, ignoreCase = true)
                       }

        // ── CORRECCIÓN: aplicar el orden seleccionado por el usuario ──
        // Antes: order se extraía del combine pero nunca se usaba → el dropdown
        //        de ordenación cambiaba el estado pero la lista no cambiaba.
        val sorted = when (order) {
            SortOrder.NAME       -> filtered.sortedBy { it.title.lowercase() }
            SortOrder.ARTIST     -> filtered.sortedBy { it.artist.lowercase() }
            SortOrder.ALBUM      -> filtered.sortedBy { it.album.lowercase() }
            SortOrder.DATE_ADDED -> filtered // MediaStore no expone fecha en el modelo actual
        }

        LibraryData(
            songs = sorted,
            playlists = playlists.filter { it.name.contains(query, ignoreCase = true) },
            history = history,
            currentPlaylistSongs = mappedPlaylistSongs,
            // ── PUNTO 6: filtrar las categorías por query ──
            // Antes: groupBy se aplicaba sobre filtered (canciones filtradas), lo que
            //        mostraba géneros/artistas/álbumes cuyas canciones coincidían con la
            //        búsqueda — pero si buscabas "Rock" en la pestaña Géneros no aparecía
            //        nada porque el filtro solo buscaba en title y artist de cada canción.
            // Ahora: se genera primero el mapa completo y luego se filtra por clave,
            //        de modo que buscar "Rock" muestra el género "Rock" con todas sus canciones.
            genres = if (query.isBlank()) mappedSongs.groupBy { it.genre }
                     else mappedSongs.groupBy { it.genre }
                         .filterKeys { it.contains(query, ignoreCase = true) },
            artists = if (query.isBlank()) mappedSongs.groupBy { it.artist }
                      else mappedSongs.groupBy { it.artist }
                          .filterKeys { it.contains(query, ignoreCase = true) },
            albums = if (query.isBlank()) mappedSongs.groupBy { it.album }
                     else mappedSongs.groupBy { it.album }
                         .filterKeys { it.contains(query, ignoreCase = true) },
            folders = if (query.isBlank()) mappedSongs.groupBy { it.folder }
                      else mappedSongs.groupBy { it.folder }
                          .filterKeys { it.contains(query, ignoreCase = true) }
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
            repository.allSongsFlow.take(1).collect { _isLoading.value = false }
        }
        viewModelScope.launch {
            playerManager.currentSong.collect { song ->
                song?.let { updateAccentColor(it); loadLyrics(it) }
                    ?: run { _lyrics.value = emptyList() }
            }
        }
    }

    private fun loadLyrics(song: Song) {
        viewModelScope.launch(Dispatchers.IO) {
            val lyricsFile = File(song.path.replaceAfterLast(".", "lrc", "lrc"))
            _lyrics.value = if (lyricsFile.exists()) parseLrc(lyricsFile.readText()) else emptyList()
        }
    }

    private fun parseLrc(content: String): List<LyricLine> {
        val lines = mutableListOf<LyricLine>()
        val regex = Regex("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})](.*)")
        content.lines().forEach { line ->
            regex.find(line)?.let { match ->
                val min = match.groupValues[1].toLong()
                val sec = match.groupValues[2].toLong()
                val ms = match.groupValues[3].toLong().let { if (it < 100) it * 10 else it }
                val text = match.groupValues[4].trim()
                if (text.isNotBlank()) lines.add(LyricLine((min * 60 * 1000) + (sec * 1000) + ms, text))
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
            val bitmap = ((loader.execute(request) as? SuccessResult)?.drawable
                as? android.graphics.drawable.BitmapDrawable)?.bitmap
            if (bitmap != null) {
                Palette.from(bitmap).generate { palette ->
                    (palette?.vibrantSwatch ?: palette?.dominantSwatch)?.rgb?.let {
                        _accentColor.value = Color(it)
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

    fun openEqualizer(context: android.content.Context) {
        val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
            putExtra(AudioEffect.EXTRA_AUDIO_SESSION, playerManager.getAudioSessionId())
            putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
            putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
        }
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        }
    }

    fun setEqualizerBand(band: Short, level: Short) = playerManager.setEqualizerBand(band, level)
    fun fetchEqualizerData() = playerManager.fetchEqualizerData()

    fun createPlaylist(name: String) =
        viewModelScope.launch(Dispatchers.IO) { repository.createPlaylist(name) }

    fun deletePlaylist(playlist: PlaylistEntity) =
        viewModelScope.launch(Dispatchers.IO) { repository.deletePlaylist(playlist) }

    fun addSongToPlaylist(playlistId: String, song: Song) =
        viewModelScope.launch(Dispatchers.IO) {
            playlistId.toLongOrNull()?.let { repository.addSongToPlaylist(it, song.id) }
        }

    fun addSongsToPlaylist(playlistId: String, songs: List<Song>) =
        viewModelScope.launch(Dispatchers.IO) {
            playlistId.toLongOrNull()?.let { repository.addSongsToPlaylist(it, songs) }
        }

    fun removeSongFromPlaylist(playlistId: String, songId: Long) =
        viewModelScope.launch(Dispatchers.IO) {
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
        try {
            repository.refreshMusicDatabase { current, total ->
                _scanProgress.value = current
                _scanTotal.value = total
            }
        } finally {
            // Garantiza que el indicador de progreso desaparece aunque el scan falle
            _isScanning.value = false
        }
    }

    fun toggleFavorite(song: Song) = viewModelScope.launch {
        val currentFavorites = repository.favorites.first()
        repository.toggleFavorite(song.id, !currentFavorites.contains(song.id))
    }

    fun updateSongTags(
        song: Song,
        title: String,
        artist: String,
        album: String,
        genre: String
    ) = viewModelScope.launch {
        val result = repository.updateSongTags(song, title, artist, album, genre)
        _tagUpdateResult.emit(result)
    }

    fun playSong(song: Song, playlist: List<Song> = uiState.value.songs) {
        viewModelScope.launch {
            playerManager.setPlaylist(playlist)
            playerManager.play(song)
            // Registrar en historial — se ejecuta en IO para no bloquear la reproducción
            launch(Dispatchers.IO) { repository.addToHistory(song.id) }
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
