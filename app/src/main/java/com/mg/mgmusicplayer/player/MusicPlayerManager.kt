package com.mg.mgmusicplayer.player

import android.content.Context
import com.mg.mgmusicplayer.data.Song
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem

class MusicPlayerManager(context: Context) {

    private val player = ExoPlayer.Builder(context).build()

    fun play(song: Song) {

        val mediaItem = MediaItem.fromUri(song.path)

        player.setMediaItem(mediaItem)

        player.prepare()

        player.play()
    }

    fun pause() {
        player.pause()
    }

    fun release() {
        player.release()
    }
}