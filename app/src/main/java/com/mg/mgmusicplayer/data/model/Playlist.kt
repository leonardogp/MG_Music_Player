package com.mg.mgmusicplayer.data.model

data class Playlist(
    val id: String,
    val name: String,
    val songs: List<Song> = emptyList()
)