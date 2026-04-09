package com.lg.monkeymusicplayer.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import android.net.Uri
import com.lg.monkeymusicplayer.core.result.Result
import com.lg.monkeymusicplayer.data.database.EqPresetEntity
import com.lg.monkeymusicplayer.data.database.FavoriteEntity
import com.lg.monkeymusicplayer.data.database.MusicDao
import com.lg.monkeymusicplayer.data.database.PlaylistEntity
import com.lg.monkeymusicplayer.data.database.PlaylistSongCrossRef
import com.lg.monkeymusicplayer.data.database.SongStatDao
import com.lg.monkeymusicplayer.data.database.SongStatEntity
import com.lg.monkeymusicplayer.data.model.BackupData
import com.lg.monkeymusicplayer.data.model.EqPresetBackup
import com.lg.monkeymusicplayer.data.model.PlaylistBackup
import com.lg.monkeymusicplayer.data.model.SongStatBackup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val musicDao: MusicDao,
    private val statDao: SongStatDao
) {
    private val json = Json {
        prettyPrint = false
        ignoreUnknownKeys = true   // tolerancia hacia versiones futuras del schema
        encodeDefaults = true
    }

    // ── Export ───────────────────────────────────────────────────────────────

    /**
     * Serializa todos los datos del usuario y los escribe en el [Uri] elegido
     * por el usuario vía SAF (ACTION_CREATE_DOCUMENT).
     */
    suspend fun exportBackup(destinationUri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val backup = buildBackupData()
            val jsonString = json.encodeToString(backup)

            context.contentResolver.openOutputStream(destinationUri)?.use { stream ->
                stream.write(jsonString.toByteArray(Charsets.UTF_8))
            } ?: return@withContext Result.Error("No se pudo abrir el archivo de destino")

            Timber.i("BackupRepository: export OK — ${jsonString.length} bytes")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "BackupRepository: export failed")
            Result.Error(e.message ?: "Error desconocido al exportar")
        }
    }

    // ── Import ───────────────────────────────────────────────────────────────

    /**
     * Lee el archivo de backup del [sourceUri] elegido por el usuario y
     * restaura los datos en Room de forma no destructiva (IGNORE en conflictos).
     *
     * "No destructiva" significa:
     *  - Los favoritos y playlists existentes se preservan.
     *  - Los stats se sobreescriben (REPLACE) porque el backup tiene el historial completo.
     *  - Las playlists del backup se crean con nuevos IDs para evitar colisiones.
     */
    suspend fun importBackup(sourceUri: Uri): Result<BackupSummary> = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.contentResolver.openInputStream(sourceUri)?.use { stream ->
                stream.readBytes().toString(Charsets.UTF_8)
            } ?: return@withContext Result.Error("No se pudo leer el archivo de backup")

            val backup = json.decodeFromString<BackupData>(jsonString)

            if (backup.schemaVersion > BackupData.CURRENT_SCHEMA_VERSION) {
                return@withContext Result.Error(
                    "El backup fue creado con una versión más nueva de la app (v${backup.schemaVersion}). " +
                    "Actualiza la app para restaurarlo."
                )
            }

            var favoritesRestored = 0
            var playlistsRestored = 0
            var presetsRestored   = 0
            var statsRestored     = 0

            // ── Favoritos ────────────────────────────────────────────────
            val favorites = backup.favorites.map { FavoriteEntity(it) }
            if (favorites.isNotEmpty()) {
                musicDao.insertFavorites(favorites)
                favoritesRestored = favorites.size
            }

            // ── Playlists ────────────────────────────────────────────────
            for (playlistBackup in backup.playlists) {
                val newId = musicDao.createPlaylist(PlaylistEntity(name = playlistBackup.name))
                if (newId > 0 && playlistBackup.songIds.isNotEmpty()) {
                    val crossRefs = playlistBackup.songIds.map {
                        PlaylistSongCrossRef(newId, it)
                    }
                    musicDao.insertPlaylistSongs(crossRefs)
                    playlistsRestored++
                }
            }

            // ── EQ Presets ───────────────────────────────────────────────
            val presets = backup.eqPresets.map {
                EqPresetEntity(name = it.name, bandLevels = it.bandLevels)
            }
            if (presets.isNotEmpty()) {
                presets.forEach { musicDao.insertEqPreset(it) }
                presetsRestored = presets.size
            }

            // ── Stats ────────────────────────────────────────────────────
            val stats = backup.stats.map {
                SongStatEntity(
                    songId         = it.songId,
                    playCount      = it.playCount,
                    skipCount      = it.skipCount,
                    completeCount  = it.completeCount,
                    totalPlayTimeMs= it.totalPlayTimeMs,
                    lastPlayedAt   = it.lastPlayedAt
                )
            }
            if (stats.isNotEmpty()) {
                statDao.insertStats(stats)
                statsRestored = stats.size
            }

            Timber.i("BackupRepository: import OK — fav=$favoritesRestored playlists=$playlistsRestored presets=$presetsRestored stats=$statsRestored")
            Result.Success(BackupSummary(favoritesRestored, playlistsRestored, presetsRestored, statsRestored))

        } catch (e: Exception) {
            Timber.e(e, "BackupRepository: import failed")
            Result.Error(e.message ?: "Error desconocido al importar")
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private suspend fun buildBackupData(): BackupData {
        val favoriteIds  = musicDao.getAllFavoriteIds()
        val playlists    = musicDao.getAllPlaylists()
        val playlistSongs= musicDao.getAllPlaylistSongs()
        val eqPresets    = musicDao.getEqPresetsSnapshot()
        val stats        = statDao.getAllStatsSnapshot()

        val playlistsById = playlists.associateBy { it.id }
        val songsByPlaylist = playlistSongs.groupBy { it.playlistId }

        val playlistBackups = playlists.map { playlist ->
            PlaylistBackup(
                name    = playlist.name,
                songIds = songsByPlaylist[playlist.id]?.map { it.songId } ?: emptyList()
            )
        }

        return BackupData(
            favorites  = favoriteIds,
            playlists  = playlistBackups,
            eqPresets  = eqPresets.map { EqPresetBackup(it.name, it.bandLevels) },
            stats      = stats.map {
                SongStatBackup(it.songId, it.playCount, it.skipCount,
                               it.completeCount, it.totalPlayTimeMs, it.lastPlayedAt)
            }
        )
    }
}

data class BackupSummary(
    val favoritesRestored: Int,
    val playlistsRestored: Int,
    val presetsRestored: Int,
    val statsRestored: Int
)
