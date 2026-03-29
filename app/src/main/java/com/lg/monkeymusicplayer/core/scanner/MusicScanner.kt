package com.lg.monkeymusicplayer.core.scanner

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.lg.monkeymusicplayer.data.model.Song
import java.io.File

class MusicScanner(private val context: Context) {

    suspend fun scan(
        excludedPaths: List<String> = emptyList(),
        onProgress: (Int, Int) -> Unit = { _, _ -> },
        onSongsFound: suspend (List<Song>) -> Unit = {}
    ): List<Song> {
        val songs = mutableListOf<Song>()
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        // ── CORRECCIÓN: pre-cargar TODOS los géneros en un Map antes del loop ──
        // Antes: se hacía 1 query al ContentResolver por cada canción → O(N) queries
        // Ahora: 1 sola query para todos → O(1) lookup por canción
        val genreMap = loadAllGenres()

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

                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                )
                val sArtworkUri = Uri.parse("content://media/external/audio/albumart")
                val albumArtUri = ContentUris.withAppendedId(sArtworkUri, albumId).toString()

                val fullPath = it.getString(dataColumn) ?: ""

                // ── Filtro de rutas excluidas ──
                // Comparamos el path completo contra cada ruta excluida.
                // startsWith cubre tanto la carpeta exacta como sus subcarpetas.
                if (excludedPaths.isNotEmpty() && fullPath.isNotEmpty()) {
                    if (excludedPaths.any { excluded -> fullPath.startsWith(excluded) }) {
                        current++
                        onProgress(current, total)
                        continue
                    }
                }

                val folder = if (fullPath.isNotEmpty()) {
                    File(fullPath).parentFile?.name ?: "Raíz"
                } else {
                    "Desconocida"
                }

                // ── Lookup O(1) en lugar de query individual por cada canción ──
                val genre = genreMap[id] ?: "Sin género"

                val song = Song(
                    id = id,
                    albumId = albumId,
                    title = title,
                    artist = artist,
                    album = album,
                    genre = genre,
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
                }
            }
            if (batch.isNotEmpty()) {
                onSongsFound(batch.toList())
                batch.clear()
                onProgress(current, total)
            }
        }
        return songs
    }

    /**
     * Carga todos los géneros del dispositivo en un Map<songId, genreName>.
     *
     * Estrategia:
     *  1. Obtener todos los géneros con sus IDs.
     *  2. Por cada género, obtener los IDs de las canciones que pertenecen a él.
     *  3. Construir el mapa inverso songId → genreName.
     *
     * Resultado: 1 + N_genres queries en lugar de N_songs queries.
     * En una biblioteca típica: ~20 géneros vs ~1000 canciones → 50x menos queries.
     */
    private fun loadAllGenres(): Map<Long, String> {
        val genreMap = mutableMapOf<Long, String>()

        // Paso 1: obtener todos los géneros disponibles
        val genreUri = MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI
        val genreProjection = arrayOf(
            MediaStore.Audio.Genres._ID,
            MediaStore.Audio.Genres.NAME
        )

        val genreCursor = context.contentResolver.query(
            genreUri, genreProjection, null, null, null
        ) ?: return genreMap

        genreCursor.use { gc ->
            val genreIdCol = gc.getColumnIndexOrThrow(MediaStore.Audio.Genres._ID)
            val genreNameCol = gc.getColumnIndexOrThrow(MediaStore.Audio.Genres.NAME)

            while (gc.moveToNext()) {
                val genreId = gc.getLong(genreIdCol)
                val genreName = gc.getString(genreNameCol) ?: continue

                // Paso 2: obtener las canciones de este género
                val songUri = MediaStore.Audio.Genres.Members.getContentUri("external", genreId)
                val songProjection = arrayOf(MediaStore.Audio.Genres.Members.AUDIO_ID)

                val songCursor = context.contentResolver.query(
                    songUri, songProjection, null, null, null
                ) ?: continue

                songCursor.use { sc ->
                    val audioIdCol = sc.getColumnIndexOrThrow(
                        MediaStore.Audio.Genres.Members.AUDIO_ID
                    )
                    while (sc.moveToNext()) {
                        val songId = sc.getLong(audioIdCol)
                        // Si una canción tiene varios géneros, quedarse con el primero encontrado
                        if (!genreMap.containsKey(songId)) {
                            genreMap[songId] = genreName
                        }
                    }
                }
            }
        }

        return genreMap
    }
}
