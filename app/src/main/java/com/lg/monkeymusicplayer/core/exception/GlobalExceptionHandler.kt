package com.lg.monkeymusicplayer.core.exception

import android.content.Context
import android.content.Intent
import com.lg.monkeymusicplayer.core.player.MusicService
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
        Timber.tag(TAG).e(throwable, "Uncaught exception in thread ${thread.name}")

        // Intentar detener el servicio de música para liberar recursos de audio
        // antes de que el proceso muera. Se hace en try/catch porque si el crash
        // ocurrió en el propio servicio, stopService() podría fallar también.
        try {
            context.stopService(Intent(context, MusicService::class.java))
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to stop MusicService during crash")
        }

        // Delegar en el handler original del sistema.
        // Esto permite que Android maneje el crash report (logcat, Play Console,
        // Firebase Crashlytics si está configurado) y limpie el proceso correctamente.
        // Si no hay handler original (caso raro), forzar salida limpia.
        if (defaultHandler != null) {
            defaultHandler.uncaughtException(thread, throwable)
        } else {
            // Fallback: imprimir stack trace y salir
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
