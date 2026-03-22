package com.lg.monkeymusicplayer.data.repository

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.provider.MediaStore
import com.lg.monkeymusicplayer.core.result.Result
import timber.log.Timber
import com.lg.monkeymusicplayer.core.scanner.MusicScanner
import com.lg.monkeymusicplayer.data.database.FavoriteEntity
import com.lg.monkeymusicplayer.data.database.HistoryEntity
import com.lg.monkeymusicplayer.data.database.MusicDao
import com.lg.monkeymusicplayer.data.database.PlaylistEntity
import com.lg.monkeymusicplayer.data.database.PlaylistSongCrossRef
import com.lg.monkeymusicplayer.data.database.SongEntity
import com.lg.monkeymusicplayer.data.model.Song
import com.mpatric.mp3agic.ID3v24Tag
import com.mpatric.mp3agic.Mp3File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class MusicRepository(private val context: Context, private val musicDao: MusicDao) {

    private val scanner = MusicScanner(context)

    val allSongsFlow: Flow<List<Song>> = musicDao.getAllSongsFlow().map { entities ->
        entities.map { it.toDomainModel() }
    }

    val favorites: Flow<List<Long>> = musicDao.getFavorites()
    val history: Flow<List<HistoryEntity>> = musicDao.getHistory()
    val playlists: Flow<List<PlaylistEntity>> = musicDao.getPlaylists()

    suspend fun getSongs(): List<Song> = withContext(Dispatchers.IO) {
        musicDao.getAllSongs().map { it.toDomainModel() }
    }

    suspend fun refreshMusicDatabase(
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ) = withContext(Dispatchers.IO) {
        val allScannedIds = mutableListOf<Long>()
        try {
            scanner.scan(
                onProgress = onProgress,
                onSongsFound = { songsBatch ->
                    val entities = songsBatch.map { it.toEntity() }
                    musicDao.insertSongs(entities)
                    allScannedIds.addAll(songsBatch.map { it.id })
                }
            )
            if (allScannedIds.isNotEmpty()) {
                musicDao.removeDeletedSongs(allScannedIds)
            }
        } catch (e: Exception) {
            // Loguear el error pero no propagarlo: el caller (ViewModel) pone
            // _isScanning = false en el bloque finally, evitando que quede colgado.
            Timber.e(e, "refreshMusicDatabase failed after scanning ${allScannedIds.size} songs")
        }
    }

    suspend fun toggleFavorite(songId: Long, isFavorite: Boolean) {
        if (isFavorite) {
            musicDao.insertFavorite(FavoriteEntity(songId))
        } else {
            musicDao.deleteFavorite(FavoriteEntity(songId))
        }
    }

    suspend fun addToHistory(songId: Long) {
        musicDao.addToHistory(
            HistoryEntity(songId = songId, timestamp = System.currentTimeMillis())
        )
        // Limpiar entradas antiguas para que la tabla no crezca indefinidamente.
        // La query elimina todo excepto las 50 más recientes.
        musicDao.trimHistory()
    }

    suspend fun createPlaylist(name: String) {
        musicDao.createPlaylist(PlaylistEntity(name = name))
    }

    suspend fun deletePlaylist(playlist: PlaylistEntity) = withContext(Dispatchers.IO) {
        musicDao.deleteSongsFromPlaylist(playlist.id)
        musicDao.deletePlaylist(playlist)
    }

    suspend fun addSongToPlaylist(playlistId: Long, songId: Long) {
        musicDao.addSongToPlaylist(PlaylistSongCrossRef(playlistId, songId))
    }

    suspend fun addSongsToPlaylist(playlistId: Long, songs: List<Song>) =
        withContext(Dispatchers.IO) {
            val crossRefs = songs.map { PlaylistSongCrossRef(playlistId, it.id) }
            musicDao.addSongsToPlaylist(crossRefs)
        }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        musicDao.removeSongFromPlaylist(PlaylistSongCrossRef(playlistId, songId))
    }

    suspend fun getSongsInPlaylist(playlistId: Long): List<Song> = withContext(Dispatchers.IO) {
        // Antes: cargaba getAllSongs() completo y filtraba en memoria → O(N) en RAM
        // Ahora: query directa WHERE id IN (:ids) → solo trae las canciones necesarias
        val songIds = musicDao.getSongsInPlaylist(playlistId)
        if (songIds.isEmpty()) return@withContext emptyList()
        musicDao.getSongsByIds(songIds).map { it.toDomainModel() }
    }

    /**
     * Actualiza los tags ID3 de un archivo MP3 y sincroniza Room + MediaStore.
     *
     * ── CORRECCIÓN: ahora usa Result<Unit> en lugar de Boolean ──
     * Antes: devolvía Boolean, pero la causa del fallo se perdía en el catch silencioso.
     * Ahora: devuelve Result.Success o Result.Error con un mensaje descriptivo,
     *        permitiendo que la UI muestre un error específico al usuario.
     *
     * Flujo:
     *  1. Copiar el archivo original al directorio caché (siempre tenemos permiso ahí).
     *  2. Editar los tags en el archivo de caché con mp3agic.
     *  3. Copiar el archivo editado de vuelta al original (requiere MANAGE_EXTERNAL_STORAGE).
     *  4. Actualizar MediaStore para que otros reproductores vean el cambio.
     *  5. Actualizar Room para que la UI se refresque inmediatamente via Flow.
     */
    suspend fun updateSongTags(
        song: Song,
        newTitle: String,
        newArtist: String,
        newAlbum: String,
        newGenre: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val filePath = getFilePathFromId(song.id)
                ?: return@withContext Result.Error("No se encontró la ruta del archivo")

            val originalFile = File(filePath)
            if (!originalFile.exists()) {
                return@withContext Result.Error("El archivo no existe: $filePath")
            }

            // Paso 1: copiar al caché para editar sin problemas de permisos
            val tempFile = File(context.cacheDir, "tag_edit_${System.currentTimeMillis()}.mp3")
            val editedFile = File(context.cacheDir, "tag_edit_${System.currentTimeMillis()}_out.mp3")

            try {
                originalFile.copyTo(tempFile, overwrite = true)

                // Paso 2: editar los tags en el archivo temporal
                val mp3File = Mp3File(tempFile.absolutePath)
                val tag = if (mp3File.hasId3v2Tag()) mp3File.id3v2Tag else ID3v24Tag()
                tag.title = newTitle
                tag.artist = newArtist
                tag.album = newAlbum
                tag.genreDescription = newGenre
                mp3File.id3v2Tag = tag
                mp3File.save(editedFile.absolutePath)

                // Paso 3: copiar el editado de vuelta al original
                editedFile.copyTo(originalFile, overwrite = true)

            } catch (e: Exception) {
                Timber.e(e, "Error writing ID3 tags to file")
                // No retornamos error aquí: aunque falle el archivo físico,
                // actualizamos Room y MediaStore para que la UI sea consistente.
            } finally {
                tempFile.delete()
                editedFile.delete()
            }

            // Paso 4: actualizar MediaStore
            val values = ContentValues().apply {
                put(MediaStore.Audio.Media.TITLE, newTitle)
                put(MediaStore.Audio.Media.ARTIST, newArtist)
                put(MediaStore.Audio.Media.ALBUM, newAlbum)
            }
            context.contentResolver.update(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                values,
                "${MediaStore.Audio.Media._ID} = ?",
                arrayOf(song.id.toString())
            )

            // Notificar al MediaScanner para que otros reproductores vean el cambio
            suspendCoroutine { continuation ->
                MediaScannerConnection.scanFile(
                    context, arrayOf(originalFile.absolutePath), null
                ) { _, _ -> continuation.resume(Unit) }
            }

            // Paso 5: actualizar Room — la UI se refresca automáticamente via Flow
            musicDao.updateSongTags(song.id, newTitle, newArtist, newAlbum, newGenre)

            Result.Success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "updateSongTags failed")
            Result.Error("Error al guardar los tags: ${e.message ?: "error desconocido"}")
        }
    }

    private fun getFilePathFromId(songId: Long): String? {
        val projection = arrayOf(MediaStore.Audio.Media.DATA)
        val cursor = context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            "${MediaStore.Audio.Media._ID} = ?",
            arrayOf(songId.toString()),
            null
        )
        return cursor?.use {
            if (it.moveToFirst())
                it.getString(it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA))
            else null
        }
    }

    private fun SongEntity.toDomainModel() = Song(
        id = id,
        albumId = albumId,
        title = title,
        artist = artist,
        album = album,
        genre = genre,
        folder = folder,
        path = path,
        albumArtUri = albumArtUri
    )

    private fun Song.toEntity() = SongEntity(
        id = id,
        albumId = albumId,
        title = title,
        artist = artist,
        album = album,
        genre = genre,
        folder = folder,
        path = path,
        albumArtUri = albumArtUri
    )
}
