package com.lg.monkeymusicplayer.ui.theme.dynamic

import androidx.compose.ui.graphics.Color
import java.util.LinkedHashMap

object ColorPaletteCache {

    private const val MAX_ENTRIES = 100

    private val cache =
        object : LinkedHashMap<Int, ColorPalette>(
            MAX_ENTRIES,
            0.75f,
            true
        ) {

            override fun removeEldestEntry(
                eldest: MutableMap.MutableEntry<Int, ColorPalette>
            ): Boolean {

                return size > MAX_ENTRIES
            }
        }

    fun get(
        key: Int
    ): ColorPalette? = cache[key]

    fun put(
        key: Int,
        palette: ColorPalette
    ) {
        cache[key] = palette
    }

    fun clear() {
        cache.clear()
    }

    fun size(): Int = cache.size
}