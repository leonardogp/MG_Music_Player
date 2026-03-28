package com.lg.monkeymusicplayer.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.net.Uri
import android.widget.RemoteViews
import androidx.media3.common.util.UnstableApi
import com.lg.monkeymusicplayer.R
import com.lg.monkeymusicplayer.core.player.MusicService
import com.lg.monkeymusicplayer.ui.MainActivity
import java.io.InputStream

/**
 * MusicWidget — widget de pantalla de inicio para Monkey Music Player.
 *
 * ARQUITECTURA:
 * AppWidgetProvider es un BroadcastReceiver de ciclo de vida muy corto;
 * no puede bindService() ni mantener estado. Todo update de datos
 * (canción, portada, play/pause, progreso) lo empuja MusicService → updateAllWidgets().
 *
 * VARIANTES:
 *   MusicWidget2x1  → music_widget_small   : portada + título + play/pause
 *   MusicWidget2x2  → music_widget_square  : portada full-bleed + controles superpuestos
 *   MusicWidget4x1  → music_widget         : portada + texto + prev/play/next
 *   MusicWidget4x2  → music_widget_medium  : portada + texto + progreso + controles + fav
 *   MusicWidget4x4  → music_widget_large   : portada dominante + progreso + shuffle/repeat + fav
 *
 * PORTADA: redondeada con Canvas + PorterDuffXfermode (RemoteViews no soporta clipToOutline
 * en runtime). Escalada a ≤256px para no exceder el límite del Binder (~1 MB por update).
 */
@UnstableApi
open class MusicWidget : AppWidgetProvider() {

