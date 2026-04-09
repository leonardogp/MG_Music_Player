package com.lg.monkeymusicplayer.data.model

import kotlinx.serialization.Serializable

/**
 * Snapshot serializable de todos los datos del usuario.
 *
 * Formato en disco: JSON UTF-8 con extensión .monkeybackup
 * Versión actual: 1 — cualquier cambio estructural debe incrementar
 * [schemaVersion] y añadir lógica de migración en [BackupRepository].
 *
 * Qué se incluye:
 *  - Favoritos (songIds — las canciones deben existir en el dispositivo)
 *  - Playlists con sus canciones (nombre + lista de songIds)
 *  - Presets de EQ (nombre + niveles de banda)
 *  - Estadísticas de reproducción (playCount, skipCount, etc.)
 *
 * Qué NO se incluye:
 *  - Los archivos de audio (son del filesystem del usuario)
 *  - El historial de reproducción (efímero, sin valor de restaurar)
 *  - Carpetas excluidas (se guardan en SharedPreferences, restaurar
 *    podría excluir carpetas que no existen en el nuevo dispositivo)
 */
@Serializable
data class BackupData(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val createdAt: Long = System.currentTimeMillis(),
    val appVersion: String = "1.0",
    val favorites: List<Long> = emptyList(),
    val playlists: List<PlaylistBackup> = emptyList(),
    val eqPresets: List<EqPresetBackup> = emptyList(),
    val stats: List<SongStatBackup> = emptyList()
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
        const val FILE_EXTENSION = "monkeybackup"
        const val MIME_TYPE = "application/octet-stream"
    }
}

@Serializable
data class PlaylistBackup(
    val name: String,
    val songIds: List<Long>
)

@Serializable
data class EqPresetBackup(
    val name: String,
    val bandLevels: String   // CSV igual que EqPresetEntity
)

@Serializable
data class SongStatBackup(
    val songId: Long,
    val playCount: Int,
    val skipCount: Int,
    val completeCount: Int,
    val totalPlayTimeMs: Long,
    val lastPlayedAt: Long
)
