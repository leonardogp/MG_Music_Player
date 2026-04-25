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
import com.lg.monkeymusicplayer.core.billing.BillingManager
import com.lg.monkeymusicplayer.core.cast.CastManager
import com.lg.monkeymusicplayer.core.feature.Feature
import com.lg.monkeymusicplayer.core.feature.FeatureGate
import com.lg.monkeymusicplayer.data.repository.CloudSyncRepository
import com.lg.monkeymusicplayer.core.queue.QueueManager
import com.lg.monkeymusicplayer.domain.usecase.GetSmartPlaylistsUseCase
import com.lg.monkeymusicplayer.domain.usecase.GetSongsUseCase
import com.lg.monkeymusicplayer.domain.usecase.GetUserStatsUseCase
import com.lg.monkeymusicplayer.domain.usecase.PlaySongUseCase
import com.lg.monkeymusicplayer.domain.usecase.RefreshMusicLibraryUseCase
import com.lg.monkeymusicplayer.domain.usecase.ToggleFavoriteUseCase
import com.lg.monkeymusicplayer.domain.usecase.UpdateSongTagsUseCase
import com.lg.monkeymusicplayer.ui.theme.PrimaryOrange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

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
    private val getSongsUseCase: GetSongsUseCase,
    private val getSmartPlaylistsUseCase: GetSmartPlaylistsUseCase,
    private val getUserStatsUseCase: GetUserStatsUseCase,
    private val refreshMusicLibraryUseCase: RefreshMusicLibraryUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val playSongUseCase: PlaySongUseCase,
    private val updateSongTagsUseCase: UpdateSongTagsUseCase,
    val queueManager: QueueManager,
    private val castManager: CastManager,
    private val cloudSyncRepository: CloudSyncRepository,
    val featureGate: FeatureGate,
    val billingManager: BillingManager,
) : ViewModel() {

    // 1. Declarar TODOS los MutableStateFlow primero para evitar NullPointerException en inicialización
    private val _loadState = MutableStateFlow<LibraryLoadState>(LibraryLoadState.Idle)
    private val _isInitialLoad = MutableStateFlow(true)
    private val _searchQuery = MutableStateFlow("")
    private val _sortOrder = MutableStateFlow(SortOrder.NAME)
    private val _currentPlaylistSongs = MutableStateFlow<List<Song>>(emptyList())
    private val _sleepTimerMinutes = MutableStateFlow(0)
    private val _sleepTimerRemaining = MutableStateFlow(0L)
    private val _accentColor = MutableStateFlow(PrimaryOrange)
    private val _lyrics = MutableStateFlow<List<LyricLine>>(emptyList())
    private val _isLoadingLyrics = MutableStateFlow(false)
    private val _hasManageStoragePermission = MutableStateFlow(false)
    
    // Eventos one-shot
    private val _requestManageStorageEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val _requestEditSongEvent = MutableSharedFlow<Song>(extraBufferCapacity = 1)
    private val _tagUpdateResult = MutableSharedFlow<Result<Unit>>(extraBufferCapacity = 1)

    // 2. Propiedades públicas simples
    val context get() = applicationContext
    val excludedFolders: StateFlow<List<String>> = excludedFoldersRepository.excludedFolders
    val hasManageStoragePermission: StateFlow<Boolean> = _hasManageStoragePermission.asStateFlow()
    val requestManageStorageEvent: SharedFlow<Unit> = _requestManageStorageEvent.asSharedFlow()
    val requestEditSongEvent: SharedFlow<Song> = _requestEditSongEvent.asSharedFlow()
    val tagUpdateResult: SharedFlow<Result<Unit>> = _tagUpdateResult.asSharedFlow()
    val isLoadingLyrics: StateFlow<Boolean> = _isLoadingLyrics.asStateFlow()
    val currentSong: StateFlow<Song?> = playerManager.currentSong
    val searchQuery = _searchQuery.asStateFlow()
    val equalizerData = playerManager.equalizerData
    val castState = castManager.castState
    val isCastConnected = castManager.isConnected
    val eqPresets: StateFlow<List<EqPresetEntity>> = repository.eqPresets
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // 3. Clases de datos auxiliares para flows combinados
    private data class PlaybackCore(
        val currentSong: Song?,
        val isPlaying: Boolean,
        val isShuffleMode: Boolean,
        val repeatMode: Int,
        val currentPosition: Long,
        val duration: Long,
        val audioSessionId: Int
    )

    private data class PlaybackUiExtras(
        val accentColor: Color,
        val lyrics: List<LyricLine>,
        val sleepTimerMinutes: Int,
        val sleepTimerRemainingMillis: Long
    )

    private data class FilteredSongs(
        val all: List<Song>,
        val filtered: List<Song>,
        val playlistSongs: List<Song>
    )

    private data class LibraryCatalog(
        val playlists: List<PlaylistEntity>,
        val history: List<HistoryEntity>,
        val genres: Map<String, List<Song>>,
        val artists: Map<String, List<Song>>,
        val albums: Map<String, List<Song>>,
        val folders: Map<String, List<Song>>
    )

    // 4. Definición de Flows intermedios
    private val playbackCoreFlow: Flow<PlaybackCore> = combine(
        playerManager.currentSong,
        playerManager.isPlaying,
        playerManager.isShuffleMode,
        playerManager.repeatMode,
        playerManager.currentPosition
    ) { song, playing, shuffle, repeat, position ->
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

    private val lastPlayedSongFlow: Flow<Song?> = combine(
        repository.history,
        repository.allSongsFlow
    ) { history, songs ->
        if (history.isEmpty()) return@combine null
        val songIndex = songs.associateBy { it.id }
        songIndex[history.first().songId]
    }.flowOn(Dispatchers.Default)

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

    // 5. Estado de UI final
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
    }.combine(getSmartPlaylistsUseCase()) { state, smart ->
        state.copy(smartPlaylists = smart)
    }.stateIn(viewModelScope, SharingStarted.Lazily, LibraryUiState())

    private var sleepTimerJob: Job? = null

    init {
        viewModelScope.launch {
            repository.allSongsFlow.take(1).collect { songs ->
                if (songs.isEmpty()) {
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

    // Resto de funciones...
    fun onCastButtonClick() {
        if (castManager.isConnected.value) {
            castManager.currentRemoteSongId 
        }
    }

    fun castCurrentSong() {
        val song = playerManager.currentSong.value ?: return
        castManager.loadSong(song)
        playerManager.pause()
    }

    fun castActiveQueue() {
        val songs = queueManager.activeSongs
        if (songs.isEmpty()) return
        castManager.loadQueue(songs)
        playerManager.pause()
    }

    fun addExcludedFolder(path: String) = excludedFoldersRepository.addFolder(path)
    fun removeExcludedFolder(path: String) = excludedFoldersRepository.removeFolder(path)

    fun requestEditSong(song: Song) {
        viewModelScope.launch { _requestEditSongEvent.emit(song) }
    }

    fun onManageStoragePermissionResult(granted: Boolean) {
        _hasManageStoragePermission.value = granted
    }

    fun requestManageStoragePermission() {
        viewModelScope.launch { _requestManageStorageEvent.emit(Unit) }
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
        if (_loadState.value is LibraryLoadState.Scanning) return@launch
        _loadState.value = LibraryLoadState.Scanning()
        val result = refreshMusicLibraryUseCase { current, total ->
            _loadState.value = LibraryLoadState.Scanning(current, total)
        }
        _loadState.value = when (result) {
            is Result.Success -> LibraryLoadState.Idle
            is Result.Error   -> LibraryLoadState.Error(result.message)
            else              -> LibraryLoadState.Idle
        }
        _isInitialLoad.value = false
    }

    fun toggleFavorite(song: Song) = viewModelScope.launch {
        val currentFavorites = repository.favorites.first()
        toggleFavoriteUseCase.toggle(song.id, currentFavorites.contains(song.id))
    }

    fun updateSongTags(
        song: Song,
        title: String,
        artist: String,
        album: String,
        genre: String
    ) = viewModelScope.launch {
        val result = updateSongTagsUseCase(song, title, artist, album, genre)
        _tagUpdateResult.emit(result)
    }

    fun playSong(song: Song, playlist: List<Song> = uiState.value.songs) {
        viewModelScope.launch {
            playSongUseCase(song, playlist)
        }
    }

    fun addToQueue(song: Song) = playerManager.addToQueue(song)

    fun createAndSwitchQueue(name: String, displayName: String = name, songs: List<Song> = emptyList()) {
        queueManager.createQueue(name, displayName)
        if (songs.isNotEmpty()) queueManager.setQueueSongs(name, songs)
        queueManager.switchToQueue(name)
        val queueSongs = queueManager.getQueue(name)?.songs ?: return
        viewModelScope.launch { playerManager.setPlaylist(queueSongs) }
    }

    fun switchToQueue(name: String) {
        queueManager.switchToQueue(name)
        val songs = queueManager.getQueue(name)?.songs ?: return
        viewModelScope.launch { playerManager.setPlaylist(songs) }
    }

    fun deleteQueue(name: String) = queueManager.deleteQueue(name)

    fun addToActiveQueue(song: Song) {
        queueManager.addToActiveQueue(song)
        playerManager.addToQueue(song)
    }

    fun moveInActiveQueue(fromIndex: Int, toIndex: Int) {
        queueManager.moveInQueue(queueManager.activeQueueName.value, fromIndex, toIndex)
    }
    fun togglePlayPause() = playerManager.togglePlayPause()
    fun skipNext() = playerManager.skipNext()
    fun skipPrevious() = playerManager.skipPrevious()
    fun seekTo(position: Long) = playerManager.seekTo(position)
    fun seekForward() = playerManager.seekForward()
    fun seekBack() = playerManager.seekBack()
    fun toggleShuffle() = playerManager.toggleShuffle()
    fun cycleRepeatMode() = playerManager.cycleRepeatMode()

    suspend fun cloudSyncUpload() = cloudSyncRepository.upload()
    suspend fun cloudSyncDownload() = cloudSyncRepository.download()

    fun isFeatureUnlocked(feature: Feature): Boolean = featureGate.isUnlocked(feature)
    fun lockedFeatures(): List<Feature> = featureGate.lockedPremiumFeatures()
    fun launchProUpgrade(activity: android.app.Activity) {
        billingManager.launchBillingFlow(activity)
    }

    fun saveEqPreset(name: String, levels: List<Float>) =
        viewModelScope.launch(Dispatchers.IO) { repository.saveEqPreset(name, levels) }

    fun deleteEqPreset(preset: EqPresetEntity) =
        viewModelScope.launch(Dispatchers.IO) { repository.deleteEqPreset(preset) }

    fun exportBackup(uri: android.net.Uri) = viewModelScope.launch {
        backupRepository.exportBackup(uri)
    }
    fun importBackup(uri: android.net.Uri) = viewModelScope.launch {
        backupRepository.importBackup(uri)
    }
}
