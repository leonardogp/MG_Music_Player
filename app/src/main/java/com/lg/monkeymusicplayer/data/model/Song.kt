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
    val isFavorite: Boolean = false,
    val lyricsPath: String? = null
)

data class LyricLine(
    val timeMs: Long,
    val text: String
)
