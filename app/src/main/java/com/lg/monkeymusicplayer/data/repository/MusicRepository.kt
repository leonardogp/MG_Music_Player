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
        val allScannedIds = mutableSetOf<Long>()
        try {
            scanner.scan(
                onProgress = onProgress,
                onSongsFound = { songsBatch ->
                    val entities = songsBatch.map { it.toEntity() }
                    musicDao.insertSongs(entities)
                    allScannedIds.addAll(songsBatch.map { it.id })
                }
            )

            // ── FIX 1: diff en Kotlin para evitar SQLiteException con >999 IDs ──
            // "DELETE FROM songs WHERE id NOT IN (:currentIds)" falla si currentIds > ~999
            // porque SQLite limita las variables bind a 999 por query.
            // Solución: obtener los IDs almacenados → calcular huérfanos → borrar solo esos.
            // Los huérfanos (canciones borradas del dispositivo) suelen ser 0 o pocos,
            // por lo que la lista para deleteSongsByIds es siempre pequeña.
            if (allScannedIds.isNotEmpty()) {
                val storedIds = musicDao.getAllIds().toSet()
                val orphanIds = storedIds - allScannedIds
                if (orphanIds.isNotEmpty()) {
                    // deleteSongsByIds usa IN (:ids) con la lista de huérfanos (normalmente pequeña)
                    musicDao.deleteSongsByIds(orphanIds.toList())
                }
            }
        } catch (e: Exception) {
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
        val songIds = musicDao.getSongsInPlaylist(playlistId)
        if (songIds.isEmpty()) return@withContext emptyList()
        musicDao.getSongsByIds(songIds).map { it.toDomainModel() }
    }

    /**
     * Actualiza los tags ID3 de un archivo MP3 y sincroniza Room + MediaStore.
     *
     * Flujo:
     *  1. Resolver la ruta física del archivo desde MediaStore.
     *  2. Copiar al caché, editar tags con mp3agic, copiar de vuelta.
     *  3. Notificar a MediaStore y MediaScanner.
     *  4. Actualizar Room → UI se refresca via Flow.
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

            val tempFile = File(context.cacheDir, "tag_edit_${System.currentTimeMillis()}.mp3")
            val editedFile = File(context.cacheDir, "tag_edit_${System.currentTimeMillis()}_out.mp3")

            try {
                originalFile.copyTo(tempFile, overwrite = true)

                val mp3File = Mp3File(tempFile.absolutePath)
                val tag = if (mp3File.hasId3v2Tag()) mp3File.id3v2Tag else ID3v24Tag()
                tag.title = newTitle
                tag.artist = newArtist
                tag.album = newAlbum
                tag.genreDescription = newGenre
                mp3File.id3v2Tag = tag
                mp3File.save(editedFile.absolutePath)

                editedFile.copyTo(originalFile, overwrite = true)

            } catch (e: Exception) {
                Timber.e(e, "Error writing ID3 tags to file")
            } finally {
                tempFile.delete()
                editedFile.delete()
            }

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

            suspendCoroutine { continuation ->
                MediaScannerConnection.scanFile(
                    context, arrayOf(originalFile.absolutePath), null
                ) { _, _ -> continuation.resume(Unit) }
            }

            musicDao.updateSongTags(song.id, newTitle, newArtist, newAlbum, newGenre)

            Result.Success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "updateSongTags failed")
            Result.Error("Error al guardar los tags: ${e.message ?: "error desconocido"}")
        }
    }

    // ── FIX 4 (soporte): exponer getFilePathFromId como internal ──
    // MusicViewModel lo usa para resolver la ruta real del .lrc
    internal fun getFilePathFromId(songId: Long): String? {
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
