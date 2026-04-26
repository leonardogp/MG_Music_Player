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
import timber.log.Timber
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.pow

@UnstableApi
class MusicPlayerManager(
    context: Context,
    private val statTracker: StatTracker,
    private val featureGate: com.lg.monkeymusicplayer.core.feature.FeatureGate
) {

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

    private var playlistIndex = mapOf<String, Song>()

    private var playerListener: Player.Listener? = null

    private var reconnectListener: MediaController.Listener? = null

    init {
        setupMediaController()
    }

    private var reconnectAttempts = 0
    private var reconnectJob: Job? = null

    private fun setupMediaController() {
        val sessionToken = SessionToken(appContext, ComponentName(appContext, MusicService::class.java))
        
        val mcl = object : MediaController.Listener {
            override fun onDisconnected(controller: MediaController) {
                scheduleReconnect()
            }
        }
        reconnectListener = mcl

        controllerFuture = MediaController.Builder(appContext, sessionToken)
            .setListener(mcl)
            .buildAsync()

        controllerFuture?.addListener({
            try {
                val mediaController = controllerFuture?.get() ?: run {
                    scheduleReconnect()
                    return@addListener
                }
                reconnectAttempts = 0

                var transitionHandledPlay = false

                playerListener = object : Player.Listener {
                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        val prevSong = _currentSong.value
                        val posMs    = mediaController.currentPosition
                        val durMs    = mediaController.duration.coerceAtLeast(0L)

                        if (prevSong != null && durMs > 0L) {
                            when (reason) {
                                Player.MEDIA_ITEM_TRANSITION_REASON_SEEK -> {
                                    statTracker.onSkip(prevSong.id, posMs, durMs)
                                }
                                Player.MEDIA_ITEM_TRANSITION_REASON_AUTO -> {
                                    val ratio = if (durMs > 0L) posMs.toFloat() / durMs else 0f
                                    if (ratio >= StatTracker.COMPLETE_THRESHOLD_PERCENT) {
                                        statTracker.onSongComplete(prevSong.id, durMs)
                                    } else {
                                        statTracker.onSkip(prevSong.id, posMs, durMs)
                                    }
                                }
                                else -> { /* no-op */ }
                            }
                        }

                        updateCurrentSong(mediaItem)
                        _duration.value = mediaController.duration.coerceAtLeast(0L)
                        fetchAudioSessionId()
                        applyReplayGain(mediaController,
                            (mediaItem?.localConfiguration?.tag as? Song)?.replayGain)

                        val newSong = mediaItem?.localConfiguration?.tag as? Song
                        if (newSong != null) {
                            transitionHandledPlay = true
                            if (mediaController.isPlaying) {
                                statTracker.onPlayStarted(newSong.id)
                            }
                        }
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        val song = _currentSong.value
                        if (isPlaying) {
                            startProgressUpdate()
                            fetchAudioSessionId()
                            if (song != null && !transitionHandledPlay) {
                                statTracker.onPlayStarted(song.id)
                            }
                            transitionHandledPlay = false
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
                            _duration.value = mediaController.duration.coerceAtLeast(0L)
                            fetchAudioSessionId()
                        } else if (playbackState == Player.STATE_ENDED) {
                            val song = _currentSong.value
                            val dur  = mediaController.duration.coerceAtLeast(0L)
                            val pos  = mediaController.currentPosition
                            if (song != null && dur > 0L) {
                                val ratio = pos.toFloat() / dur
                                if (ratio >= StatTracker.COMPLETE_THRESHOLD_PERCENT) {
                                    statTracker.onSongComplete(song.id, dur)
                                }
                            }
                        }
                        _playbackState.value = playbackState
                    }

                    override fun onTimelineChanged(
                        timeline: androidx.media3.common.Timeline, reason: Int
                    ) { updateQueue() }
                }
                playerListener?.let { mediaController.addListener(it) }

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
                Timber.e(e, "setupMediaController: exception while configuring listener")
                scheduleReconnect()
            }
        }, MoreExecutors.directExecutor())
    }

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

    private fun applyReplayGain(controller: MediaController, gainDb: Float?) {
        if (!featureGate.isUnlocked(com.lg.monkeymusicplayer.core.feature.Feature.REPLAY_GAIN)) {
            controller.volume = 1f
            return
        }
        val volume = if (gainDb != null) {
            10f.pow(gainDb / 20f).coerceIn(0f, 1f)
        } else {
            1f
        }
        controller.volume = volume
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

    fun setShuffleMode(enabled: Boolean) {
        controller?.shuffleModeEnabled = enabled
    }

    fun setRepeatMode(mode: Int) {
        controller?.repeatMode = mode
    }

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
