package com.lg.monkeymusicplayer.core.cache

import android.graphics.Bitmap
import android.util.LruCache

object CoverCache {
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 8

    private val memoryCache = object : LruCache<Long, Bitmap>(cacheSize) {
        override fun sizeOf(key: Long, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    fun put(songId: Long, bitmap: Bitmap) {
        if (get(songId) == null) {
            memoryCache.put(songId, bitmap)
        }
    }

    fun get(songId: Long): Bitmap? {
        return memoryCache.get(songId)
    }

    fun clear() {
        memoryCache.evictAll()
    }
}