    open val widgetLayout: Int = R.layout.music_widget

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, widgetLayout)
            bindClickListeners(context, views)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
        context.startService(
            Intent(context, MusicService::class.java).apply {
                action = MusicService.ACTION_WIDGET_UPDATE_REQUEST
            }
        )
    }

    private fun bindClickListeners(context: Context, views: RemoteViews) {
        val openApp = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_container, openApp)
        views.setOnClickPendingIntent(R.id.widget_play_pause, svcPI(context, MusicService.ACTION_WIDGET_PLAY_PAUSE))
        safeSetPI(views, R.id.widget_prev,     svcPI(context, MusicService.ACTION_WIDGET_PREV))
        safeSetPI(views, R.id.widget_next,     svcPI(context, MusicService.ACTION_WIDGET_NEXT))
        safeSetPI(views, R.id.widget_favorite, svcPI(context, MusicService.ACTION_WIDGET_FAVORITE))
        safeSetPI(views, R.id.widget_shuffle,  svcPI(context, MusicService.ACTION_WIDGET_SHUFFLE))
        safeSetPI(views, R.id.widget_repeat,   svcPI(context, MusicService.ACTION_WIDGET_REPEAT))
    }

    companion object {

        /**
         * Actualiza todas las variantes de widget activas.
         * Llamado desde MusicService al cambiar canción, play/pause, favorito, shuffle o progreso.
         *
         * @param progressMs     posición actual en ms
         * @param durationMs     duración total en ms
         * @param isFavorite     si la canción está en favoritos
         * @param isShuffleOn    estado shuffle
         * @param isRepeatOn     estado repeat (any mode != OFF)
         */
        fun updateAllWidgets(
            context: Context,
            songTitle: String?,
            artistName: String?,
            albumName: String?,
            isPlaying: Boolean,
            albumArtUri: String?,
            progressMs: Long = 0L,
            durationMs: Long = 0L,
            isFavorite: Boolean = false,
            isShuffleOn: Boolean = false,
            isRepeatOn: Boolean = false
        ) {
            val mgr = AppWidgetManager.getInstance(context)

            // Cargar y redondear la portada UNA vez para todos los providers
            val artBitmap = loadRoundedAlbumArt(context, albumArtUri, cornerRadiusDp = 14f)

            val providers = listOf(
                MusicWidget2x1::class.java to R.layout.music_widget_small,
                MusicWidget2x2::class.java to R.layout.music_widget_square,
                MusicWidget4x1::class.java to R.layout.music_widget,
                MusicWidget4x2::class.java to R.layout.music_widget_medium,
                MusicWidget4x4::class.java to R.layout.music_widget_large
            )

            for ((cls, layoutId) in providers) {
                val ids = mgr.getAppWidgetIds(ComponentName(context, cls))
                if (ids.isEmpty()) continue

                val views = RemoteViews(context.packageName, layoutId)

                // ── Portada ─────────────────────────────────────────────────
                if (artBitmap != null) {
                    views.setImageViewBitmap(R.id.widget_album_art, artBitmap)
                } else {
                    views.setImageViewResource(R.id.widget_album_art, R.drawable.ic_monkey_head)
                }

                // ── Textos ──────────────────────────────────────────────────
                views.setTextViewText(R.id.widget_song_title, songTitle ?: "Monkey Player")
                safeSetText(views, R.id.widget_song_artist, artistName ?: "Not playing")
                safeSetText(views, R.id.widget_song_album, albumName ?: "")

                // ── Play/pause: icono negro sobre fondo blanco ──────────────
                views.setImageViewResource(
                    R.id.widget_play_pause,
                    if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                )
                // Tint negro para que el icono contraste sobre el círculo blanco
                views.setInt(R.id.widget_play_pause, "setColorFilter", 0xFF000000.toInt())

                // ── Favorito ────────────────────────────────────────────────
                safeSetImageRes(views, R.id.widget_favorite,
                    if (isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border)

                // ── Shuffle (4x4 y 4x2 no lo tiene, solo 4x4) ──────────────
                // Alpha 255 = activo (blanco pleno), 80 = inactivo (translúcido)
                safeSetAlpha(views, R.id.widget_shuffle, if (isShuffleOn) 255 else 80)

                // ── Repeat (4x4) ────────────────────────────────────────────
                safeSetAlpha(views, R.id.widget_repeat, if (isRepeatOn) 255 else 80)

                // ── Progreso + tiempos (4x2 y 4x4) ─────────────────────────
                val progress = if (durationMs > 0L)
                    ((progressMs.toFloat() / durationMs) * 1000).toInt().coerceIn(0, 1000)
                else 0
                safeSetProgress(views, R.id.widget_progress, progress)
                safeSetText(views, R.id.widget_time_current, formatTime(progressMs))
                safeSetText(views, R.id.widget_time_total,   formatTime(durationMs))

                // ── PendingIntents ───────────────────────────────────────────
                bindAllPendingIntents(context, views)

                mgr.updateAppWidget(ids, views)
            }
        }

        // ── PendingIntents ───────────────────────────────────────────────────

        private fun bindAllPendingIntents(context: Context, views: RemoteViews) {
            val openApp = PendingIntent.getActivity(
                context, 0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_container, openApp)
            views.setOnClickPendingIntent(R.id.widget_play_pause, svcPI(context, MusicService.ACTION_WIDGET_PLAY_PAUSE))
            safeSetPI(views, R.id.widget_prev,     svcPI(context, MusicService.ACTION_WIDGET_PREV))
            safeSetPI(views, R.id.widget_next,     svcPI(context, MusicService.ACTION_WIDGET_NEXT))
            safeSetPI(views, R.id.widget_favorite, svcPI(context, MusicService.ACTION_WIDGET_FAVORITE))
            safeSetPI(views, R.id.widget_shuffle,  svcPI(context, MusicService.ACTION_WIDGET_SHUFFLE))
            safeSetPI(views, R.id.widget_repeat,   svcPI(context, MusicService.ACTION_WIDGET_REPEAT))
        }

        private fun svcPI(context: Context, action: String) = PendingIntent.getService(
            context, action.hashCode(),
            Intent(context, MusicService::class.java).apply { this.action = action },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // ── RemoteViews helpers (no lanzan si el id no existe en ese layout) ─

        private fun safeSetPI(views: RemoteViews, id: Int, pi: android.app.PendingIntent) {
            try { views.setOnClickPendingIntent(id, pi) } catch (_: Exception) {}
        }
        private fun safeSetText(views: RemoteViews, id: Int, text: String) {
            try { views.setTextViewText(id, text) } catch (_: Exception) {}
        }
        private fun safeSetImageRes(views: RemoteViews, id: Int, resId: Int) {
            try { views.setImageViewResource(id, resId) } catch (_: Exception) {}
        }
        private fun safeSetAlpha(views: RemoteViews, id: Int, alpha: Int) {
            try { views.setInt(id, "setAlpha", alpha) } catch (_: Exception) {}
        }
        private fun safeSetProgress(views: RemoteViews, id: Int, progress: Int) {
            try { views.setProgressBar(id, 1000, progress, false) } catch (_: Exception) {}
        }

        // ── Album art ────────────────────────────────────────────────────────

        /**
         * Carga la portada desde MediaStore, la escala a ≤256px y la redondea con Canvas.
         * RemoteViews no soporta clipToOutline en runtime, por lo que el redondeo
         * debe estar en el Bitmap antes de entregarlo.
         */
        private fun loadRoundedAlbumArt(
            context: Context,
            albumArtUri: String?,
            cornerRadiusDp: Float
        ): Bitmap? {
            albumArtUri ?: return null
            return try {
                val stream: InputStream =
                    context.contentResolver.openInputStream(Uri.parse(albumArtUri)) ?: return null
                val raw = BitmapFactory.decodeStream(stream)
                stream.close()
                raw?.let { roundBitmap(it, cornerRadiusDp, context) }
            } catch (_: Exception) {
                null
            }
        }

        private fun roundBitmap(src: Bitmap, cornerDp: Float, context: Context): Bitmap {
            val maxPx = 256
            val scale = minOf(maxPx.toFloat() / src.width, maxPx.toFloat() / src.height, 1f)
            val w = (src.width * scale).toInt().coerceAtLeast(1)
            val h = (src.height * scale).toInt().coerceAtLeast(1)
            val scaled = Bitmap.createScaledBitmap(src, w, h, true)
            val cornerPx = cornerDp * context.resources.displayMetrics.density
            val output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            canvas.drawRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), cornerPx, cornerPx, paint)
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            canvas.drawBitmap(scaled, 0f, 0f, paint)
            if (scaled != src) scaled.recycle()
            return output
        }

        private fun formatTime(ms: Long): String {
            val s = ms / 1000
            return "%d:%02d".format(s / 60, s % 60)
        }
    }
}
