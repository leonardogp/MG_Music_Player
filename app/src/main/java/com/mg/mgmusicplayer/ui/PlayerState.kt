package com.mg.mgmusicplayer.ui

import com.mg.mgmusicplayer.data.model.Song

data class PlayerState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val isShuffleMode: Boolean = false,
    val repeatMode: Int = 0, // Player.REPEAT_MODE_OFF
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val currentQueue: List<Song> = emptyList(),
    val audioSessionId: Int = -1
)
