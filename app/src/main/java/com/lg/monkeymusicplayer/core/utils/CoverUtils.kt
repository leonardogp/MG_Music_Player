package com.lg.monkeymusicplayer.core.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.lg.monkeymusicplayer.core.cache.CoverCache
import java.io.File

object CoverUtils {

    fun getEmbeddedCover(context: Context, songId: Long, path: String): Bitmap? {
        // Revisar cache primero para evitar la extracción costosa
        val cached = CoverCache.get(songId)
        if (cached != null) return cached

        return try {
            // ── CORRECCIÓN: usar .use{} para garantizar release() siempre ──
            // Antes: si setDataSource() lanzaba excepción, retriever.release()
            //        nunca se ejecutaba → recursos nativos (AudioFlinger) quedaban abiertos.
            // Ahora: MediaMetadataRetriever implementa AutoCloseable, .use{} garantiza
            //        que close() (= release()) se llame sin importar lo que ocurra.
            val art = MediaMetadataRetriever().use { retriever ->
                retriever.setDataSource(context, Uri.parse(path))
                retriever.embeddedPicture
            }

            if (art != null) {
                val bitmap = BitmapFactory.decodeByteArray(art, 0, art.size)
                if (bitmap != null) CoverCache.put(songId, bitmap)
                bitmap
            } else {
                // Fallback: buscar imagen de carátula en la carpeta del archivo
                getExternalCover(path)
            }
        } catch (e: Exception) {
            // setDataSource puede fallar con archivos corruptos o rutas inaccesibles.
            // En ese caso intentar el fallback externo antes de rendirnos.
            try {
                getExternalCover(path)
            } catch (_: Exception) {
                null
            }
        }
    }

    /**
     * Busca una imagen de carátula en la misma carpeta del archivo de audio.
     * Nombres comunes: cover.jpg, folder.jpg, front.png, album.jpg, etc.
     */
    private fun getExternalCover(songPath: String): Bitmap? {
        val file = File(songPath)
        val parent = file.parentFile ?: return null

        val coverFiles = parent.listFiles { _, name ->
            val lower = name.lowercase()
            lower.contains("cover") ||
            lower.contains("folder") ||
            lower.contains("front") ||
            lower.contains("album")
        }

        val imageFile = coverFiles?.firstOrNull {
            it.extension.lowercase() in listOf("jpg", "jpeg", "png")
        }

        return imageFile?.let {
            try {
                BitmapFactory.decodeFile(it.absolutePath)
            } catch (_: Exception) {
                null
            }
        }
    }
}
