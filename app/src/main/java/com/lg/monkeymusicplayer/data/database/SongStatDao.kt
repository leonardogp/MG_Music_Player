package com.lg.monkeymusicplayer.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SongStatDao {

    // ── Lectura ──────────────────────────────────────────────────────────────

    @Query("SELECT * FROM song_stats WHERE songId = :songId")
    suspend fun getStatForSong(songId: Long): SongStatEntity?

    @Query("SELECT * FROM song_stats ORDER BY playCount DESC")
    fun getAllStats(): Flow<List<SongStatEntity>>

    /** Top N por playCount — base del Daily Mix. */
    @Query("SELECT * FROM song_stats ORDER BY playCount DESC LIMIT :limit")
    suspend fun getTopPlayed(limit: Int): List<SongStatEntity>

    /**
     * Canciones con score histórico alto pero sin reproducción reciente.
     * Base del Rediscover playlist: lastPlayedAt < [cutoffMs].
     */
    @Query("""
        SELECT * FROM song_stats
        WHERE playCount > :minPlays AND lastPlayedAt < :cutoffMs
        ORDER BY playCount DESC
        LIMIT :limit
    """)
    suspend fun getRediscoverCandidates(
        minPlays: Int,
        cutoffMs: Long,
        limit: Int
    ): List<SongStatEntity>

    @Query("SELECT * FROM song_stats")
    suspend fun getAllStatsSnapshot(): List<SongStatEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStats(stats: List<SongStatEntity>)

    // ── Escritura atómica (UPDATE OR INSERT) ─────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(stat: SongStatEntity)

    @Query("""
        UPDATE song_stats
        SET playCount = playCount + 1,
            lastPlayedAt = :timestamp
        WHERE songId = :songId
    """)
    suspend fun incrementPlay(songId: Long, timestamp: Long)

    @Query("""
        UPDATE song_stats
        SET skipCount = skipCount + 1
        WHERE songId = :songId
    """)
    suspend fun incrementSkip(songId: Long)

    @Query("""
        UPDATE song_stats
        SET completeCount = completeCount + 1,
            totalPlayTimeMs = totalPlayTimeMs + :playedMs
        WHERE songId = :songId
    """)
    suspend fun incrementComplete(songId: Long, playedMs: Long)

    @Query("""
        UPDATE song_stats
        SET totalPlayTimeMs = totalPlayTimeMs + :playedMs
        WHERE songId = :songId
    """)
    suspend fun addPlayTime(songId: Long, playedMs: Long)
}
