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

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressJob: Job? = null
    
    private var lastPlaylist = listOf<Song>()

    init {
        setupMediaController()
    }

    private fun setupMediaController() {
        val sessionToken = SessionToken(appContext, ComponentName(appContext, MusicService::class.java))
        controllerFuture = MediaController.Builder(appContext, sessionToken).buildAsync()
        
        controllerFuture?.addListener({
            try {
                val player = controllerFuture?.get() ?: return@addListener
                player.addListener(object : Player.Listener {
                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        updateCurrentSong(mediaItem)
                        _duration.value = player.duration.coerceAtLeast(0L)
                        fetchAudioSessionId()
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
                        }
                    }
                    
                    override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
                        updateQueue()
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
                
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, MoreExecutors.directExecutor())
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

    private fun updateCurrentSong(mediaItem: MediaItem?) {
        val song = mediaItem?.localConfiguration?.tag as? Song 
            ?: lastPlaylist.find { it.id.toString() == mediaItem?.mediaId }
        _currentSong.value = song
    }
    
    private fun updateQueue() {
        val player = controller ?: return
        val queue = mutableListOf<Song>()
        for (i in 0 until player.mediaItemCount) {
            val song = player.getMediaItemAt(i).localConfiguration?.tag as? Song
                ?: lastPlaylist.find { it.id.toString() == player.getMediaItemAt(i).mediaId }
            song?.let { queue.add(it) }
        }
        _currentQueue.value = queue
    }

    private fun startProgressUpdate() {
        stopProgressUpdate()
        progressJob = scope.launch {
            while (isActive) {
                controller?.let {
                    _currentPosition.value = it.currentPosition
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
        stopProgressUpdate()
        scope.cancel()
        controllerFuture?.let { MediaController.releaseFuture(it) }
    }
    
    fun getAudioSessionId(): Int = _audioSessionId.value
}
