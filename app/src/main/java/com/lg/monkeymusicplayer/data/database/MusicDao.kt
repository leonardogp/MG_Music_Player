package com.lg.monkeymusicplayer.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MusicDao {
    @Query("SELECT * FROM songs")
    fun getAllSongsFlow(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs")
    suspend fun getAllSongs(): List<SongEntity>

    @Query("SELECT id FROM songs")
    suspend fun getAllIds(): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<SongEntity>)

    @Query("UPDATE songs SET title = :title, artist = :artist, album = :album, genre = :genre WHERE id = :id")
    suspend fun updateSongTags(id: Long, title: String, artist: String, album: String, genre: String)

    @Query("DELETE FROM songs WHERE id IN (:ids)")
    suspend fun deleteSongsByIds(ids: List<Long>)

    // ── FIX: removeDeletedSongs ya no usa NOT IN (:currentIds) ──
    // SQLite tiene un límite de ~999 variables bind. Con bibliotecas de >999 canciones,
    // "NOT IN (:currentIds)" lanza SQLiteException: too many SQL variables.
    // La estrategia correcta es: obtener IDs actuales en Room → diff en Kotlin → borrar solo los
    // huérfanos con deleteSongsByIds(ids), que también usa IN pero con una lista pequeña
    // (solo los borrados, que normalmente son 0 o pocos).
    // Este método se mantiene como fallback para migraciones/tests, pero ya no se llama
    // desde MusicRepository en el flujo normal de escaneo.
    @Query("DELETE FROM songs WHERE id NOT IN (:currentIds)")
    suspend fun removeDeletedSongsUnsafe(currentIds: List<Long>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteEntity)

    @Delete
    suspend fun deleteFavorite(favorite: FavoriteEntity)

    @Query("SELECT songId FROM favorites")
    fun getFavorites(): Flow<List<Long>>

    @Insert
    suspend fun createPlaylist(playlist: PlaylistEntity): Long

    @Query("SELECT * FROM playlists")
    fun getPlaylists(): Flow<List<PlaylistEntity>>

    @Delete
    suspend fun deletePlaylist(playlist: PlaylistEntity)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun deleteSongsFromPlaylist(playlistId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addSongToPlaylist(crossRef: PlaylistSongCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addSongsToPlaylist(crossRefs: List<PlaylistSongCrossRef>)

    @Delete
    suspend fun removeSongFromPlaylist(crossRef: PlaylistSongCrossRef)

    @Query("SELECT songId FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun getSongsInPlaylist(playlistId: Long): List<Long>

    @Query("SELECT * FROM songs WHERE id IN (:ids)")
    suspend fun getSongsByIds(ids: List<Long>): List<SongEntity>

    @Insert
    suspend fun addToHistory(history: HistoryEntity)

    @Query("DELETE FROM history WHERE id NOT IN (SELECT id FROM history ORDER BY timestamp DESC LIMIT 50)")
    suspend fun trimHistory()

    @Query("SELECT * FROM history ORDER BY timestamp DESC LIMIT 50")
    fun getHistory(): Flow<List<HistoryEntity>>
}
