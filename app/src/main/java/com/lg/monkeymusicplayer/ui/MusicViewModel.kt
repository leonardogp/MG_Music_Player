package com.lg.monkeymusicplayer.ui

import android.app.Application
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.media.audiofx.AudioEffect
import androidx.annotation.OptIn
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.lg.monkeymusicplayer.R
import com.lg.monkeymusicplayer.core.player.MusicPlayerManager
import com.lg.monkeymusicplayer.core.player.MusicService
import com.lg.monkeymusicplayer.core.result.Result
import com.lg.monkeymusicplayer.data.database.EqPresetEntity
import com.lg.monkeymusicplayer.data.database.HistoryEntity
import com.lg.monkeymusicplayer.data.database.PlaylistEntity
import com.lg.monkeymusicplayer.data.model.LyricLine
import com.lg.monkeymusicplayer.data.model.Song
import com.lg.monkeymusicplayer.data.repository.ExcludedFoldersRepository
import com.lg.monkeymusicplayer.data.repository.MusicRepository
import com.lg.monkeymusicplayer.data.repository.BackupRepository
import com.lg.monkeymusicplayer.data.repository.SmartRepository
import com.lg.monkeymusicplayer.data.repository.StatsRepository
import com.lg.monkeymusicplayer.ui.theme.PrimaryOrange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File  // retenido para posibles extensiones futuras; sin uso directo

