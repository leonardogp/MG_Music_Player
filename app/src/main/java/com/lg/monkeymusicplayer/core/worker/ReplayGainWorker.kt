package com.lg.monkeymusicplayer.core.worker

import android.content.Context
import android.provider.MediaStore
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.lg.monkeymusicplayer.core.player.ReplayGainReader
import com.lg.monkeymusicplayer.data.database.MusicDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * ReplayGainWorker — lee los tags REPLAYGAIN_TRACK_GAIN de disco en background
 * y actualiza el campo replayGain en Room para las canciones que no lo tienen aún.
 *
 * Se lanza como tarea one-shot después de cada scan. No bloquea la UI.
 * Mp3File() parsea archivos completos (~50–200ms por archivo), por lo que procesar
 * 800 canciones secuencialmente aquí no tiene impacto en la experiencia del usuario.
 */
@HiltWorker
class ReplayGainWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val musicDao: MusicDao
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "replay_gain_background"

        fun schedule(context: Context) {
            val request = OneTimeWorkRequestBuilder<ReplayGainWorker>().build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
            Timber.d("ReplayGainWorker: scheduled")
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // Solo procesar canciones cuyo replayGain aún sea null
            val songs = musicDao.getAllSongs().filter { it.replayGain == null }
            if (songs.isEmpty()) return@withContext Result.success()

            Timber.d("ReplayGainWorker: procesando ${songs.size} canciones sin ReplayGain")

            // Construir mapa id → path real desde MediaStore para el conjunto de IDs
            val idSet = songs.map { it.id }.toSet()
            val pathMap = queryRealPaths(idSet)

            var updated = 0
            for (song in songs) {
                val path = pathMap[song.id] ?: continue
                val gain = ReplayGainReader.readTrackGain(path) ?: continue
                musicDao.updateReplayGain(song.id, gain)
                updated++
            }

            Timber.d("ReplayGainWorker: $updated canciones actualizadas con ReplayGain")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "ReplayGainWorker: error")
            Result.retry()
        }
    }

    private fun queryRealPaths(ids: Set<Long>): Map<Long, String> {
        val result = mutableMapOf<Long, String>()
        // Procesar en chunks de 500 para evitar el límite de 999 variables SQLite
        ids.chunked(500).forEach { chunk ->
            val placeholders = chunk.joinToString(",") { "?" }
            applicationContext.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DATA),
                "${MediaStore.Audio.Media._ID} IN ($placeholders)",
                chunk.map { it.toString() }.toTypedArray(),
                null
            )?.use { c ->
                val colId   = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val colData = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                while (c.moveToNext()) {
                    val path = c.getString(colData) ?: continue
                    result[c.getLong(colId)] = path
                }
            }
        }
        return result
    }
}
