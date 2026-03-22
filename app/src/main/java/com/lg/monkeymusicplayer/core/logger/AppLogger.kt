package com.lg.monkeymusicplayer.core.logger

import timber.log.Timber
import com.lg.monkeymusicplayer.BuildConfig

/**
 * Punto de inicialización del sistema de logging.
 *
 * ── CORRECCIÓN: wrapper eliminado, solo queda initialize() ──
 * Antes: AppLogger exponía d/e/i/w/v() que solo redirigían a Timber.tag().x(),
 *        añadiendo una capa de indirección sin ningún beneficio. Además, el wrapper
 *        impedía usar las variantes con Throwable de Timber (e.g. Timber.e(exception)).
 *
 * Ahora: usar Timber directamente en el código:
 *   Timber.d("mensaje")
 *   Timber.tag("MiClase").w("advertencia")
 *   Timber.e(exception, "descripción del error")
 *
 * Solo se mantiene initialize() aquí para centralizar el setup en Application.
 */
object AppLogger {
    fun initialize() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        // En release podrías plantar un árbol que reporte a Crashlytics:
        // else { Timber.plant(CrashlyticsTree()) }
    }
}
