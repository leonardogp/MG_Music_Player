package com.lg.monkeymusicplayer.core.scanner

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.lg.monkeymusicplayer.data.model.Song
import java.io.File

class MusicScanner(private val context: Context) {

    suspend fun scan(
        onProgress: (Int, Int) -> Unit = { _, _ -> }, 
        onSongsFound: suspend (List<Song>) -> Unit = {}
    ): List<Song> {
        val songs = mutableListOf<Song>()
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.IS_MUSIC,
            MediaStore.Audio.Media.DATA
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
            val total = it.count
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val albumIdColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val titleColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val dataColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

            var current = 0
            val batch = mutableListOf<Song>()
            
            while (it.moveToNext()) {
                current++
                
                val id = it.getLong(idColumn)
                val albumId = it.getLong(albumIdColumn)
                val title = it.getString(titleColumn) ?: "Desconocido"
                val artist = it.getString(artistColumn) ?: "Artista Desconocido"
                val album = it.getString(albumColumn) ?: "Álbum Desconocido"
                
                val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                val sArtworkUri = Uri.parse("content://media/external/audio/albumart")
                val albumArtUri = ContentUris.withAppendedId(sArtworkUri, albumId).toString()

                val fullPath = it.getString(dataColumn) ?: ""
                val folder = if (fullPath.isNotEmpty()) {
                    File(fullPath).parentFile?.name ?: "Raíz"
                } else {
                    "Desconocida"
                }

                val song = Song(
                    id = id,
                    albumId = albumId,
                    title = title,
                    artist = artist,
                    album = album,
                    genre = "Sin género",
                    folder = folder,
                    path = contentUri.toString(),
                    albumArtUri = albumArtUri
                )
                
                songs.add(song)
                batch.add(song)
                
                if (batch.size >= 50) {
                    onSongsFound(batch.toList())
                    batch.clear()
                    onProgress(current, total)
                } else if (current == total) {
                    if (batch.isNotEmpty()) {
                        onSongsFound(batch.toList())
                        batch.clear()
                    }
                    onProgress(current, total)
                }
            }
        }
        return songs
    }
}
