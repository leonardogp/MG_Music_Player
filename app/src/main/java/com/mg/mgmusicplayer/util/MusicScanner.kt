package com.mg.mgmusicplayer.util

import android.content.Context
import android.provider.MediaStore
import com.mg.mgmusicplayer.data.Song

object MusicScanner {

    fun getSongs(context: Context): List<Song> {

        val songs = mutableListOf<Song>()

        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DATA
        )

        val cursor = context.contentResolver.query(
            uri,
            projection,
            null,
            null,
            null
        )

        cursor?.use {

            val idIndex = it.getColumnIndex(MediaStore.Audio.Media._ID)
            val titleIndex = it.getColumnIndex(MediaStore.Audio.Media.TITLE)
            val artistIndex = it.getColumnIndex(MediaStore.Audio.Media.ARTIST)
            val dataIndex = it.getColumnIndex(MediaStore.Audio.Media.DATA)

            while (it.moveToNext()) {

                val song = Song(
                    it.getLong(idIndex),
                    it.getString(titleIndex),
                    it.getString(artistIndex),
                    it.getString(dataIndex)
                )

                songs.add(song)
            }
        }

        return songs
    }
}