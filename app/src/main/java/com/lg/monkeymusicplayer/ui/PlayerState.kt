package com.lg.monkeymusicplayer.ui

import androidx.compose.ui.graphics.Color
import com.lg.monkeymusicplayer.data.model.Song
import com.lg.monkeymusicplayer.data.model.LyricLine

data class PlayerState(
    val currentSong: Song? = null,
    val lastPlayedSong: Song? = null,   // última canción del historial; visible en idle state
    val isPlaying: Boolean = false,
    val isShuffleMode: Boolean = false,
    val repeatMode: Int = 0,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val currentQueue: List<Song> = emptyList(),
    val audioSessionId: Int = -1,
    val accentColor: Color = Color(0xFF6200EE),
    val lyrics: List<LyricLine> = emptyList(),
    val sleepTimerMinutes: Int = 0,
    val sleepTimerRemainingMillis: Long = 0L,
    val isFavorite: Boolean = false
) {
    object RepeatMode {
        const val NONE = 0
        const val ALL = 1
        const val ONE = 2
    }
}
