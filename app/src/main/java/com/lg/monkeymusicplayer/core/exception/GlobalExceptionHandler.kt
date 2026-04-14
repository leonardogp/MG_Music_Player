package com.lg.monkeymusicplayer.core.exception

import android.content.Context
import android.content.Intent
import com.lg.monkeymusicplayer.core.player.MusicService
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber

class GlobalExceptionHandler(
    private val context: Context,
    // ── CORRECCIÓN: guardar el handler original del sistema ──
    // Antes: se llamaba exitProcess(1) directamente, matando el proceso sin dar
    //        oportunidad a que Android liberara recursos (ExoPlayer, Equalizer,
    //        MediaSession, AudioFlinger session), lo que podía dejar la sesión de
    //        audio del sistema en estado corrupto para otras apps.
    // Ahora: se delega en el handler por defecto del sistema, que hace crash reporting
    //        y limpieza antes de matar el proceso. El servicio de música se detiene
    //        explícitamente primero para liberar los recursos de audio.
    private val defaultHandler: Thread.UncaughtExceptionHandler? =
        Thread.getDefaultUncaughtExceptionHandler()
) : Thread.UncaughtExceptionHandler {

    companion object {
        private const val TAG = "GlobalExceptionHandler"

        fun setup(context: Context) {
            val handler = GlobalExceptionHandler(context)
            Thread.setDefaultUncaughtExceptionHandler(handler)
        }
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        Timber.tag(TAG).e(throwable, "Uncaught exception in thread \${thread.name}")

        // Enriquecer el crash report con contexto antes de que el proceso muera.
        // Crashlytics envía estos datos junto al stack trace al dashboard.
        try {
            val crashlytics = FirebaseCrashlytics.getInstance()
            crashlytics.setCustomKey("crash_thread", thread.name)
            crashlytics.setCustomKey("crash_thread_id", thread.id)
            crashlytics.log("Fatal crash in thread: \${thread.name} — \${throwable.message}")
            // Forzar flush antes de que el proceso muera para no perder el evento
            crashlytics.recordException(throwable)
        } catch (e: Exception) {
            // No propagar — si Crashlytics falla, continuar con el flujo normal de crash
            Timber.tag(TAG).e(e, "Failed to record crash in Crashlytics")
        }

        // Intentar detener el servicio de música para liberar recursos de audio
        // antes de que el proceso muera.
        try {
            context.stopService(Intent(context, MusicService::class.java))
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to stop MusicService during crash")
        }

        // Delegar en el handler original del sistema.
        if (defaultHandler != null) {
            defaultHandler.uncaughtException(thread, throwable)
        } else {
            throwable.printStackTrace()
            android.os.Process.killProcess(android.os.Process.myPid())
        }
    }

    // Tipos de excepción personalizados para lanzar desde el código con contexto claro
    class MusicPlayerException(message: String?) : Exception(message)
    class AudioSessionException(message: String?) : Exception(message)
    class PermissionException(message: String?) : Exception(message)
    class MusicScanException(message: String?) : Exception(message)
}
