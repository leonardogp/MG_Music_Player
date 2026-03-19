package com.lg.monkeymusicplayer.util

/**
 * Utility for formatting time values for display
 */
object TimeFormatter {

    /**
     * Format duration in milliseconds to MM:SS format
     */
    fun formatDuration(durationMs: Long): String {
        if (durationMs < 0) return "00:00"
        
        val totalSeconds = durationMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        
        return String.format("%02d:%02d", minutes, seconds)
    }

    /**
     * Format duration to HH:MM:SS format
     */
    fun formatDurationLong(durationMs: Long): String {
        if (durationMs < 0) return "00:00:00"
        
        val totalSeconds = durationMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    /**
     * Parse time string "MM:SS" to milliseconds
     */
    fun parseTimeToMs(timeString: String): Long {
        return try {
            val parts = timeString.split(":")
            when {
                parts.size == 2 -> {
                    val minutes = parts[0].toLong()
                    val seconds = parts[1].toLong()
                    (minutes * 60 + seconds) * 1000
                }
                parts.size == 3 -> {
                    val hours = parts[0].toLong()
                    val minutes = parts[1].toLong()
                    val seconds = parts[2].toLong()
                    (hours * 3600 + minutes * 60 + seconds) * 1000
                }
                else -> 0
            }
        } catch (e: Exception) {
            0
        }
    }
}