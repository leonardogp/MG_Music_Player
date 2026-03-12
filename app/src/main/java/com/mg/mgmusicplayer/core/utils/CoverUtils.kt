package com.mg.mgmusicplayer.core.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.mg.mgmusicplayer.core.cache.CoverCache
import java.io.File

object CoverUtils {
    fun getEmbeddedCover(context: Context, songId: Long, path: String): Bitmap? {
        val cached = CoverCache.get(songId)
        if (cached != null) return cached

        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, Uri.parse(path))
            val art = retriever.embeddedPicture
            retriever.release()
            if (art != null) {
                val bitmap = BitmapFactory.decodeByteArray(art, 0, art.size)
                CoverCache.put(songId, bitmap)
                bitmap
            } else {
                getExternalCover(path)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun getExternalCover(songPath: String): Bitmap? {
        val file = File(songPath)
        val parent = file.parentFile ?: return null
        val coverFiles = parent.listFiles { _, name ->
            val lower = name.lowercase()
            lower.contains("cover") || lower.contains("folder") || lower.contains("front") || lower.contains("album")
        }
        
        val imageFile = coverFiles?.firstOrNull { it.extension.lowercase() in listOf("jpg", "jpeg", "png") }
        return imageFile?.let { BitmapFactory.decodeFile(it.absolutePath) }
    }
}
