package com.lg.monkeymusicplayer

import android.app.Application
import com.lg.monkeymusicplayer.core.exception.GlobalExceptionHandler
import com.lg.monkeymusicplayer.core.logger.AppLogger
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.lg.monkeymusicplayer.BuildConfig
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MonkeyMusicPlayerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppLogger.initialize()
        GlobalExceptionHandler.setup(this)
        setupCrashlytics()
    }

    private fun setupCrashlytics() {
        val crashlytics = FirebaseCrashlytics.getInstance()
        // En debug, deshabilitar el envío automático para no contaminar el dashboard
        // con crashes de desarrollo. Los errores siguen visibles en Logcat via Timber.
        crashlytics.isCrashlyticsCollectionEnabled = !BuildConfig.DEBUG
        // Metadatos útiles para filtrar crashes en el dashboard
        crashlytics.setCustomKey("build_type", BuildConfig.BUILD_TYPE)
        crashlytics.setCustomKey("version_name", BuildConfig.VERSION_NAME)
        crashlytics.setCustomKey("version_code", BuildConfig.VERSION_CODE)
    }
}
