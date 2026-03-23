package com.lg.monkeymusicplayer.core.utils

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.media.MediaMetadataRetriever
import android.net.Uri
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

    // ── CORRECCIÓN: eliminado LruCache<Long, Bitmap> interno ──
    // Antes: SongCoverFetcher tenía su propio LruCache de 50 entradas, además de
    //        CoverCache.kt (otro LruCache global) y el cache interno de Coil.
    //        La misma imagen podía vivir en tres lugares simultáneamente, triplicando
    //        el uso de RAM sin ningún beneficio.
    // Ahora: Coil ya gestiona un cache de memoria y disco eficientemente. El Fetcher
    //        solo se encarga de la extracción del archivo cuando Coil lo necesite.
    //        Coil cachea el resultado automáticamente y no llamará a fetch() de nuevo
    //        para la misma URI mientras el bitmap esté en su cache.

    override suspend fun fetch(): FetchResult? {
        // Extraer el artwork embebido en el archivo de audio usando MediaMetadataRetriever.
        // .use{} garantiza release() aunque setDataSource() lance excepción.
        return MediaMetadataRetriever().use { retriever ->
            try {
                retriever.setDataSource(context, uri)
                val picture = retriever.embeddedPicture ?: return@use null
                val bitmap = BitmapFactory.decodeByteArray(picture, 0, picture.size)
                    ?: return@use null

                DrawableResult(
                    drawable = BitmapDrawable(context.resources, bitmap),
                    isSampled = false,
                    dataSource = DataSource.DISK
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    class Factory(private val context: Context) : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            // Solo interceptar URIs de MediaStore audio
            if (data.authority == "media" && data.pathSegments.contains("audio")) {
                return SongCoverFetcher(context, data)
            }
            return null
        }
    }
}
