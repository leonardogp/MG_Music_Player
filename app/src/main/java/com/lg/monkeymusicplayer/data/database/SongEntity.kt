package com.lg.monkeymusicplayer.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

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
    val albumArtUri: String,
    /**
     * ReplayGain track gain en dB, leído del tag ID3 TXXX:REPLAYGAIN_TRACK_GAIN.
     * Null si el archivo no tiene el tag. Se aplica como ajuste de volumen en ExoPlayer.
     * Rango típico: -15.0 a +15.0 dB.
     */
    val replayGain: Float? = null
)
