package com.lg.monkeymusicplayer.core.scanner

import android.content.Context
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock

class MusicScannerCacheTest {
    private lateinit var scanner: MusicScanner
    private val context: Context = mock()

    @Before
    fun setup() {
        scanner = MusicScanner(context)
    }

    @Test
    fun testIsPathExcluded_true() {
        val fullPath = "/storage/emulated/0/WhatsApp/Media/WhatsApp Audio/file.mp3"
        val excluded = listOf("WhatsApp/Media/WhatsApp Audio")
        assertTrue(scanner.isPathExcluded(fullPath, excluded))
    }

    @Test
    fun testIsPathExcluded_false() {
        val fullPath = "/storage/emulated/0/Music/file.mp3"
        val excluded = listOf("WhatsApp/Media/WhatsApp Audio")
        assertFalse(scanner.isPathExcluded(fullPath, excluded))
    }
}
