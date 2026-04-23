package com.lg.monkeymusicplayer.core.scanner

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.lg.monkeymusicplayer.data.model.Song
import com.lg.monkeymusicplayer.util.PermissionHelper
import com.lg.monkeymusicplayer.core.player.ReplayGainReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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

        // OPT-1: género en 1 query (Android 10+) o N+1 queries cacheadas (Android 9-)
        val genreMap = loadAllGenres()

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

        // OPT-5: pre-calcular URIs base una sola vez fuera del loop
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
            // OPT-3: batch más grande → menos transacciones Room
            val BATCH_SIZE = 150
            // batch contiene (song, realPath) para poder leer ReplayGain después
            val batch = mutableListOf<Pair<Song, String>>()

            while (c.moveToNext()) {
                current++
                val fullPath = c.getString(colData) ?: ""

                if (excludedSet.isNotEmpty() && fullPath.isNotEmpty() &&
                    excludedSet.any { ex -> fullPath.startsWith(ex) }) continue

                val id      = c.getLong(colId)
                val albumId = c.getLong(colAlbumId)

                val song = Song(
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
                    replayGain  = null  // se rellena en el paso paralelo post-batch
                )

                songs.add(song)
                batch.add(song to fullPath)

                if (batch.size >= BATCH_SIZE) {
                    // OPT-2: leer ReplayGain en paralelo para todo el batch
                    val enriched = enrichBatch(batch, existingIds)
                    onSongsFound(enriched)
                    batch.clear()
                }

                // OPT-4: throttle de progreso cada 50 canciones
                if (current - lastProgressAt >= 50 || current == total) {
                    onProgress(current, total)
                    lastProgressAt = current
                }
            }

            if (batch.isNotEmpty()) {
                val enriched = enrichBatch(batch, existingIds)
                onSongsFound(enriched)
                onProgress(current, total)
            }
        }
        return songs
    }

    /**
     * OPT-2: Lee ReplayGain en paralelo para todo el batch usando coroutines en Dispatchers.IO.
     * Solo procesa canciones nuevas (no estaban en existingIds).
     * Reduce el tiempo de lectura de tags de O(N × t_disco) a ~O(t_disco) por batch.
     */
    private suspend fun enrichBatch(
        batch: List<Pair<Song, String>>,
        existingIds: Set<Long>
    ): List<Song> {
        val newPairs = batch.filter { (song, path) ->
            song.id !in existingIds && path.isNotEmpty()
        }
        if (newPairs.isEmpty()) return batch.map { it.first }

        return coroutineScope {
            val deferred = newPairs.map { (song, path) ->
                async(Dispatchers.IO) {
                    song.id to ReplayGainReader.readTrackGain(path)
                }
            }
            val gainMap = deferred.awaitAll().toMap()
            batch.map { (song, _) ->
                val gain = gainMap[song.id]
                if (gain != null) song.copy(replayGain = gain) else song
            }
        }
    }

    /**
     * OPT-1: Carga géneros.
     * Android 10+ (API 29): columna GENRE inline en la tabla de audio → 1 sola query.
     * Android 9-: queries anidadas por género, pero cacheadas 5 min.
     */
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

    /** Android 10+: una sola query con columna GENRE inline. */
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

    /** Android 9 y anteriores: queries anidadas por género (cacheadas). */
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
