package com.lg.monkeymusicplayer.data.model

data class Song(
    val id: Long,
    val albumId: Long,
    val title: String,
    val artist: String,
    val album: String,
    val genre: String,
    val folder: String,
    val path: String,
    val albumArtUri: String,
    val duration: Long = 0L,
    val dateAdded: Long = 0L,
    val isFavorite: Boolean = false,
    val lyricsPath: String? = null,
    /** ReplayGain track gain en dB. Null si el archivo no tiene el tag. */
    val replayGain: Float? = null
)

data class LyricLine(
    val timeMs: Long,
    val text: String
)
