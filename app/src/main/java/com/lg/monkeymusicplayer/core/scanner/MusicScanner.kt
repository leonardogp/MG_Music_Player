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

        // Guard: en Android 14+ (especialmente Xiaomi HyperOS) el ContentResolver puede
        // devolver cursor vacío silenciosamente si READ_MEDIA_AUDIO no está concedido.
        // Verificar antes de consultar para evitar confundir "sin permisos" con "sin música".
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
            Timber.w("MusicScanner: ContentResolver devolvió cursor null — posible bloqueo de permisos OEM (HyperOS/MIUI)")
            return songs
        }

        // Precompute a Set for excluded paths to improve lookup performance
        val excludedSet = excludedPaths.toSet()
        cursor.use {
            val total = it.count
            Timber.d("MusicScanner: encontradas $total canciones en MediaStore")
            if (total == 0) {
                Timber.w("MusicScanner: MediaStore devuelve 0 canciones — verificar permisos en Ajustes de la app o restricciones del OEM")
            }
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
                if (excludedSet.isNotEmpty() && fullPath.isNotEmpty()) {
                    if (excludedSet.any { excluded -> fullPath.startsWith(excluded) }) {
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

                // ReplayGain: se lee el tag del archivo físico (path real, no content URI).
                // Es una operación IO liviana (~1ms por archivo); se ejecuta en el contexto
                // suspendido del scanner que ya corre en Dispatchers.IO.
                val replayGain = ReplayGainReader.readTrackGain(fullPath)

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
        val now = System.currentTimeMillis()
        genreCache?.let {
            if (now - genreCacheTimeMs < GENRE_CACHE_TTL_MS) {
                return it
            }
        }
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

        // Cache the result for subsequent scans within TTL
        genreCache = genreMap
        genreCacheTimeMs = now
        return genreMap
    }
}
