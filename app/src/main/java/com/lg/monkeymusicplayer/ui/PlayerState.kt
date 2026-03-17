package com.lg.monkeymusicplayer.ui

import androidx.compose.ui.graphics.Color
import com.lg.monkeymusicplayer.data.model.Song

data class PlayerState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val isShuffleMode: Boolean = false,
    val repeatMode: Int = 0,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val currentQueue: List<Song> = emptyList(),
    val audioSessionId: Int = -1,
    val accentColor: Color = Color(0xFF6200EE), // Default accent
    val sleepTimerMinutes: Int = 0,
    val sleepTimerRemainingMillis: Long = 0L
)
