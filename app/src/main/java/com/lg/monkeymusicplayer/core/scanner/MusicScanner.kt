package com.lg.monkeymusicplayer.core.scanner

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.lg.monkeymusicplayer.data.model.Song
import com.lg.monkeymusicplayer.util.PermissionHelper
import timber.log.Timber
import java.io.File

class MusicScanner(private val context: Context) {

    fun invalidateGenreCache() {
        genreCache = null
        genreCacheTimeMs = 0L
    }

    private var genreCache: Map<Long, String>? = null
    private var genreCacheTimeMs: Long = 0L
    private val GENRE_CACHE_TTL_MS = 5L * 60L * 1000L

    internal fun isPathExcluded(fullPath: String, excludedPaths: List<String>): Boolean {
        val set = excludedPaths.toSet()
        return set.isNotEmpty() && fullPath.isNotEmpty() && set.any { ex -> fullPath.startsWith(ex) }
    }

    suspend fun scan(
        excludedPaths: List<String> = emptyList(),
        existingIds: Set<Long> = emptySet(),
        onProgress: (Int, Int) -> Unit = { _, _ -> },
        onSongsFound: suspend (List<Song>) -> Unit = {}
    ): List<Song> {
        val songs = mutableListOf<Song>()

        val genreMap = loadAllGenres()

        // ReplayGain se lee en background post-scan (ReplayGainWorker).
        // NO se lee aquí porque Mp3File() parsea el archivo completo → demasiado lento.
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DATA
        )

        if (!PermissionHelper.hasReadPermissions(context)) {
            Timber.w("MusicScanner: scan abortado — permisos no concedidos")
            return songs
        }

        val cursor = context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            "${MediaStore.Audio.Media.IS_MUSIC} != 0",
            null, null
        ) ?: run {
            Timber.w("MusicScanner: cursor null")
            return songs
        }

        val excludedSet = excludedPaths.toSet()
        val artworkBase = Uri.parse("content://media/external/audio/albumart")
        val mediaBase   = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        cursor.use { c ->
            val total = c.count
            Timber.d("MusicScanner: $total canciones en MediaStore")

            val colId      = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val colAlbumId = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val colTitle   = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val colArtist  = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val colAlbum   = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val colData    = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

            var current = 0
            var lastProgressAt = 0
            val BATCH_SIZE = 150
            val batch = mutableListOf<Song>()

            while (c.moveToNext()) {
                current++
                val fullPath = c.getString(colData) ?: ""

                if (excludedSet.isNotEmpty() && fullPath.isNotEmpty() &&
                    excludedSet.any { ex -> fullPath.startsWith(ex) }) continue

                val id      = c.getLong(colId)
                val albumId = c.getLong(colAlbumId)

                batch.add(Song(
                    id          = id,
                    albumId     = albumId,
                    title       = c.getString(colTitle)  ?: "Desconocido",
                    artist      = c.getString(colArtist) ?: "Artista Desconocido",
                    album       = c.getString(colAlbum)  ?: "Álbum Desconocido",
                    genre       = genreMap[id] ?: "Sin género",
                    folder      = if (fullPath.isNotEmpty())
                                      File(fullPath).parentFile?.name ?: "Raíz"
                                  else "Desconocida",
                    path        = ContentUris.withAppendedId(mediaBase, id).toString(),
                    albumArtUri = ContentUris.withAppendedId(artworkBase, albumId).toString(),
                    replayGain  = null
                ))

                if (batch.size >= BATCH_SIZE) {
                    songs.addAll(batch)
                    onSongsFound(batch.toList())
                    batch.clear()
                }

                if (current - lastProgressAt >= 50 || current == total) {
                    onProgress(current, total)
                    lastProgressAt = current
                }
            }

            if (batch.isNotEmpty()) {
                songs.addAll(batch)
                onSongsFound(batch.toList())
                onProgress(current, total)
            }
        }
        return songs
    }

    private fun loadAllGenres(): Map<Long, String> {
        val now = System.currentTimeMillis()
        genreCache?.let {
            if (now - genreCacheTimeMs < GENRE_CACHE_TTL_MS) return it
        }
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            loadGenresApi29()
        else
            loadGenresLegacy()
        genreCache = result
        genreCacheTimeMs = now
        return result
    }

    private fun loadGenresApi29(): Map<Long, String> {
        val result = mutableMapOf<Long, String>()
        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.GENRE),
            "${MediaStore.Audio.Media.IS_MUSIC} != 0",
            null, null
        )?.use { c ->
            val colId    = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val colGenre = c.getColumnIndex(MediaStore.Audio.Media.GENRE)
            if (colGenre == -1) return loadGenresLegacy()
            while (c.moveToNext()) {
                val genre = c.getString(colGenre)
                if (!genre.isNullOrBlank()) result[c.getLong(colId)] = genre
            }
        }
        return result
    }

    private fun loadGenresLegacy(): Map<Long, String> {
        val result = mutableMapOf<Long, String>()
        context.contentResolver.query(
            MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Audio.Genres._ID, MediaStore.Audio.Genres.NAME),
            null, null, null
        )?.use { gc ->
            val colId   = gc.getColumnIndexOrThrow(MediaStore.Audio.Genres._ID)
            val colName = gc.getColumnIndexOrThrow(MediaStore.Audio.Genres.NAME)
            while (gc.moveToNext()) {
                val genreId   = gc.getLong(colId)
                val genreName = gc.getString(colName) ?: continue
                context.contentResolver.query(
                    MediaStore.Audio.Genres.Members.getContentUri("external", genreId),
                    arrayOf(MediaStore.Audio.Genres.Members.AUDIO_ID),
                    null, null, null
                )?.use { sc ->
                    val colAudio = sc.getColumnIndexOrThrow(MediaStore.Audio.Genres.Members.AUDIO_ID)
                    while (sc.moveToNext()) {
                        val sid = sc.getLong(colAudio)
                        if (!result.containsKey(sid)) result[sid] = genreName
                    }
                }
            }
        }
        return result
    }
}
