package com.lg.monkeymusicplayer

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.lg.monkeymusicplayer.core.exception.GlobalExceptionHandler
import com.lg.monkeymusicplayer.core.logger.AppLogger
import com.lg.monkeymusicplayer.core.worker.SmartPlaylistWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application entry point.
 *
 * Implementa [Configuration.Provider] para que WorkManager use [HiltWorkerFactory]
 * y pueda inyectar dependencias en los Workers vía @HiltWorker.
 * WorkManager lee [workManagerConfiguration] en su inicialización lazy; no hace falta
 * llamar a WorkManager.initialize() explícitamente al implementar esta interfaz.
 */
@HiltAndroidApp
class MonkeyMusicPlayerApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        AppLogger.initialize()
        GlobalExceptionHandler.setup(this)
        SmartPlaylistWorker.schedule(this)
    }
}
