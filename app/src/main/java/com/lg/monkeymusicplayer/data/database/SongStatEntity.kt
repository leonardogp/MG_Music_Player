package com.lg.monkeymusicplayer.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Estadísticas de reproducción por canción.
 *
 * Campos:
 *  - playCount       : veces que se inició la reproducción
 *  - skipCount       : veces que se saltó antes del umbral de compleción (80%)
 *  - completeCount   : veces que se reprodujo ≥80% de la duración
 *  - totalPlayTimeMs : tiempo acumulado reproducido en ms (para calcular completionRate)
 *  - lastPlayedAt    : timestamp Unix de la última reproducción (para el decay de recency)
 *
 * El score del Smart Engine se calcula a partir de estos campos en GetSmartPlaylistUseCase.
 */
@Entity(tableName = "song_stats")
data class SongStatEntity(
    @PrimaryKey val songId: Long,
    val playCount: Int = 0,
    val skipCount: Int = 0,
    val completeCount: Int = 0,
    val totalPlayTimeMs: Long = 0L,
    val lastPlayedAt: Long = 0L
)
