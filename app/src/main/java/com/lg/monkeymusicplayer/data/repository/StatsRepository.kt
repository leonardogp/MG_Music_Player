package com.lg.monkeymusicplayer.data.repository

import com.lg.monkeymusicplayer.data.database.MusicDao
import com.lg.monkeymusicplayer.data.database.SongStatDao
import com.lg.monkeymusicplayer.data.model.ArtistStat
import com.lg.monkeymusicplayer.data.model.Song
import com.lg.monkeymusicplayer.data.model.SongWithStat
import com.lg.monkeymusicplayer.data.model.UserStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatsRepository @Inject constructor(
    private val musicDao: MusicDao,
    private val statDao: SongStatDao
) {
    companion object {
        private const val TOP_SONGS_LIMIT = 20
        private const val TOP_ARTISTS_LIMIT = 10
    }

    /**
     * Flow reactivo de estadísticas del usuario.
     * Se recalcula automáticamente cuando cambian las stats (tras cada reproducción).
     */
    val userStats: Flow<UserStats> = combine(
        musicDao.getAllSongsFlow(),
        statDao.getAllStats()
    ) { songEntities, stats ->

        if (stats.isEmpty()) return@combine UserStats()

        // Mapa songId → Song para lookups O(1)
        val songMap: Map<Long, Song> = songEntities.associate { e ->
            e.id to Song(e.id, e.albumId, e.title, e.artist, e.album,
                         e.genre, e.folder, e.path, e.albumArtUri)
        }

        // ── Totales globales ─────────────────────────────────────────────
        val totalListenedMs = stats.sumOf { it.totalPlayTimeMs }
        val totalPlays      = stats.sumOf { it.playCount }
        val uniqueSongs     = stats.count { it.playCount > 0 }

        // ── Top canciones ────────────────────────────────────────────────
        val topSongs = stats
            .filter { it.playCount > 0 }
            .sortedByDescending { it.playCount }
            .take(TOP_SONGS_LIMIT)
            .mapNotNull { stat ->
                val song = songMap[stat.songId] ?: return@mapNotNull null
                val completionRate = if (stat.playCount > 0)
                    stat.completeCount.toFloat() / stat.playCount else 0f
                SongWithStat(song, stat.playCount, stat.totalPlayTimeMs, completionRate)
            }

        // ── Top artistas ─────────────────────────────────────────────────
        val topArtists = stats
            .filter { it.playCount > 0 }
            .groupBy { stat -> songMap[stat.songId]?.artist ?: return@groupBy null }
            .mapNotNull { (artist, artistStats) ->
                if (artist == null) return@mapNotNull null
                ArtistStat(
                    artist         = artist,
                    totalPlays     = artistStats.sumOf { it.playCount },
                    totalPlayTimeMs= artistStats.sumOf { it.totalPlayTimeMs },
                    songCount      = artistStats.size
                )
            }
            .sortedByDescending { it.totalPlays }
            .take(TOP_ARTISTS_LIMIT)

        UserStats(
            totalListenedMs  = totalListenedMs,
            totalPlays       = totalPlays,
            uniqueSongsPlayed= uniqueSongs,
            topSongs         = topSongs,
            topArtists       = topArtists
        )
    }.flowOn(Dispatchers.IO)
}
