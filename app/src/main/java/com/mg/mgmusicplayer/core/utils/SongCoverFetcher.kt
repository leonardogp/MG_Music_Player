package com.mg.mgmusicplayer.core.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.LruCache
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options

class SongCoverFetcher(
    private val context: Context,
    private val uri: Uri
) : Fetcher {

    companion object {
        // Cache en memoria para evitar re-extraer imágenes de los archivos
        private val coverCache = LruCache<Long, Bitmap>(50)
    }

    override suspend fun fetch(): FetchResult? {
        val songId = uri.lastPathSegment?.toLongOrNull()
        
        // 1. Intentar obtener de la caché
        if (songId != null) {
            val cachedBitmap = coverCache.get(songId)
            if (cachedBitmap != null) {
                return DrawableResult(
                    drawable = BitmapDrawable(context.resources, cachedBitmap),
                    isSampled = false,
                    dataSource = DataSource.MEMORY
                )
            }
        }

        // 2. Si no está en caché, extraer usando MediaMetadataRetriever
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val picture = retriever.embeddedPicture
            if (picture != null) {
                val bitmap = BitmapFactory.decodeByteArray(picture, 0, picture.size)
                if (bitmap != null) {
                    // Guardar en caché para futuras peticiones
                    if (songId != null) {
                        coverCache.put(songId, bitmap)
                    }
                    DrawableResult(
                        drawable = BitmapDrawable(context.resources, bitmap),
                        isSampled = false,
                        dataSource = DataSource.DISK
                    )
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        } finally {
            retriever.release()
        }
    }

    class Factory(private val context: Context) : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            // Manejar URIs que vienen de MediaStore audio
            if (data.authority == "media" && data.pathSegments.contains("audio")) {
                return SongCoverFetcher(context, data)
            }
            return null
        }
    }
}
