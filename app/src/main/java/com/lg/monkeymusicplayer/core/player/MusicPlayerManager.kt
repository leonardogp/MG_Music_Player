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
import com.lg.monkeymusicplayer.data.model.Song
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.abs

@UnstableApi
class MusicPlayerManager(context: Context) {

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

    // Referencia al listener para poder removerlo explícitamente en release()
    private var playerListener: Player.Listener? = null

    init {
        setupMediaController()
    }

    private fun setupMediaController() {
        val sessionToken = SessionToken(appContext, ComponentName(appContext, MusicService::class.java))
        controllerFuture = MediaController.Builder(appContext, sessionToken).buildAsync()
        
        controllerFuture?.addListener({
            try {
                val player = controllerFuture?.get() ?: return@addListener

                playerListener = object : Player.Listener {
                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        updateCurrentSong(mediaItem)
                        _duration.value = player.duration.coerceAtLeast(0L)
                        fetchAudioSessionId()
                        fetchEqualizerData()
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _isPlaying.value = isPlaying
                        if (isPlaying) {
                            startProgressUpdate()
                            fetchAudioSessionId()
                        } else {
                            stopProgressUpdate()
                        }
                    }

                    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                        _isShuffleMode.value = shuffleModeEnabled
                    }

                    override fun onRepeatModeChanged(repeatMode: Int) {
                        _repeatMode.value = repeatMode
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        _playbackState.value = playbackState
                        if (playbackState == Player.STATE_READY) {
                            _duration.value = player.duration.coerceAtLeast(0L)
                            fetchAudioSessionId()
                            fetchEqualizerData()
                        }
                    }
                    
                    override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
                        updateQueue()
                    }
                }

                player.addListener(playerListener!!)

                // ── PUNTO 2: reconexión si el servicio es matado por el sistema ──
                // Si Android destruye MusicService por presión de memoria, el player
                // entra en STATE_IDLE mientras _isPlaying era true. Sin reconexión,
                // los botones de reproducción quedan mudos hasta reiniciar la app.
                player.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_IDLE && _isPlaying.value) {
                            scope.launch(Dispatchers.Main) {
                                stopProgressUpdate()
                                _isPlaying.value = false
                                delay(1000) // Pausa breve para evitar bucle de reconexión
                                reconnect()
                            }
                        }
                    }
                })

                // Initial state sync
                _isPlaying.value = player.isPlaying
                _playbackState.value = player.playbackState
                _isShuffleMode.value = player.shuffleModeEnabled
                _repeatMode.value = player.repeatMode
                updateCurrentSong(player.currentMediaItem)
                _duration.value = player.duration.coerceAtLeast(0L)
                updateQueue()
                if (player.isPlaying) startProgressUpdate()
                
                fetchAudioSessionId()
                fetchEqualizerData()
                
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, MoreExecutors.directExecutor())
    }

    private fun reconnect() {
        // Limpiar el listener y el future anterior antes de reinicializar
        controller?.let { player -> playerListener?.let { player.removeListener(it) } }
        playerListener = null
        controllerFuture?.let { MediaController.releaseFuture(it) }
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
        fetchEqualizerData() // Refresh
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
        progressJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                val pos = withContext(Dispatchers.Main) {
                    controller?.currentPosition ?: _currentPosition.value
                }
                if (abs(pos - _currentPosition.value) > 500) {
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

    fun skipNext() { controller?.seekToNext() }
    fun skipPrevious() { controller?.seekToPrevious() }

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
        controller?.let { player ->
            playerListener?.let { player.removeListener(it) }
        }
        playerListener = null
        stopProgressUpdate()
        scope.cancel()
        controllerFuture?.let { MediaController.releaseFuture(it) }
    }
    
    fun getAudioSessionId(): Int = _audioSessionId.value
}
