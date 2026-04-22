package com.lg.monkeymusicplayer.core.scanner

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.lg.monkeymusicplayer.data.model.Song
import com.lg.monkeymusicplayer.util.PermissionHelper
import com.lg.monkeymusicplayer.core.player.ReplayGainReader
import timber.log.Timber
import java.io.File

class MusicScanner(private val context: Context) {

    // Explicit invalidation hook for event-driven cache invalidation
    fun invalidateGenreCache() {
        genreCache = null
        genreCacheTimeMs = 0L
    }

    // Simple in-memory cache for genre lookups to avoid repeated expensive queries
    private var genreCache: MutableMap<Long, String>? = null
    private var genreCacheTimeMs: Long = 0L
    private val GENRE_CACHE_TTL_MS: Long = 5L * 60L * 1000L // 5 minutes

    // Public helper for tests: determine if a given path should be excluded.
    internal fun isPathExcluded(fullPath: String, excludedPaths: List<String>): Boolean {
        val set = excludedPaths.toSet()
        return set.isNotEmpty() && fullPath.isNotEmpty() && set.any { excluded -> fullPath.startsWith(excluded) }
    }

    /**
     * Scans the MediaStore for music.
     * @param existingIds IDs already in the database. Reading ReplayGain will be skipped for these.
     */
    suspend fun scan(
        excludedPaths: List<String> = emptyList(),
        existingIds: Set<Long> = emptySet(),
        onProgress: (Int, Int) -> Unit = { _, _ -> },
        onSongsFound: suspend (List<Song>) -> Unit = {}
    ): List<Song> {
        val songs = mutableListOf<Song>()
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        // Pre-cargar TODOS los géneros en un Map para evitar miles de queries individuales.
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

        if (!PermissionHelper.hasReadPermissions(context)) {
            Timber.w("MusicScanner: scan abortado — permisos de lectura no concedidos")
            return songs
        }

        val cursor = context.contentResolver.query(
            uri,
            projection,
            selection,
            null,
            null
        )

        if (cursor == null) {
            Timber.w("MusicScanner: ContentResolver devolvió cursor null")
            return songs
        }

        val excludedSet = excludedPaths.toSet()
        cursor.use {
            val total = it.count
            Timber.d("MusicScanner: encontradas $total canciones en MediaStore")
            
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

                val fullPath = it.getString(dataColumn) ?: ""

                // Filtro de rutas excluidas
                if (excludedSet.isNotEmpty() && fullPath.isNotEmpty() && excludedSet.any { excluded -> fullPath.startsWith(excluded) }) {
                    onProgress(current, total)
                    continue
                }

                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                )
                val sArtworkUri = Uri.parse("content://media/external/audio/albumart")
                val albumArtUri = ContentUris.withAppendedId(sArtworkUri, albumId).toString()

                val folder = if (fullPath.isNotEmpty()) {
                    File(fullPath).parentFile?.name ?: "Raíz"
                } else {
                    "Desconocida"
                }

                val genre = genreMap[id] ?: "Sin género"

                // OPTIMIZACIÓN: Solo leer ReplayGain si la canción es NUEVA.
                // Leer etiquetas ID3 directamente de miles de archivos es extremadamente lento.
                val isNew = id !in existingIds
                val replayGain = if (isNew && fullPath.isNotEmpty()) {
                    ReplayGainReader.readTrackGain(fullPath)
                } else {
                    null
                }

                val song = Song(
                    id = id,
                    albumId = albumId,
                    title = title,
                    artist = artist,
                    album = album,
                    genre = genre,
                    folder = folder,
                    path = contentUri.toString(),
                    albumArtUri = albumArtUri,
                    replayGain = replayGain
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

    private fun loadAllGenres(): Map<Long, String> {
        val now = System.currentTimeMillis()
        genreCache?.let {
            if (now - genreCacheTimeMs < GENRE_CACHE_TTL_MS) {
                return it
            }
        }
        val genreMap = mutableMapOf<Long, String>()

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
                        if (!genreMap.containsKey(songId)) {
                            genreMap[songId] = genreName
                        }
                    }
                }
            }
        }

        genreCache = genreMap
        genreCacheTimeMs = now
        return genreMap
    }
}
