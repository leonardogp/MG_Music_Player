package com.lg.monkeymusicplayer.core.exception

import android.content.Context
import com.lg.monkeymusicplayer.core.logger.AppLogger
import kotlin.system.exitProcess

class GlobalExceptionHandler(private val context: Context) : Thread.UncaughtExceptionHandler {

    companion object {
        private const val TAG = "GlobalExceptionHandler"

        fun setup(context: Context) {
            val handler = GlobalExceptionHandler(context)
            Thread.setDefaultUncaughtExceptionHandler(handler)
        }
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        AppLogger.e(TAG, "Uncaught exception in thread ${thread.name}: ${throwable.message}")
        throwable.printStackTrace()
        
        // You could start an error activity here
        
        exitProcess(1)
    }

    class MusicPlayerException(message: String?) : Exception(message)
    class AudioSessionException(message: String?) : Exception(message)
    class PermissionException(message: String?) : Exception(message)
    class MusicScanException(message: String?) : Exception(message)
}
