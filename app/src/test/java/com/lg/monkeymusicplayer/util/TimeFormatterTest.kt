package com.lg.monkeymusicplayer.util

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeFormatterTest {

    @Test
    fun formatDuration_mm_ss() {
        // 2 minutes 3 seconds = 123000 ms
        val result = TimeFormatter.formatDuration(123_000L)
        assertEquals("02:03", result)
    }

    @Test
    fun formatDurationLong_withHours() {
        // 1 hour 2 minutes 3 seconds = 3723000 ms
        val result = TimeFormatter.formatDurationLong(3_723_000L)
        assertEquals("01:02:03", result)
    }

    @Test
    fun formatDurationLong_withoutHours() {
        // 12 minutes 34 seconds = 754000 ms
        val result = TimeFormatter.formatDurationLong(754_000L)
        assertEquals("12:34", result)
    }

    @Test
    fun parseTimeToMs_mm_ss() {
        val ms = TimeFormatter.parseTimeToMs("03:15")
        assertEquals(3 * 60_000L + 15_000L, ms)
    }

    @Test
    fun parseTimeToMs_hh_mm_ss() {
        val ms = TimeFormatter.parseTimeToMs("1:02:03")
        assertEquals(1 * 3600_000L + 2 * 60_000L + 3_000L, ms)
    }

    @Test
    fun parseTimeToMs_invalid() {
        val ms = TimeFormatter.parseTimeToMs("not-a-time")
        assertEquals(0L, ms)
    }
}
