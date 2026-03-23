package com.lg.monkeymusicplayer.data.database

import androidx.room.*
import com.lg.monkeymusicplayer.data.model.Song

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val id: Long,
    val albumId: Long,
    val title: String,
    val artist: String,
    val album: String,
    val genre: String,
    val folder: String,
    val path: String,
    val albumArtUri: String
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val songId: Long
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

@Entity(tableName = "playlist_songs", primaryKeys = ["playlistId", "songId"])
data class PlaylistSongCrossRef(
    val playlistId: Long,
    val songId: Long
)

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songId: Long,
    val timestamp: Long
)

@Dao
interface MusicDao {
    @Query("SELECT * FROM songs")
    fun getAllSongsFlow(): kotlinx.coroutines.flow.Flow<List<SongEntity>>

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

    @Query("DELETE FROM songs WHERE id NOT IN (:currentIds)")
    suspend fun removeDeletedSongs(currentIds: List<Long>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteEntity)

    @Delete
    suspend fun deleteFavorite(favorite: FavoriteEntity)

    @Query("SELECT songId FROM favorites")
    fun getFavorites(): kotlinx.coroutines.flow.Flow<List<Long>>

    @Insert
    suspend fun createPlaylist(playlist: PlaylistEntity): Long

    @Query("SELECT * FROM playlists")
    fun getPlaylists(): kotlinx.coroutines.flow.Flow<List<PlaylistEntity>>

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

    // Query directa por IDs — evita cargar toda la biblioteca solo para filtrar
    @Query("SELECT * FROM songs WHERE id IN (:ids)")
    suspend fun getSongsByIds(ids: List<Long>): List<SongEntity>

    @Insert
    suspend fun addToHistory(history: HistoryEntity)

    // Limitar el historial a las últimas 50 entradas tras cada inserción.
    // Sin esto la tabla crece indefinidamente aunque getHistory() solo muestre 50.
    @Query("DELETE FROM history WHERE id NOT IN (SELECT id FROM history ORDER BY timestamp DESC LIMIT 50)")
    suspend fun trimHistory()

    @Query("SELECT * FROM history ORDER BY timestamp DESC LIMIT 50")
    fun getHistory(): kotlinx.coroutines.flow.Flow<List<HistoryEntity>>
}

@Database(
    entities = [SongEntity::class, FavoriteEntity::class, PlaylistEntity::class, PlaylistSongCrossRef::class, HistoryEntity::class],
    version = 2,
    exportSchema = false
)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun musicDao(): MusicDao

    companion object {
        @Volatile
        private var INSTANCE: MusicDatabase? = null

        fun getDatabase(context: android.content.Context): MusicDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MusicDatabase::class.java,
                    "music_database"
                )
                .apply {
                    // En debug: migración destructiva para agilizar el desarrollo.
                    // En release: falla con un error claro en lugar de borrar datos del usuario.
                    // TODO: añadir migraciones explícitas (addMigrations) antes de cada release
                    //       que cambie el esquema de la base de datos.
                    if (com.lg.monkeymusicplayer.BuildConfig.DEBUG) {
                        fallbackToDestructiveMigration()
                    }
                }
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
