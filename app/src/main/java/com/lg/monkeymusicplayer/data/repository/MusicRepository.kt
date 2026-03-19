package com.lg.monkeymusicplayer.data.repository

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.provider.MediaStore
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

    // Flow reactivo desde Room para las canciones
    val allSongsFlow: Flow<List<Song>> = musicDao.getAllSongsFlow().map { entities ->
        entities.map { it.toDomainModel() }
    }

    val favorites: Flow<List<Long>> = musicDao.getFavorites()
    val history: Flow<List<HistoryEntity>> = musicDao.getHistory()
    val playlists: Flow<List<PlaylistEntity>> = musicDao.getPlaylists()

    suspend fun getSongs(): List<Song> = withContext(Dispatchers.IO) {
        musicDao.getAllSongs().map { it.toDomainModel() }
    }

    suspend fun refreshMusicDatabase(onProgress: (Int, Int) -> Unit = { _, _ -> }) = withContext(Dispatchers.IO) {
        val allScannedIds = mutableListOf<Long>()
        
        scanner.scan(
            onProgress = onProgress,
            onSongsFound = { songsBatch ->
                val entities = songsBatch.map { it.toEntity() }
                // Insertar en la base de datos de forma incremental
                // Esto disparará el Flow allSongsFlow y actualizará la UI poco a poco
                musicDao.insertSongs(entities)
                allScannedIds.addAll(songsBatch.map { it.id })
            }
        )
        
        // Limpiar canciones que ya no existen en el dispositivo
        if (allScannedIds.isNotEmpty()) {
            musicDao.removeDeletedSongs(allScannedIds)
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
        musicDao.addToHistory(HistoryEntity(songId = songId, timestamp = System.currentTimeMillis()))
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

    suspend fun addSongsToPlaylist(playlistId: Long, songs: List<Song>) = withContext(Dispatchers.IO) {
        val crossRefs = songs.map { PlaylistSongCrossRef(playlistId, it.id) }
        musicDao.addSongsToPlaylist(crossRefs)
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        musicDao.removeSongFromPlaylist(PlaylistSongCrossRef(playlistId, songId))
    }

    suspend fun getSongsInPlaylist(playlistId: Long): List<Song> = withContext(Dispatchers.IO) {
        val songIds = musicDao.getSongsInPlaylist(playlistId)
        val allSongs = musicDao.getAllSongs()
        allSongs.filter { songIds.contains(it.id) }.map { it.toDomainModel() }
    }

    suspend fun updateSongTags(song: Song, newTitle: String, newArtist: String, newAlbum: String, newGenre: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val filePath = getFilePathFromId(song.id)
            if (filePath != null) {
                val file = File(filePath)
                if (file.exists()) {
                    // 1. Actualizar el archivo físico (si es MP3)
                    try {
                        val mp3file = Mp3File(file.absolutePath)
                        val id3v2Tag = if (mp3file.hasId3v2Tag()) mp3file.id3v2Tag else ID3v24Tag()
                        id3v2Tag.title = newTitle
                        id3v2Tag.artist = newArtist
                        id3v2Tag.album = newAlbum
                        id3v2Tag.genreDescription = newGenre
                        mp3file.id3v2Tag = id3v2Tag
                        
                        val tempPath = file.absolutePath + ".tmp"
                        mp3file.save(tempPath)
                        val tempFile = File(tempPath)
                        if (tempFile.exists()) {
                            if (file.delete()) {
                                tempFile.renameTo(file)
                            } else {
                                tempFile.delete()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        // Continuamos aunque falle el ID3 para al menos actualizar la DB y MediaStore
                    }
                    
                    // 2. Notificar al sistema (MediaStore)
                    suspendCoroutine { continuation ->
                        MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), null) { _, _ ->
                            continuation.resume(Unit)
                        }
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
                    
                    // 3. Actualizar la base de datos local de Room inmediatamente
                    musicDao.updateSongTags(song.id, newTitle, newArtist, newAlbum, newGenre)
                    
                    return@withContext true
                }
            }
            false
        } catch (e: Exception) {
            e.printStackTrace()
            false
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
            if (it.moveToFirst()) it.getString(it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)) else null
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
