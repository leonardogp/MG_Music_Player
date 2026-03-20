package com.lg.monkeymusicplayer

import android.app.Application
import com.lg.monkeymusicplayer.core.exception.GlobalExceptionHandler
import com.lg.monkeymusicplayer.core.logger.AppLogger

class MonkeyMusicPlayerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppLogger.initialize()
        GlobalExceptionHandler.setup(this)
    }
}
