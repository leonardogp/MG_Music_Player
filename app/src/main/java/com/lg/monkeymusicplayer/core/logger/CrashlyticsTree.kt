package com.lg.monkeymusicplayer.core.logger

import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber
import android.util.Log

/**
 * Timber Tree que enruta los logs a Firebase Crashlytics en builds de release.
 *
 * Estrategia:
 *  - ERROR / WTF → reportar como excepción no fatal (aparece en el dashboard de Crashlytics)
 *  - WARN        → añadir como breadcrumb (log personalizado visible en el crash report)
 *  - INFO / DEBUG / VERBOSE → ignorar en release (reducen ruido y volumen de datos)
 *
 * El árbol solo se planta si Crashlytics está disponible (google-services.json presente).
 * En debug, Timber.DebugTree() cubre el logging local.
 */
class CrashlyticsTree : Timber.Tree() {

    private val crashlytics by lazy { FirebaseCrashlytics.getInstance() }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        when (priority) {
            Log.ERROR, Log.ASSERT -> {
                // Reportar como error no fatal — aparece en Crashlytics > Non-fatals
                if (t != null) {
                    crashlytics.recordException(t)
                } else {
                    // Si no hay Throwable, crear uno para capturar el stack trace
                    crashlytics.recordException(
                        RuntimeException("${tag ?: "App"}: $message")
                    )
                }
                // También loggear como breadcrumb para contexto
                crashlytics.log("E/${tag ?: "App"}: $message")
            }
            Log.WARN -> {
                // Añadir como breadcrumb — visible en crash reports pero no crea entrada propia
                crashlytics.log("W/${tag ?: "App"}: $message")
            }
            // INFO, DEBUG, VERBOSE — ignorar en release
        }
    }
}
