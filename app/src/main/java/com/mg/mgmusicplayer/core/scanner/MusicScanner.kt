package com.mg.mgmusicplayer.core.scanner

import android.content.Context
import android.provider.MediaStore
import com.mg.mgmusicplayer.data.model.Song
import java.io.File

class MusicScanner(private val context: Context) {

    fun scan(): List<Song> {
        val songs = mutableListOf<Song>()
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.IS_MUSIC
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        val cursor = context.contentResolver.query(
            uri,
            projection,
            selection,
            null,
            null
        )

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val albumIdColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val titleColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val dataColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val albumId = it.getLong(albumIdColumn)
                val title = it.getString(titleColumn) ?: "Desconocido"
                val artist = it.getString(artistColumn) ?: "Artista Desconocido"
                val album = it.getString(albumColumn) ?: "Álbum Desconocido"
                val fullPath = it.getString(dataColumn) ?: ""
                
                val folder = if (fullPath.isNotEmpty()) {
                    File(fullPath).parentFile?.name ?: "Raíz"
                } else {
                    "Desconocida"
                }

                val genre = getGenreForSong(id)

                val contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                    .buildUpon()
                    .appendPath(id.toString())
                    .build()

                songs.add(
                    Song(
                        id = id,
                        albumId = albumId,
                        title = title,
                        artist = artist,
                        album = album,
                        genre = genre,
                        folder = folder,
                        path = contentUri.toString()
                    )
                )
            }
        }
        return songs
    }

    private fun getGenreForSong(songId: Long): String {
        val uri = MediaStore.Audio.Genres.getContentUriForAudioId("external", songId.toInt())
        val projection = arrayOf(MediaStore.Audio.Genres.NAME)
        
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getString(0) ?: "Sin género"
            }
        }
        return "Sin género"
    }
}