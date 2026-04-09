package com.lg.monkeymusicplayer.core.smart

import com.lg.monkeymusicplayer.data.database.SongStatEntity
import com.lg.monkeymusicplayer.data.model.Song
import com.lg.monkeymusicplayer.data.model.SmartPlaylist
import com.lg.monkeymusicplayer.data.model.SmartPlaylistType
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ln

/**
 * SmartEngine — calcula scores y genera las 3 Smart Playlists.
 *
 * Formula de score (inspirada en Hacker News ranking + Spotify decay):
 *
 *   score = (playCount × 2)
 *         + (completeCount × 3)          // completar vale más que solo reproducir
 *         - (skipCount × 1.5)            // penalización por skip
 *         + completionRate               // ratio 0..1 de tiempo escuchado vs total
 *         - recencyDecay                 // penaliza canciones no escuchadas recientemente
 *
 * recencyDecay = ln(1 + daysSinceLastPlay / DECAY_HALF_LIFE_DAYS)
 * Con DECAY_HALF_LIFE_DAYS = 14, una canción no escuchada en 2 semanas
 * pierde ~ln(2) ≈ 0.69 puntos — suave pero apreciable a escala de meses.
 *
 * El engine no sabe nada de Room ni de coroutines; recibe los datos ya cargados.
 * La orquestación es responsabilidad de [SmartRepository].
 */
@Singleton
class SmartEngine @Inject constructor() {

    companion object {
        private const val DECAY_HALF_LIFE_DAYS = 14.0
        private const val MS_PER_DAY = 86_400_000L
        private const val DAILY_MIX_SIZE = 30
        private const val REDISCOVER_SIZE = 25
        private const val TOP_SONGS_SIZE = 25
        /** Días sin reproducción para considerar una canción como "olvidada". */
        private const val REDISCOVER_CUTOFF_DAYS = 21
        /** Mínimo de plays para entrar en Rediscover (evitar ruido con canciones apenas probadas). */
        private const val REDISCOVER_MIN_PLAYS = 3
    }

    // ── API pública ──────────────────────────────────────────────────────────

    /**
     * Genera las 3 Smart Playlists a partir de stats y canciones disponibles.
     *
     * @param stats   Todas las filas de `song_stats`.
     * @param songs   Mapa songId → Song del catálogo completo.
     * @param nowMs   Timestamp actual (inyectado para facilitar tests).
     */
    fun generate(
        stats: List<SongStatEntity>,
        songs: Map<Long, Song>,
        nowMs: Long = System.currentTimeMillis()
    ): List<SmartPlaylist> {
        if (stats.isEmpty() || songs.isEmpty()) return emptyList()

        val scored = stats
            .mapNotNull { stat ->
                val song = songs[stat.songId] ?: return@mapNotNull null
                ScoredSong(song, stat, calculateScore(stat, nowMs))
            }
            .sortedByDescending { it.score }

        return listOf(
            buildDailyMix(scored, nowMs),
            buildRediscover(scored, nowMs),
            buildTopSongs(scored)
        )
    }

    // ── Builders de playlist ─────────────────────────────────────────────────

    private fun buildDailyMix(scored: List<ScoredSong>, nowMs: Long): SmartPlaylist {
        // Top score con diversidad de artistas: máximo 3 canciones por artista
        val artistCount = mutableMapOf<String, Int>()
        val songs = scored
            .filter { it.stat.playCount > 0 }
            .filter { song ->
                val count = artistCount.getOrDefault(song.song.artist, 0)
                if (count < 3) { artistCount[song.song.artist] = count + 1; true } else false
            }
            .take(DAILY_MIX_SIZE)
            .map { it.song }
        return SmartPlaylist(SmartPlaylistType.DAILY_MIX, songs)
    }

    private fun buildRediscover(scored: List<ScoredSong>, nowMs: Long): SmartPlaylist {
        val cutoffMs = nowMs - (REDISCOVER_CUTOFF_DAYS * MS_PER_DAY)
        val songs = scored
            .filter { it.stat.playCount >= REDISCOVER_MIN_PLAYS }
            .filter { it.stat.lastPlayedAt < cutoffMs }
            // Reordenar: para Rediscover el score histórico importa más que el recency
            .sortedByDescending { historicScore(it.stat) }
            .take(REDISCOVER_SIZE)
            .map { it.song }
        return SmartPlaylist(SmartPlaylistType.REDISCOVER, songs)
    }

    private fun buildTopSongs(scored: List<ScoredSong>): SmartPlaylist {
        val songs = scored
            .filter { it.stat.playCount > 0 }
            .sortedByDescending { it.stat.playCount + it.stat.completeCount }
            .take(TOP_SONGS_SIZE)
            .map { it.song }
        return SmartPlaylist(SmartPlaylistType.TOP_SONGS, songs)
    }

    // ── Scoring ──────────────────────────────────────────────────────────────

    fun calculateScore(stat: SongStatEntity, nowMs: Long): Double {
        val completionRate = if (stat.playCount > 0 && stat.totalPlayTimeMs > 0) {
            (stat.completeCount.toDouble() / stat.playCount).coerceIn(0.0, 1.0)
        } else 0.0

        val daysSinceLast = if (stat.lastPlayedAt > 0L) {
            ((nowMs - stat.lastPlayedAt) / MS_PER_DAY).toDouble()
        } else Double.MAX_VALUE / 1000.0

        val recencyDecay = ln(1.0 + daysSinceLast / DECAY_HALF_LIFE_DAYS)

        return (stat.playCount * 2.0) +
               (stat.completeCount * 3.0) -
               (stat.skipCount * 1.5) +
               completionRate -
               recencyDecay
    }

    /** Score histórico sin penalización de recency — usado en Rediscover. */
    private fun historicScore(stat: SongStatEntity): Double {
        val completionRate = if (stat.playCount > 0) {
            (stat.completeCount.toDouble() / stat.playCount).coerceIn(0.0, 1.0)
        } else 0.0
        return (stat.playCount * 2.0) + (stat.completeCount * 3.0) -
               (stat.skipCount * 1.5) + completionRate
    }

    // ── Tipos internos ───────────────────────────────────────────────────────

    private data class ScoredSong(
        val song: Song,
        val stat: SongStatEntity,
        val score: Double
    )
}
