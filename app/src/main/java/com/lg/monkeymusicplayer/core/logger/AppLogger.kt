package com.lg.monkeymusicplayer.core.logger

import timber.log.Timber
import com.lg.monkeymusicplayer.BuildConfig

object AppLogger {
    fun initialize() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }

    fun d(tag: String, message: String) {
        Timber.tag(tag).d(message)
    }

    fun e(tag: String, message: String) {
        Timber.tag(tag).e(message)
    }

    fun i(tag: String, message: String) {
        Timber.tag(tag).i(message)
    }

    fun w(tag: String, message: String) {
        Timber.tag(tag).w(message)
    }

    fun v(tag: String, message: String) {
        Timber.tag(tag).v(message)
    }
}
