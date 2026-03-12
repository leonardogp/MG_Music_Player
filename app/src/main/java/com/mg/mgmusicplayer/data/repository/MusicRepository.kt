package com.mg.mgmusicplayer.data.repository

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.provider.MediaStore
import com.mg.mgmusicplayer.core.scanner.MusicScanner
import com.mg.mgmusicplayer.data.database.FavoriteEntity
import com.mg.mgmusicplayer.data.database.HistoryEntity
import com.mg.mgmusicplayer.data.database.MusicDao
import com.mg.mgmusicplayer.data.database.PlaylistEntity
import com.mg.mgmusicplayer.data.database.PlaylistSongCrossRef
import com.mg.mgmusicplayer.data.model.Song
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

    suspend fun getSongs(): List<Song> = withContext(Dispatchers.IO) {
        scanner.scan()
    }

    val favorites: Flow<List<Long>> = musicDao.getFavorites()
    val history: Flow<List<HistoryEntity>> = musicDao.getHistory()
    val playlists: Flow<List<PlaylistEntity>> = musicDao.getPlaylists()

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

    suspend fun addSongToPlaylist(playlistId: Long, songId: Long) {
        musicDao.addSongToPlaylist(PlaylistSongCrossRef(playlistId, songId))
    }

    suspend fun addSongsToPlaylist(playlistId: Long, songs: List<Song>) = withContext(Dispatchers.IO) {
        val crossRefs = songs.map { PlaylistSongCrossRef(playlistId, it.id) }
        musicDao.addSongsToPlaylist(crossRefs)
    }

    suspend fun getSongsInPlaylist(playlistId: Long): List<Song> = withContext(Dispatchers.IO) {
        val songIds = musicDao.getSongsInPlaylist(playlistId)
        val allSongs = getSongs()
        allSongs.filter { songIds.contains(it.id) }
    }

    suspend fun updateSongTags(song: Song, newTitle: String, newArtist: String, newAlbum: String, newGenre: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val filePath = getFilePathFromId(song.id)
            if (filePath != null) {
                val file = File(filePath)
                if (file.exists()) {
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
                            return@withContext false
                        }
                    }
                    
                    // Force MediaStore update by scanning the file
                    suspendCoroutine { continuation ->
                        MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), null) { _, _ ->
                            continuation.resume(Unit)
                        }
                    }
                    
                    // Also try direct MediaStore update as fallback/complement
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
}
