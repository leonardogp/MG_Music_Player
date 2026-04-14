package com.lg.monkeymusicplayer.core.player

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionToken
import com.lg.monkeymusicplayer.core.tracker.StatTracker
import com.lg.monkeymusicplayer.data.model.Song
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@UnstableApi
class MusicPlayerManager(context: Context, private val statTracker: StatTracker) {

    private val appContext = context.applicationContext
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private val controller: MediaController? get() = if (controllerFuture?.isDone == true) try { controllerFuture?.get() } catch (e: Exception) { null } else null

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _playbackState = MutableStateFlow(Player.STATE_IDLE)
    val playbackState: StateFlow<Int> = _playbackState

    private val _isShuffleMode = MutableStateFlow(false)
    val isShuffleMode: StateFlow<Boolean> = _isShuffleMode

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration
    
    private val _currentQueue = MutableStateFlow<List<Song>>(emptyList())
    val currentQueue: StateFlow<List<Song>> = _currentQueue

    private val _audioSessionId = MutableStateFlow(C.AUDIO_SESSION_ID_UNSET)
    val audioSessionId: StateFlow<Int> = _audioSessionId

    private val _equalizerData = MutableStateFlow<Bundle?>(null)
    val equalizerData: StateFlow<Bundle?> = _equalizerData

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressJob: Job? = null
    
    private var lastPlaylist = listOf<Song>()

    // Índice para lookup O(1) en lugar de find{} O(N) en updateQueue/updateCurrentSong
    private var playlistIndex = mapOf<String, Song>()

    // playerListener: Player.Listener para eventos de reproducción
    private var playerListener: Player.Listener? = null

    // reconnectListener: MediaController.Listener para detectar desconexión de sesión.
    private var reconnectListener: MediaController.Listener? = null

    init {
        setupMediaController()
    }

    // Contador de intentos para backoff exponencial en reconexión
    private var reconnectAttempts = 0
    private var reconnectJob: Job? = null

    private fun setupMediaController() {
        val sessionToken = SessionToken(appContext, ComponentName(appContext, MusicService::class.java))
        
        // Creamos el listener de la sesión
        val mcl = object : MediaController.Listener {
            override fun onDisconnected(controller: MediaController) {
                scheduleReconnect()
            }
        }
        reconnectListener = mcl

        // Lo establecemos en el Builder antes de construir el controlador
        controllerFuture = MediaController.Builder(appContext, sessionToken)
            .setListener(mcl)
            .buildAsync()

        controllerFuture?.addListener({
            try {
                val mediaController = controllerFuture?.get() ?: run {
                    scheduleReconnect()
                    return@addListener
                }
                // Conexión exitosa — resetear contador de reintentos
                reconnectAttempts = 0

                playerListener = object : Player.Listener {
                    // Duración de la canción activa capturada en STATE_READY.
                    // onMediaItemTransition lee currentPosition/duration DESPUÉS de que
                    // Media3 ya actualizó el cursor a la nueva canción, por lo que no
                    // son fiables para describir la canción ANTERIOR. Guardamos la duración
                    // confirmada cuando el player está en STATE_READY para usarla al
                    // decidir skip vs complete en la transición.
                    private var confirmedDurationMs: Long = 0L
                    // Flag para evitar doble onPlayStarted (transición + onIsPlayingChanged)
                    private var transitionHandled = false

                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        val prevSong = _currentSong.value
                        val prevDurMs = confirmedDurationMs

                        // Clasificar el evento de la canción anterior usando la duración
                        // confirmada (no la del nuevo item) y la posición actual.
                        // Para AUTO (auto-advance), la canción terminó → complete.
                        // Para SEEK (skip manual), verificar ratio contra el umbral.
                        if (prevSong != null && prevDurMs > 0L) {
                            when (reason) {
                                Player.MEDIA_ITEM_TRANSITION_REASON_AUTO -> {
                                    // La canción terminó sola: siempre es complete
                                    statTracker.onSongComplete(prevSong.id, prevDurMs)
                                }
                                Player.MEDIA_ITEM_TRANSITION_REASON_SEEK -> {
                                    // Skip manual: posMs viene del nuevo item (puede ser 0),
                                    // pero sabemos que fue skip porque el usuario lo forzó.
                                    // Usamos currentPosition solo si es razonablemente alto;
                                    // de lo contrario asumimos 0 (inicio de la siguiente).
                                    val posMs = mediaController.currentPosition
                                        .takeIf { it > 0L && it < prevDurMs } ?: 0L
                                    statTracker.onSkip(prevSong.id, posMs, prevDurMs)
                                }
                                // REPEAT y PLAYLIST_CHANGED no generan evento de stats
                            }
                        }

                        confirmedDurationMs = 0L
                        transitionHandled = false

                        updateCurrentSong(mediaItem)
                        _duration.value = mediaController.duration.coerceAtLeast(0L)
                        fetchAudioSessionId()

                        // Registrar inicio de la nueva canción.
                        // No condicionamos a isPlaying porque en auto-advance el player
                        // puede estar en STATE_BUFFERING en este instante.
                        val newSong = mediaItem?.localConfiguration?.tag as? Song
                        if (newSong != null) {
                            statTracker.onPlayStarted(newSong.id)
                            transitionHandled = true
                        }
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        val song = _currentSong.value
                        if (isPlaying) {
                            startProgressUpdate()
                            fetchAudioSessionId()
                            // onPlayStarted solo si NO vino de una transición reciente.
                            // Evita doble conteo cuando auto-advance ya lo registró arriba.
                            if (song != null && !transitionHandled) {
                                statTracker.onPlayStarted(song.id)
                            }
                            transitionHandled = false
                        } else {
                            stopProgressUpdate()
                            if (song != null) {
                                statTracker.onPause(song.id, mediaController.currentPosition)
                            }
                        }
                        _isPlaying.value = isPlaying
                    }

                    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                        _isShuffleMode.value = shuffleModeEnabled
                    }

                    override fun onRepeatModeChanged(repeatMode: Int) {
                        _repeatMode.value = repeatMode
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY) {
                            val dur = mediaController.duration.coerceAtLeast(0L)
                            _duration.value = dur
                            // Guardar duración confirmada para usarla en onMediaItemTransition.
                            // STATE_READY garantiza que duration es válida (no C.TIME_UNSET).
                            if (dur > 0L) confirmedDurationMs = dur
                            fetchAudioSessionId()
                        } else if (playbackState == Player.STATE_ENDED) {
                            // Última canción de la cola terminó sin auto-advance
                            val song = _currentSong.value
                            val dur = confirmedDurationMs.takeIf { it > 0L }
                                ?: mediaController.duration.coerceAtLeast(0L)
                            if (song != null && dur > 0L) {
                                statTracker.onSongComplete(song.id, dur)
                                confirmedDurationMs = 0L
                            }
                        }
                        _playbackState.value = playbackState
                    }

                    override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
                        updateQueue()
                    }
                }
                mediaController.addListener(playerListener!!)

