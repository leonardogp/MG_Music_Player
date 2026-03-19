package com.lg.monkeymusicplayer.core.logger

import timber.log.Timber

object AppLogger {
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