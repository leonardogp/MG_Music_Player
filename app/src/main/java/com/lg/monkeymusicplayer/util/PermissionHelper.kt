package com.lg.monkeymusicplayer.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Helper class for managing audio file access permissions
 */
object PermissionHelper {

    /**
     * Get required permissions based on Android version
     */
    fun getRequiredAudioPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ requires READ_MEDIA_AUDIO
            arrayOf(
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.POST_NOTIFICATIONS
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11-12 require MANAGE_EXTERNAL_STORAGE for full access
            arrayOf(Manifest.permission.MANAGE_EXTERNAL_STORAGE)
        } else {
            // Android 10 and below use READ_EXTERNAL_STORAGE
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    /**
     * Check if all required audio permissions are granted
     */
    fun hasAudioPermissions(context: Context): Boolean {
        return getRequiredAudioPermissions().all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Check specific permission
     */
    fun hasPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Get notification permission (Android 13+)
     */
    fun getNotificationPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            emptyArray()
        }
    }

    /**
     * Get foreground service permission (Android 12+)
     */
    fun getForegroundServicePermission(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK)
        } else {
            emptyArray()
        }
    }
}