@OptIn(UnstableApi::class)
@HiltViewModel
class MusicViewModel @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    private val repository: MusicRepository,
    private val playerManager: MusicPlayerManager,
    private val excludedFoldersRepository: ExcludedFoldersRepository,
    private val smartRepository: SmartRepository,
    val statsRepository: StatsRepository,
    val backupRepository: BackupRepository,
) : ViewModel() {

    val context get() = applicationContext

    // Carpetas excluidas del escaneo — expuesto directamente desde el repositorio
    val excludedFolders: StateFlow<List<String>> = excludedFoldersRepository.excludedFolders

    fun addExcludedFolder(path: String) = excludedFoldersRepository.addFolder(path)
    fun removeExcludedFolder(path: String) = excludedFoldersRepository.removeFolder(path)

    private val _loadState = MutableStateFlow<LibraryLoadState>(LibraryLoadState.Idle)
    private val _isInitialLoad = MutableStateFlow(true)
    private val _searchQuery = MutableStateFlow("")
    private val _sortOrder = MutableStateFlow(SortOrder.NAME)
    private val _currentPlaylistSongs = MutableStateFlow<List<Song>>(emptyList())
    private val _sleepTimerMinutes = MutableStateFlow(0)
    private val _sleepTimerRemaining = MutableStateFlow(0L)
    private var sleepTimerJob: Job? = null
    private val _accentColor = MutableStateFlow(PrimaryOrange)
    private val _lyrics = MutableStateFlow<List<LyricLine>>(emptyList())
    private val _isLoadingLyrics = MutableStateFlow(false)
    val isLoadingLyrics: StateFlow<Boolean> = _isLoadingLyrics.asStateFlow()

    // EQ presets guardados por el usuario — Flow directo desde Room
    val eqPresets: StateFlow<List<EqPresetEntity>> = repository.eqPresets
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

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

    // ── Flows intermedios: playerStateFlow ───────────────────────────────────
    //
    // Dividimos las 14 fuentes originales en 3 flows tipados.
    // Cada combine interno usa la sobrecarga tipada (≤5 fuentes) — el compilador
    // valida tipos en lugar de depender de casts Array<Any?> en runtime.

    /** Posición de reproducción: canción activa, flags de control y progreso. */
    private data class PlaybackCore(
        val currentSong: Song?,
        val isPlaying: Boolean,
        val isShuffleMode: Boolean,
        val repeatMode: Int,
        val currentPosition: Long,
        val duration: Long,
        val audioSessionId: Int
    )

    /** Extras de UI: color de acento, letras y sleep timer. */
    private data class PlaybackUiExtras(
        val accentColor: Color,
        val lyrics: List<LyricLine>,
        val sleepTimerMinutes: Int,
        val sleepTimerRemainingMillis: Long
    )

    private val playbackCoreFlow: Flow<PlaybackCore> = combine(
        playerManager.currentSong,
        playerManager.isPlaying,
        playerManager.isShuffleMode,
        playerManager.repeatMode,
        playerManager.currentPosition
    ) { song, playing, shuffle, repeat, position ->
        // duration y audioSessionId se combinan en un segundo paso porque combine
        // tiene sobrecargas tipadas solo hasta 5 argumentos
        PlaybackCore(song, playing, shuffle, repeat, position, 0L, -1)
    }.combine(playerManager.duration) { core, dur ->
        core.copy(duration = dur)
    }.combine(playerManager.audioSessionId) { core, sessionId ->
        core.copy(audioSessionId = sessionId)
    }.flowOn(Dispatchers.Default)

    private val playbackUiExtrasFlow: Flow<PlaybackUiExtras> = combine(
        _accentColor,
        _lyrics,
        _sleepTimerMinutes,
        _sleepTimerRemaining
    ) { color, lyrics, timerMin, timerRem ->
        PlaybackUiExtras(color, lyrics, timerMin, timerRem)
    }.flowOn(Dispatchers.Default)

    private val mappedQueueFlow: Flow<List<Song>> = combine(
        playerManager.currentQueue,
        repository.favorites
    ) { queue, favorites ->
        queue.map { it.copy(isFavorite = favorites.contains(it.id)) }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    /**
     * Última canción reproducida, resuelta desde el historial.
     * Visible en PlayerBottomBar cuando currentSong == null (estado idle).
     */
    private val lastPlayedSongFlow: Flow<Song?> = combine(
        repository.history,
        repository.allSongsFlow
    ) { history, songs ->
        if (history.isEmpty()) return@combine null
        val songIndex = songs.associateBy { it.id }
        songIndex[history.first().songId]
    }.flowOn(Dispatchers.Default)

    // combine final: 5 fuentes tipadas — sin Array<Any?>, sin índices numéricos
    private val playerStateFlow: Flow<PlayerState> = combine(
        playbackCoreFlow,
        playbackUiExtrasFlow,
        mappedQueueFlow,
        repository.favorites,
        lastPlayedSongFlow
    ) { core, extras, queue, favorites, lastPlayed ->
        val isFavorite = core.currentSong?.let { favorites.contains(it.id) } ?: false
        PlayerState(
            currentSong              = core.currentSong?.copy(isFavorite = isFavorite),
            lastPlayedSong           = lastPlayed,
            isPlaying                = core.isPlaying,
            isShuffleMode            = core.isShuffleMode,
            repeatMode               = core.repeatMode,
            currentPosition          = core.currentPosition,
            duration                 = core.duration,
            currentQueue             = queue,
            audioSessionId           = core.audioSessionId,
            accentColor              = extras.accentColor,
            lyrics                   = extras.lyrics,
            sleepTimerMinutes        = extras.sleepTimerMinutes,
            sleepTimerRemainingMillis = extras.sleepTimerRemainingMillis,
            isFavorite               = isFavorite
        )
    }

    // ── Flows intermedios: libraryDataFlow ───────────────────────────────────
    //
    // Dividimos las 7 fuentes originales en 2 flows tipados.

    /** Songs filtradas, ordenadas y enriquecidas con isFavorite. */
    private data class FilteredSongs(
        val all: List<Song>,            // todas las canciones con isFavorite
        val filtered: List<Song>,       // filtradas y ordenadas por query/sortOrder
        val playlistSongs: List<Song>   // canciones de la playlist activa, con isFavorite
    )

    /** Metadatos de biblioteca: playlists, historial, categorías. */
    private data class LibraryCatalog(
        val playlists: List<PlaylistEntity>,
        val history: List<HistoryEntity>,
        val genres: Map<String, List<Song>>,
        val artists: Map<String, List<Song>>,
        val albums: Map<String, List<Song>>,
        val folders: Map<String, List<Song>>
    )

    private val filteredSongsFlow: Flow<FilteredSongs> = combine(
        repository.allSongsFlow,
        repository.favorites,
        _currentPlaylistSongs,
        _searchQuery,
        _sortOrder
    ) { songs, favorites, playlistSongs, query, order ->
        val mapped = songs.map { it.copy(isFavorite = favorites.contains(it.id)) }
        val mappedPlaylist = playlistSongs.map { it.copy(isFavorite = favorites.contains(it.id)) }

        val filtered = if (query.isBlank()) mapped
                       else mapped.filter {
                           it.title.contains(query, ignoreCase = true) ||
                           it.artist.contains(query, ignoreCase = true)
                       }
        val sorted = when (order) {
            SortOrder.NAME       -> filtered.sortedBy { it.title.lowercase() }
            SortOrder.ARTIST     -> filtered.sortedBy { it.artist.lowercase() }
            SortOrder.ALBUM      -> filtered.sortedBy { it.album.lowercase() }
            SortOrder.DATE_ADDED -> filtered
        }
        FilteredSongs(all = mapped, filtered = sorted, playlistSongs = mappedPlaylist)
    }.flowOn(Dispatchers.Default)

    private val libraryCatalogFlow: Flow<LibraryCatalog> = combine(
        repository.playlists,
        repository.history,
        filteredSongsFlow,
        _searchQuery
    ) { playlists, history, fs, query ->
        val mapped = fs.all
        LibraryCatalog(
            playlists = playlists.filter { it.name.contains(query, ignoreCase = true) },
            history   = history,
            genres    = if (query.isBlank()) mapped.groupBy { it.genre }
                        else mapped.groupBy { it.genre }.filterKeys { it.contains(query, ignoreCase = true) },
            artists   = if (query.isBlank()) mapped.groupBy { it.artist }
                        else mapped.groupBy { it.artist }.filterKeys { it.contains(query, ignoreCase = true) },
            albums    = if (query.isBlank()) mapped.groupBy { it.album }
                        else mapped.groupBy { it.album }.filterKeys { it.contains(query, ignoreCase = true) },
            folders   = if (query.isBlank()) mapped.groupBy { it.folder }
                        else mapped.groupBy { it.folder }.filterKeys { it.contains(query, ignoreCase = true) }
        )
    }.flowOn(Dispatchers.Default)

    // ── uiState final: 5 fuentes tipadas ────────────────────────────────────
    //
    // Antes: 8 fuentes con Array<Any?>.
    // Ahora: filteredSongsFlow y libraryCatalogFlow ya agrupan todo —
    // solo necesitamos 5 fuentes para construir LibraryUiState completo.

    val uiState: StateFlow<LibraryUiState> = combine(
        filteredSongsFlow,
        libraryCatalogFlow,
        playerStateFlow,
        _loadState,
        _isInitialLoad
    ) { fs, catalog, playerState, loadState, isInitialLoad ->
        LibraryUiState(
            loadState            = loadState,
            isInitialLoad        = isInitialLoad,
            songs                = fs.filtered,
            genres               = catalog.genres,
            artists              = catalog.artists,
            albums               = catalog.albums,
            folders              = catalog.folders,
            playlists            = catalog.playlists,
            history              = catalog.history,
            currentPlaylistSongs = fs.playlistSongs,
            searchQuery          = _searchQuery.value,
            sortOrder            = _sortOrder.value,
            playerState          = playerState
        )
    }
    // Encadenar smartPlaylists como 6ª fuente (combine() está limitado a 5 parámetros
    // en la sobrecarga tipada; .combine() encadenado es el patrón recomendado).
    .combine(smartRepository.smartPlaylists) { state, smart ->
        state.copy(smartPlaylists = smart)
    }
    .stateIn(viewModelScope, SharingStarted.Lazily, LibraryUiState())

    init {
        viewModelScope.launch {
            // Esperar el primer valor de la librería.
            // Si está vacía → primera instalación o DB limpia → lanzar escaneo automático.
            // Si ya tiene canciones → solo quitar el loading, no re-escanear.
            repository.allSongsFlow.take(1).collect { songs ->
                if (songs.isEmpty()) {
                    // Primera instalación o DB vacía: escanear.
                    // isInitialLoad se apaga cuando el scan termina (en scanMusic).
                    scanMusic()
                } else {
                    _isInitialLoad.value = false
                }
            }
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
            _isLoadingLyrics.value = true
            _lyrics.value = emptyList()
            try {
                val lrcContent = repository.fetchLyrics(song)
                _lyrics.value = if (lrcContent != null) parseLrc(lrcContent) else emptyList()
            } finally {
                _isLoadingLyrics.value = false
            }
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
                // Iniciar fade out 30s antes del fin del timer.
                // Si el timer es ≤ 30s, el fade empieza inmediatamente.
                val fadeStartThreshold = MusicService.SLEEP_FADE_DURATION_MS
                var fadeTriggered = false

                while (_sleepTimerRemaining.value > 0) {
                    delay(1000)
                    _sleepTimerRemaining.value -= 1000

                    if (!fadeTriggered && _sleepTimerRemaining.value <= fadeStartThreshold) {
                        fadeTriggered = true
                        playerManager.fadeAndPause()
                    }
                }
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
        if (_loadState.value is LibraryLoadState.Scanning) return@launch
        _loadState.value = LibraryLoadState.Scanning()
        try {
            repository.refreshMusicDatabase { current, total ->
                _loadState.value = LibraryLoadState.Scanning(current, total)
            }
            _loadState.value = LibraryLoadState.Idle
        } catch (e: Exception) {
            _loadState.value = LibraryLoadState.Error(e.message ?: context.getString(R.string.error_unknown))
        } finally {
            // Apagar initial load en cualquier caso (scan completado o fallido)
            _isInitialLoad.value = false
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

    // ── EQ Presets ──────────────────────────────────────────────────────────────

    fun saveEqPreset(name: String, levels: List<Float>) =
        viewModelScope.launch(Dispatchers.IO) { repository.saveEqPreset(name, levels) }

    fun deleteEqPreset(preset: EqPresetEntity) =
        viewModelScope.launch(Dispatchers.IO) { repository.deleteEqPreset(preset) }
}
