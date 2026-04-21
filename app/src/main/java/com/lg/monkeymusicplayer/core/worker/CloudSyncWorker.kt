package com.lg.monkeymusicplayer.core.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.lg.monkeymusicplayer.core.result.Result
import com.lg.monkeymusicplayer.data.repository.CloudSyncRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * CloudSyncWorker — sincronización periódica de datos en background.
 *
 * ## Política de ejecución
 * - Periódico: cada 12 horas.
 * - Requiere conexión a red ([NetworkType.CONNECTED]).
 * - [ExistingPeriodicWorkPolicy.KEEP]: no reinicia el contador si ya está programado.
 * - En caso de error: [Result.retry] — WorkManager reintenta con backoff exponencial.
 *
 * ## Qué sincroniza
 * Exporta un backup completo al archivo de nube local. El archivo puede estar
 * sincronizado automáticamente por Google Drive (si la carpeta está en Drive),
 * o el usuario puede compartirlo manualmente.
 *
 * Para integrar un backend real (Firebase, Dropbox, etc.):
 * reemplazar la llamada a [CloudSyncRepository.upload] por la del SDK correspondiente
 * sin cambiar este Worker.
 */
@HiltWorker
class CloudSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val cloudSyncRepository: CloudSyncRepository
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "cloud_sync_periodic"
        private const val INTERVAL_HOURS = 12L

        /**
         * Registra (o actualiza) la tarea periódica en WorkManager.
         * Llamar desde [MonkeyMusicPlayerApp.onCreate] si el usuario tiene
         * la sincronización habilitada en ajustes.
         */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<CloudSyncWorker>(
                INTERVAL_HOURS, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
            Timber.d("CloudSyncWorker: scheduled (interval=${INTERVAL_HOURS}h, requires network)")
        }

        /** Cancela la sincronización periódica (ej. usuario desactiva la feature). */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Timber.d("CloudSyncWorker: cancelled")
        }
    }

    override suspend fun doWork(): Result {
        Timber.d("CloudSyncWorker: starting sync")
        return when (val result = cloudSyncRepository.upload()) {
            is com.lg.monkeymusicplayer.core.result.Result.Success -> {
                Timber.d("CloudSyncWorker: sync OK → ${result.data}")
                Result.success()
            }
            is com.lg.monkeymusicplayer.core.result.Result.Error -> {
                Timber.w("CloudSyncWorker: sync failed — ${result.message}")
                Result.retry()
            }
            else -> Result.retry()
        }
    }
}
