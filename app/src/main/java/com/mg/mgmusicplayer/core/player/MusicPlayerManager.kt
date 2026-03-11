package com.mg.mgmusicplayer.core.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.mg.mgmusicplayer.data.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MusicPlayerManager(context: Context) {

    private val player = ExoPlayer.Builder(context).build()
    
    private var playlist: List<Song> = emptyList()
    
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _isShuffleMode = MutableStateFlow(false)
    val isShuffleMode = _isShuffleMode.asStateFlow()

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val index = player.currentMediaItemIndex
                if (index >= 0 && index < playlist.size) {
                    _currentSong.value = playlist[index]
                }
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                _isShuffleMode.value = shuffleModeEnabled
            }
        })
    }

    fun setPlaylist(songs: List<Song>) {
        playlist = songs
        player.setMediaItems(songs.map { MediaItem.fromUri(it.path) })
        player.prepare()
    }

    fun play(song: Song) {
        val index = playlist.indexOf(song)
        if (index != -1) {
            player.seekTo(index, 0)
            player.play()
        } else {
            setPlaylist(listOf(song))
            player.play()
        }
    }

    fun togglePlayPause() {
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    fun skipNext() {
        if (player.hasNextMediaItem()) {
            player.seekToNext()
        }
    }

    fun skipPrevious() {
        if (player.hasPreviousMediaItem()) {
            player.seekToPrevious()
        }
    }

    fun toggleShuffle() {
        player.shuffleModeEnabled = !player.shuffleModeEnabled
    }
    
    fun release() {
        player.release()
    }
}