                // Initial state sync
                _isPlaying.value = mediaController.isPlaying
                _playbackState.value = mediaController.playbackState
                _isShuffleMode.value = mediaController.shuffleModeEnabled
                _repeatMode.value = mediaController.repeatMode
                updateCurrentSong(mediaController.currentMediaItem)
                _duration.value = mediaController.duration.coerceAtLeast(0L)
                updateQueue()
                if (mediaController.isPlaying) startProgressUpdate()

                fetchAudioSessionId()
                fetchEqualizerData()

            } catch (e: Exception) {
                e.printStackTrace()
                scheduleReconnect()
            }
        }, MoreExecutors.directExecutor())
    }

    /**
     * Reconexión con backoff exponencial: 500ms → 1s → 2s → 4s (máx).
     * Cancela cualquier reintento pendiente para no acumular corrutinas.
     */
    private fun scheduleReconnect() {
        reconnectJob?.cancel()
        reconnectJob = scope.launch(Dispatchers.Main) {
            val delayMs = minOf(500L * (1L shl reconnectAttempts), 4000L)
            delay(delayMs)
            reconnectAttempts = (reconnectAttempts + 1).coerceAtMost(4)
            reconnect()
        }
    }

    private fun reconnect() {
        controller?.let { player ->
            playerListener?.let { player.removeListener(it) }
            // No es necesario (ni posible) remover el MediaController.Listener manualmente
            // ya que se asocia al ciclo de vida del controlador en el Builder.
        }
        playerListener = null
        reconnectListener = null
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        setupMediaController()
    }

    private fun fetchAudioSessionId() {
        val player = controller ?: return
        val command = SessionCommand(MusicService.COMMAND_GET_AUDIO_SESSION_ID, Bundle.EMPTY)
        val future = player.sendCustomCommand(command, Bundle.EMPTY)
        future.addListener({
            try {
                val result = future.get()
                if (result.resultCode == SessionResult.RESULT_SUCCESS) {
                    val sessionId = result.extras.getInt("audio_session_id", C.AUDIO_SESSION_ID_UNSET)
                    if (sessionId != C.AUDIO_SESSION_ID_UNSET) {
                        _audioSessionId.value = sessionId
                    }
                }
            } catch (e: Exception) {
                // Ignore errors
            }
        }, MoreExecutors.directExecutor())
    }

    fun fetchEqualizerData() {
        val player = controller ?: return
        val command = SessionCommand(MusicService.COMMAND_GET_EQUALIZER_DATA, Bundle.EMPTY)
        val future = player.sendCustomCommand(command, Bundle.EMPTY)
        future.addListener({
            try {
                val result = future.get()
                if (result.resultCode == SessionResult.RESULT_SUCCESS) {
                    _equalizerData.value = result.extras
                }
            } catch (e: Exception) {
                // Ignore
            }
        }, MoreExecutors.directExecutor())
    }

    fun setEqualizerBand(band: Short, level: Short) {
        val player = controller ?: return
        val args = Bundle().apply {
            putShort("band", band)
            putShort("level", level)
        }
        player.sendCustomCommand(SessionCommand(MusicService.COMMAND_SET_EQUALIZER_BAND, Bundle.EMPTY), args)
    }

    private fun updateCurrentSong(mediaItem: MediaItem?) {
        val song = mediaItem?.localConfiguration?.tag as? Song
            ?: playlistIndex[mediaItem?.mediaId]
        _currentSong.value = song
    }
    
    private fun updateQueue() {
        val player = controller ?: return
        val queue = mutableListOf<Song>()
        for (i in 0 until player.mediaItemCount) {
            val item = player.getMediaItemAt(i)
            val song = item.localConfiguration?.tag as? Song
                ?: playlistIndex[item.mediaId]
            song?.let { queue.add(it) }
        }
        _currentQueue.value = queue
    }

    private fun startProgressUpdate() {
        stopProgressUpdate()
        progressJob = scope.launch(Dispatchers.Main) {
            // ── FIX 3: toda la operación read-compare-write en Main ──
            // Antes: pos se leía en Main pero la comparación y el write ocurrían en Default,
            // lo que creaba una condición de carrera no atómica sobre _currentPosition.
            // Ahora: el job corre directamente en Main; la corrutina es ligera (solo lectura
            // de una propiedad y update de StateFlow) y no bloquea el hilo.
            while (isActive) {
                val pos = controller?.currentPosition ?: _currentPosition.value
                if (kotlin.math.abs(pos - _currentPosition.value) > 500) {
                    _currentPosition.value = pos
                }
                delay(1000)
            }
        }
    }

    private fun stopProgressUpdate() {
        progressJob?.cancel()
        progressJob = null
    }

    fun setPlaylist(songs: List<Song>) {
        val player = controller ?: return
        lastPlaylist = songs
        playlistIndex = songs.associateBy { it.id.toString() }
        val newMediaItems = songs.map { song ->
            MediaItem.Builder()
                .setMediaId(song.id.toString())
                .setUri(song.path)
                .setTag(song)
                .build()
        }
        
        if (isPlaylistDifferent(player, newMediaItems)) {
            player.setMediaItems(newMediaItems)
            player.prepare()
        }
    }

    fun addToQueue(song: Song) {
        val player = controller ?: return
        val mediaItem = MediaItem.Builder()
            .setMediaId(song.id.toString())
            .setUri(song.path)
            .setTag(song)
            .build()
        player.addMediaItem(mediaItem)
        playlistIndex = playlistIndex + (song.id.toString() to song)
        if (!player.isPlaying && player.playbackState == Player.STATE_IDLE) {
            player.prepare()
        }
    }

    private fun isPlaylistDifferent(player: Player, newItems: List<MediaItem>): Boolean {
        if (player.mediaItemCount != newItems.size) return true
        for (i in 0 until player.mediaItemCount) {
            if (player.getMediaItemAt(i).mediaId != newItems[i].mediaId) return true
        }
        return false
    }

    fun play(song: Song) {
        val player = controller ?: return
        var index = -1
        for (i in 0 until player.mediaItemCount) {
            if (player.getMediaItemAt(i).mediaId == song.id.toString()) {
                index = i
                break
            }
        }

        if (index != -1) {
            if (player.currentMediaItemIndex != index) {
                player.seekTo(index, 0)
            }
            player.play()
        } else {
            val mediaItem = MediaItem.Builder()
                .setMediaId(song.id.toString())
                .setUri(song.path)
                .setTag(song)
                .build()
            player.addMediaItem(mediaItem)
            player.seekTo(player.mediaItemCount - 1, 0)
            player.prepare()
            player.play()
        }
    }

    fun togglePlayPause() {
        val player = controller ?: return
        if (player.isPlaying) player.pause() else player.play()
    }

    fun pause() {
        controller?.pause()
    }

    /**
     * Fade out de [MusicService.SLEEP_FADE_DURATION_MS] ms y luego pausa.
     * Usado por el sleep timer para no cortar la música de golpe.
     */
    fun fadeAndPause() {
        val player = controller ?: run { pause(); return }
        val args = Bundle.EMPTY
        player.sendCustomCommand(
            SessionCommand(MusicService.COMMAND_FADE_AND_PAUSE, Bundle.EMPTY), args
        )
    }

    fun skipNext() { controller?.seekToNext() }
    fun skipPrevious() { controller?.seekToPrevious() }

    fun setCurrentPosition(position: Long) {
        controller?.seekTo(position)
        _currentPosition.value = position
    }

    fun seekTo(position: Long) {
        controller?.seekTo(position)
        _currentPosition.value = position
    }

    fun seekForward() { controller?.let { it.seekTo(it.currentPosition + 10000) } }
    fun seekBack() { controller?.let { it.seekTo(it.currentPosition - 10000) } }

    fun toggleShuffle() { controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled } }

    fun cycleRepeatMode() {
        val player = controller ?: return
        player.repeatMode = when (player.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun release() {
        reconnectJob?.cancel()
        controller?.let { player ->
            playerListener?.let { player.removeListener(it) }
        }
        playerListener = null
        reconnectListener = null
        stopProgressUpdate()
        scope.cancel()
        controllerFuture?.let { MediaController.releaseFuture(it) }
    }
    
    fun getAudioSessionId(): Int = _audioSessionId.value
}
