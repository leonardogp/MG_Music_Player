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
    val isFavorite: Boolean = false
)