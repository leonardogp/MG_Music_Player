package com.lg.monkeymusicplayer.data.model

/**
 * Agregados de estadísticas de escucha del usuario.
 *
 * Calculados en [StatsRepository] a partir de [SongStatEntity] + [SongEntity].
 */
data class UserStats(
    /** Total de minutos escuchados (suma de totalPlayTimeMs de todas las canciones). */
    val totalListenedMs: Long = 0L,

    /** Total de reproducciones únicas iniciadas. */
    val totalPlays: Int = 0,

    /** Total de canciones distintas reproducidas al menos una vez. */
    val uniqueSongsPlayed: Int = 0,

    /** Top canciones ordenadas por playCount descendente. */
    val topSongs: List<SongWithStat> = emptyList(),

    /** Top artistas: nombre del artista + plays acumulados de todas sus canciones. */
    val topArtists: List<ArtistStat> = emptyList()
)

data class SongWithStat(
    val song: Song,
    val playCount: Int,
    val totalPlayTimeMs: Long,
    val completionRate: Float   // completeCount / playCount, 0..1
)

data class ArtistStat(
    val artist: String,
    val totalPlays: Int,
    val totalPlayTimeMs: Long,
    val songCount: Int           // nº de canciones distintas del artista reproducidas
)
