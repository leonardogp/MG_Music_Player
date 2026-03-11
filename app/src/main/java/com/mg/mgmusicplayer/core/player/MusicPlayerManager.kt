package com.mg.mgmusicplayer.core.player

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.mg.mgmusicplayer.data.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MusicPlayerManager(context: Context) {

    private var controllerFuture: ListenableFuture<MediaController>
    private var player: Player? = null
    
    private var playlist: List<Song> = emptyList()
    
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _isShuffleMode = MutableStateFlow(false)
    val isShuffleMode = _isShuffleMode.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode = _repeatMode.asStateFlow()

    init {
        val sessionToken = SessionToken(context, ComponentName(context, MusicService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture.addListener({
            player = controllerFuture.get()
            player?.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _isPlaying.value = isPlaying
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    val index = player?.currentMediaItemIndex ?: -1
                    if (index >= 0 && index < playlist.size) {
                        _currentSong.value = playlist[index]
                    }
                }

                override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                    _isShuffleMode.value = shuffleModeEnabled
                }

                override fun onRepeatModeChanged(repeatMode: Int) {
                    _repeatMode.value = repeatMode
                }
            })
        }, MoreExecutors.directExecutor())
    }

    fun setPlaylist(songs: List<Song>) {
        playlist = songs
        player?.setMediaItems(songs.map { MediaItem.fromUri(it.path) })
        player?.prepare()
    }

    fun play(song: Song) {
        val index = playlist.indexOf(song)
        if (index != -1) {
            player?.seekTo(index, 0)
            player?.play()
        } else {
            setPlaylist(listOf(song))
            player?.play()
        }
    }

    fun togglePlayPause() {
        if (player?.isPlaying == true) {
            player?.pause()
        } else {
            player?.play()
        }
    }

    fun skipNext() {
        if (player?.hasNextMediaItem() == true) {
            player?.seekToNext()
        }
    }

    fun skipPrevious() {
        if (player?.hasPreviousMediaItem() == true) {
            player?.seekToPrevious()
        }
    }

    fun seekForward() {
        player?.let { it.seekTo(it.currentPosition + 10000) }
    }

    fun seekBack() {
        player?.let { it.seekTo(it.currentPosition - 10000) }
    }

    fun toggleShuffle() {
        player?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled }
    }

    fun cycleRepeatMode() {
        player?.let {
            val nextMode = when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
            it.repeatMode = nextMode
        }
    }
    
    fun release() {
        MediaController.releaseFuture(controllerFuture)
    }
}