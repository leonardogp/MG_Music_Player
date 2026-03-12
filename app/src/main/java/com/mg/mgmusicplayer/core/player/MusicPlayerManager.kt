package com.mg.mgmusicplayer.core.player

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.mg.mgmusicplayer.data.model.Song
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@UnstableApi
class MusicPlayerManager(private val context: Context) {

    private var exoPlayer: ExoPlayer? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private val controller: MediaController? get() = if (controllerFuture?.isDone == true) controllerFuture?.get() else null

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _isShuffleMode = MutableStateFlow(false)
    val isShuffleMode: StateFlow<Boolean> = _isShuffleMode

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode

    init {
        setupPlayer()
        setupMediaController()
    }

    private fun setupPlayer() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        exoPlayer = ExoPlayer.Builder(context)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()

        exoPlayer?.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val song = mediaItem?.localConfiguration?.tag as? Song
                _currentSong.value = song
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                _isShuffleMode.value = shuffleModeEnabled
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                _repeatMode.value = repeatMode
            }
        })
    }

    private fun setupMediaController() {
        val sessionToken = SessionToken(context, ComponentName(context, MusicService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            // Controller is ready
        }, MoreExecutors.directExecutor())
    }

    fun setPlaylist(songs: List<Song>) {
        val mediaItems = songs.map { song ->
            MediaItem.Builder()
                .setMediaId(song.id.toString())
                .setUri(song.path)
                .setTag(song)
                .build()
        }
        exoPlayer?.setMediaItems(mediaItems)
        exoPlayer?.prepare()
    }

    fun play(song: Song) {
        val timeline = exoPlayer?.currentTimeline ?: return
        val window = Timeline.Window()
        var index = -1
        for (i in 0 until timeline.windowCount) {
            if (timeline.getWindow(i, window).mediaItem.mediaId == song.id.toString()) {
                index = i
                break
            }
        }

        if (index != -1) {
            exoPlayer?.seekTo(index, 0)
            exoPlayer?.play()
        }
    }

    fun togglePlayPause() {
        if (exoPlayer?.isPlaying == true) {
            exoPlayer?.pause()
        } else {
            exoPlayer?.play()
        }
    }

    fun skipNext() {
        exoPlayer?.seekToNext()
    }

    fun skipPrevious() {
        exoPlayer?.seekToPrevious()
    }

    fun seekForward() {
        exoPlayer?.let { it.seekTo(it.currentPosition + 10000) }
    }

    fun seekBack() {
        exoPlayer?.let { it.seekTo(it.currentPosition - 10000) }
    }

    fun toggleShuffle() {
        exoPlayer?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled }
    }

    fun cycleRepeatMode() {
        val nextMode = when (exoPlayer?.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        exoPlayer?.repeatMode = nextMode
    }

    fun release() {
        exoPlayer?.release()
        exoPlayer = null
        controllerFuture?.let { MediaController.releaseFuture(it) }
    }
    
    fun getAudioSessionId(): Int {
        return exoPlayer?.audioSessionId ?: C.AUDIO_SESSION_ID_UNSET
    }
}
