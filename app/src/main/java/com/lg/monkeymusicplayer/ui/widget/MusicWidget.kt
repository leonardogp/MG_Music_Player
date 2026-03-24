package com.lg.monkeymusicplayer.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.RemoteViews
import androidx.media3.common.util.UnstableApi
import com.lg.monkeymusicplayer.R
import com.lg.monkeymusicplayer.core.player.MusicService
import com.lg.monkeymusicplayer.ui.MainActivity
import java.io.InputStream

/**
 * MusicWidget: Provee controles básicos en la pantalla de inicio.
 * 
 * NOTA DE ARQUITECTURA: Los BroadcastReceivers (como AppWidgetProvider) tienen
 * un ciclo de vida corto y NO pueden usar bindService() directamente. Intentar
 * crear un MediaController dentro de onUpdate lanza ReceiverCallNotAllowedException.
 * 
 * SOLUCIÓN: El widget solo configura los PendingIntents. Las actualizaciones
 * de estado (título, arte, play/pause) deben ser empujadas desde el MusicService
 * hacia el widget cuando el estado del reproductor cambie.
 */
@UnstableApi
open class MusicWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.music_widget)
            
            // Configurar botones para enviar comandos al Service
            views.setOnClickPendingIntent(R.id.widget_play_pause, getServicePendingIntent(context, MusicService.ACTION_WIDGET_PLAY_PAUSE))
            views.setOnClickPendingIntent(R.id.widget_next, getServicePendingIntent(context, MusicService.ACTION_WIDGET_NEXT))
            views.setOnClickPendingIntent(R.id.widget_prev, getServicePendingIntent(context, MusicService.ACTION_WIDGET_PREV))

            // Abrir la app al tocar el contenedor
            val mainIntent = Intent(context, MainActivity::class.java)
            val mainPI = PendingIntent.getActivity(context, 0, mainIntent, PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_container, mainPI)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
        
        // Solicitar actualización inicial al servicio al crear el widget
        val intent = Intent(context, MusicService::class.java).apply {
            action = MusicService.ACTION_WIDGET_UPDATE_REQUEST
        }
        context.startService(intent)
    }

    private fun getServicePendingIntent(context: Context, action: String): PendingIntent {
        val intent = Intent(context, MusicService::class.java).apply { this.action = action }
        return PendingIntent.getService(context, action.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE)
    }

    companion object {
        /**
         * Método estático para que el MusicService actualice el widget.
         */
        fun updateWidget(
            context: Context,
            songTitle: String?,
            artistName: String?,
            isPlaying: Boolean,
            albumArtUri: String?
        ) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, MusicWidget::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds.isEmpty()) return

            val views = RemoteViews(context.packageName, R.layout.music_widget)
            
            views.setTextViewText(R.id.widget_song_title, songTitle ?: "Monkey Player")
            views.setTextViewText(R.id.widget_song_artist, artistName ?: "Not playing")
            
            val playPauseIcon = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
            views.setImageViewResource(R.id.widget_play_pause, playPauseIcon)

            // Carga de arte del álbum
            if (albumArtUri != null) {
                try {
                    val uri = Uri.parse(albumArtUri)
                    val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    if (bitmap != null) {
                        views.setImageViewBitmap(R.id.widget_album_art, bitmap)
                    } else {
                        views.setImageViewResource(R.id.widget_album_art, R.drawable.ic_monkey_head)
                    }
                } catch (e: Exception) {
                    views.setImageViewResource(R.id.widget_album_art, R.drawable.ic_monkey_head)
                }
            } else {
                views.setImageViewResource(R.id.widget_album_art, R.drawable.ic_monkey_head)
            }

            appWidgetManager.updateAppWidget(componentName, views)
        }
    }
}
