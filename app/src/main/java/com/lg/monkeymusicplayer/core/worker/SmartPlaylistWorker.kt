package com.lg.monkeymusicplayer.core.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.lg.monkeymusicplayer.data.repository.SmartRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.firstOrNull
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Worker periódico que fuerza un collect del Flow de Smart Playlists para mantener
 * los scores actualizados incluso si la app no está en primer plano.
 *
 * El recálculo real ocurre dentro de [SmartRepository] / [SmartEngine] de forma reactiva
 * cada vez que hay una reproducción; este Worker actúa como safety net de 24h para
 * escenarios donde el usuario no abre la app frecuentemente.
 *
 * Política de scheduling:
 *  - Periódico: cada 24 horas.
 *  - [ExistingPeriodicWorkPolicy.KEEP]: no reinicia el contador si ya existe una tarea programada.
 *  - RequiresCharging = false: debe ejecutarse aunque no esté cargando.
 *  - Tag único [WORK_NAME] para gestión y cancelación.
 */
@HiltWorker
class SmartPlaylistWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val smartRepository: SmartRepository
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "smart_playlist_daily_refresh"
        private const val INTERVAL_HOURS = 24L

        /**
         * Registra (o actualiza) la tarea periódica en WorkManager.
         * Debe llamarse desde [MonkeyMusicPlayerApp.onCreate].
         */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<SmartPlaylistWorker>(
                INTERVAL_HOURS, TimeUnit.HOURS
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
            Timber.d("SmartPlaylistWorker: scheduled (interval=${INTERVAL_HOURS}h)")
        }

        /** Cancela la tarea periódica. Útil en tests o si el usuario desactiva la feature. */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }

    override suspend fun doWork(): Result {
        return try {
            // Un collect del Flow es suficiente para disparar el recálculo en SmartEngine.
            smartRepository.smartPlaylists.firstOrNull()
            Timber.d("SmartPlaylistWorker: refresh completado")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "SmartPlaylistWorker: error durante refresh")
            Result.retry()
        }
    }
}
