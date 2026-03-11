package com.mg.mgmusicplayer.data.model

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val genre: String,
    val folder: String,
    val path: String
)