package com.lg.monkeymusicplayer.core.scanner

import android.media.MediaMetadataRetriever
import android.util.Log

class MusicScannerV2 {

    fun scanMusicFiles(directoryPath: String): List<String> {
        val musicFiles = mutableListOf<String>()

        try {
            val dir = File(directoryPath)
            if (dir.exists() && dir.isDirectory) {
                val files = dir.listFiles() ?: throw Exception("Unable to list files in directory.")

                for (file in files) {
                    if (file.extension.equals("mp3", ignoreCase = true)) {
                        musicFiles.add(file.absolutePath)
                    }
                }
            } else {
                Log.e("MusicScannerV2", "Provided path is not a directory or does not exist.")
            }
        } catch (e: Exception) {
            Log.e("MusicScannerV2", "Error while scanning music files: ${"$"}{e.message}", e)
        }

        return musicFiles
    }
}