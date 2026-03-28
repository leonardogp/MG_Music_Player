package com.lg.monkeymusicplayer.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Almacena presets de EQ definidos por el usuario.
 *
 * bandLevels: valores normalizados en [-1f, 1f], separados por coma.
 * Se serializa como String para no depender de TypeConverters externos,
 * mantener la migración simple y soportar número variable de bandas entre dispositivos.
 *
 * Ejemplo: "-0.2,0.0,0.4,0.6,0.3"
 */
@Entity(tableName = "eq_presets")
data class EqPresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val bandLevels: String   // CSV de floats: "-0.2,0.0,0.4,0.6,0.3"
) {
    fun toLevels(): List<Float> =
        bandLevels.split(",").mapNotNull { it.trim().toFloatOrNull() }

    companion object {
        fun fromLevels(name: String, levels: List<Float>): EqPresetEntity =
            EqPresetEntity(name = name, bandLevels = levels.joinToString(","))
    }
}
