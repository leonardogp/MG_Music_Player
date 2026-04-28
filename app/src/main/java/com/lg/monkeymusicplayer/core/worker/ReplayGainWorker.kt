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
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * ReplayGainWorker — lee los tags REPLAYGAIN_TRACK_GAIN de disco en background.
 * 
 * Optimización: se han añadido pausas (delay) entre archivos para evitar saturar
 * el decodificador de audio del sistema (Codec2) y los buffers de I/O en dispositivos reales.
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
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val songs = musicDao.getAllSongs().filter { it.replayGain == null }
            if (songs.isEmpty()) return@withContext Result.success()

            val idSet = songs.map { it.id }.toSet()
            val pathMap = queryRealPaths(idSet)

            var updated = 0
            for ((index, song) in songs.withIndex()) {
                val path = pathMap[song.id] ?: continue
                
                // Leer el gain (operación pesada de I/O)
                val gain = ReplayGainReader.readTrackGain(path)
                if (gain != null) {
                    musicDao.updateReplayGain(song.id, gain)
                    updated++
                }

                // OPT: Cada 10 archivos, pausar 100ms para liberar el bufferpool del sistema
                if (index % 10 == 0) {
                    delay(100)
                }
            }

            Timber.d("ReplayGainWorker: proceso completado ($updated canciones actualizadas)")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "ReplayGainWorker failed")
            Result.retry()
        }
    }

    private fun queryRealPaths(ids: Set<Long>): Map<Long, String> {
        val result = mutableMapOf<Long, String>()
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
