package com.lg.monkeymusicplayer.core.player

import android.app.PendingIntent
import android.content.Intent
import android.media.audiofx.Equalizer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.lg.monkeymusicplayer.data.database.MusicDao
import com.lg.monkeymusicplayer.data.database.SongEntity
import com.lg.monkeymusicplayer.data.model.Song
import com.lg.monkeymusicplayer.ui.MainActivity
import com.lg.monkeymusicplayer.ui.widget.MusicWidget
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

@UnstableApi
@AndroidEntryPoint
class MusicService : MediaSessionService() {

    @Inject
    lateinit var musicDao: MusicDao

    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer
    private var equalizer: Equalizer? = null

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val handler = Handler(Looper.getMainLooper())
    private var crossfadeDurationMs = 5000L
    private var isFading = false
    private var widgetUpdateJob: Job? = null

    private val crossfadeCheckRunnable = object : Runnable {
        override fun run() {
            if (player.isPlaying && !isFading && crossfadeDurationMs > 0) {
                val remaining = player.duration - player.currentPosition
                if (remaining in 1..crossfadeDurationMs) {
                    performFadeOut()
                }
            }
            handler.postDelayed(this, 500)
        }
    }

    companion object {
        const val COMMAND_GET_AUDIO_SESSION_ID = "COMMAND_GET_AUDIO_SESSION_ID"
        const val COMMAND_SET_CROSSFADE_DURATION = "COMMAND_SET_CROSSFADE_DURATION"
        const val COMMAND_SET_EQUALIZER_BAND = "COMMAND_SET_EQUALIZER_BAND"
        const val COMMAND_GET_EQUALIZER_DATA = "COMMAND_GET_EQUALIZER_DATA"
        const val COMMAND_FADE_AND_PAUSE = "COMMAND_FADE_AND_PAUSE"
        /** Duración del fade out del sleep timer en ms. */
        const val SLEEP_FADE_DURATION_MS = 30_000L

        const val ACTION_WIDGET_PLAY_PAUSE = "com.lg.monkeymusicplayer.ACTION_WIDGET_PLAY_PAUSE"
        const val ACTION_WIDGET_NEXT = "com.lg.monkeymusicplayer.ACTION_WIDGET_NEXT"
        const val ACTION_WIDGET_PREV = "com.lg.monkeymusicplayer.ACTION_WIDGET_PREV"
        const val ACTION_WIDGET_UPDATE_REQUEST = "com.lg.monkeymusicplayer.ACTION_WIDGET_UPDATE_REQUEST"
        const val ACTION_WIDGET_FAVORITE = "com.lg.monkeymusicplayer.ACTION_WIDGET_FAVORITE"
        const val ACTION_WIDGET_SHUFFLE  = "com.lg.monkeymusicplayer.ACTION_WIDGET_SHUFFLE"
        const val ACTION_WIDGET_REPEAT   = "com.lg.monkeymusicplayer.ACTION_WIDGET_REPEAT"
    }

    override fun onCreate() {
        super.onCreate()

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(32 * 1024, 64 * 1024, 1024, 1024)
            .setBackBuffer(10 * 1024, true)
            .build()

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .setLoadControl(loadControl)
            .build()

        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                updateWidget()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updateWidget()
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                if (reason == Player.DISCONTINUITY_REASON_AUTO_TRANSITION) {
                    performFadeIn()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY && equalizer == null) {
                    setupEqualizer()
                }
                updateWidget()
            }
        })

        handler.post(crossfadeCheckRunnable)

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(pendingIntent)
            .setCallback(CustomMediaSessionCallback())
            .build()

        // Restaurar la última sesión al arrancar el servicio.
        // Esto garantiza que el reproductor tenga los MediaItems cargados
        // desde el primer momento — el PlayerBottomBar mostrará la última
        // canción incluso antes de que el usuario toque algo.
        // No llamamos player.play() — solo preparamos la cola sin reproducir.
        restoreLastSession()
    }

    /**
     * Carga los MediaItems del historial en el player sin iniciar reproducción.
     * Permite que el controller sincronice el estado (canción, cola) en cuanto
     * se conecta, sin necesidad de que el usuario interactúe primero.
     */
    private fun restoreLastSession() {
        serviceScope.launch {
            val history = withContext(Dispatchers.IO) {
                musicDao.getHistory().firstOrNull() ?: emptyList()
            }
            if (history.isEmpty()) return@launch

            val songIds = history.map { it.songId }
            val songEntities = withContext(Dispatchers.IO) {
                musicDao.getSongsByIds(songIds)
            }
            val songs = songIds.mapNotNull { id ->
                songEntities.find { it.id == id }?.toDomainModel()
            }
            if (songs.isEmpty()) return@launch

            // Solo cargar si el player no tiene ya items (evitar sobreescribir
            // una sesión activa si el servicio no fue destruido entre sesiones)
            if (player.mediaItemCount == 0) {
                val mediaItems = songs.map { song ->
                    MediaItem.Builder()
                        .setMediaId(song.id.toString())
                        .setUri(song.path)
                        .setTag(song)
                        .build()
                }
                player.setMediaItems(mediaItems)
                player.prepare()
                // No llamar player.play() — estado inicial es pausado
            }
        }
    }

    private fun updateWidget() {
        // Capturar la canción actual en Main antes de lanzar la corrutina.
        val song = player.currentMediaItem?.localConfiguration?.tag as? Song

        // Cancelar cualquier update pendiente — evita que un evento anterior
        // (e.g. onIsPlayingChanged disparado justo antes de onMediaItemTransition)
        // sobreescriba al widget con datos de la canción ya abandonada.
        widgetUpdateJob?.cancel()
        widgetUpdateJob = serviceScope.launch {
            if (song != null) {
                val isFav = withContext(Dispatchers.IO) {
                    musicDao.getFavorites().firstOrNull()?.contains(song.id) ?: false
                }

                // Guard post-IO: si la canción cambió mientras esperábamos el DAO,
                // descartar este update para no pintar información stale.
                val stillCurrent = player.currentMediaItem?.localConfiguration?.tag as? Song
                if (stillCurrent?.id != song.id) return@launch

                MusicWidget.updateAllWidgets(
                    context        = this@MusicService,
                    songTitle      = song.title,
                    artistName     = song.artist,
                    albumName      = song.album,
                    isPlaying      = player.isPlaying,
                    albumArtUri    = song.albumArtUri,
                    progressMs     = player.currentPosition,
                    durationMs     = player.duration.coerceAtLeast(0L),
                    isFavorite     = isFav,
                    isShuffleOn    = player.shuffleModeEnabled,
                    isRepeatOn     = player.repeatMode != androidx.media3.common.Player.REPEAT_MODE_OFF
                )
            } else {
                // Sin canción activa: mostrar último registro del historial (idle state)
                val lastEntity = withContext(Dispatchers.IO) {
                    musicDao.getHistory().firstOrNull()?.firstOrNull()
                        ?.let { hist -> musicDao.getSongsByIds(listOf(hist.songId)).firstOrNull() }
                }
                val last = lastEntity?.toDomainModel()
                MusicWidget.updateAllWidgets(
                    context     = this@MusicService,
                    songTitle   = last?.title,
                    artistName  = last?.artist,
                    albumName   = last?.album,
                    isPlaying   = false,
                    albumArtUri = last?.albumArtUri
                )
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_WIDGET_PLAY_PAUSE -> {
                if (player.mediaItemCount == 0) {
                    restoreLastSessionAndPlay()
                } else {
                    if (player.isPlaying) player.pause() else player.play()
                }
            }
            ACTION_WIDGET_NEXT    -> player.seekToNext()
            ACTION_WIDGET_PREV    -> player.seekToPrevious()
            ACTION_WIDGET_SHUFFLE -> {
                player.shuffleModeEnabled = !player.shuffleModeEnabled
                updateWidget()
            }
            ACTION_WIDGET_REPEAT -> {
                // Ciclar: OFF → ALL → ONE → OFF
                player.repeatMode = when (player.repeatMode) {
                    androidx.media3.common.Player.REPEAT_MODE_OFF -> androidx.media3.common.Player.REPEAT_MODE_ALL
                    androidx.media3.common.Player.REPEAT_MODE_ALL -> androidx.media3.common.Player.REPEAT_MODE_ONE
                    else -> androidx.media3.common.Player.REPEAT_MODE_OFF
                }
                updateWidget()
            }
            ACTION_WIDGET_FAVORITE -> {
                // Toggle favorito de la canción actual a través del DAO en IO
                val song = player.currentMediaItem?.localConfiguration?.tag as? Song
                if (song != null) {
                    serviceScope.launch {
                        val isFav = withContext(Dispatchers.IO) {
                            musicDao.getFavorites().firstOrNull()?.contains(song.id) ?: false
                        }
                        withContext(Dispatchers.IO) {
                            if (isFav) musicDao.deleteFavorite(
                                com.lg.monkeymusicplayer.data.database.FavoriteEntity(song.id)
                            ) else musicDao.insertFavorite(
                                com.lg.monkeymusicplayer.data.database.FavoriteEntity(song.id)
                            )
                        }
                        updateWidget()
                    }
                }
            }
            ACTION_WIDGET_UPDATE_REQUEST -> updateWidget()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun restoreLastSessionAndPlay() {
        serviceScope.launch {
            // restoreLastSession() es una función regular que lanza su propia corrutina.
            // Esperamos a que el player tenga items antes de reproducir usando un delay
            // corto y verificando el estado, o reutilizando la lógica inline.
            if (player.mediaItemCount == 0) {
                val history = withContext(Dispatchers.IO) {
                    musicDao.getHistory().firstOrNull() ?: emptyList()
                }
                if (history.isNotEmpty()) {
                    val songIds = history.map { it.songId }
                    val songEntities = withContext(Dispatchers.IO) {
                        musicDao.getSongsByIds(songIds)
                    }
                    val songs = songIds.mapNotNull { id ->
                        songEntities.find { it.id == id }?.toDomainModel()
                    }
                    if (songs.isNotEmpty()) {
                        val mediaItems = songs.map { song ->
                            MediaItem.Builder()
                                .setMediaId(song.id.toString())
                                .setUri(song.path)
                                .setTag(song)
                                .build()
                        }
                        player.setMediaItems(mediaItems)
                        player.prepare()
                    }
                }
            }
            if (player.mediaItemCount > 0) {
                player.play()
            }
        }
    }

    private fun setupEqualizer() {
        try {
            equalizer = Equalizer(0, player.audioSessionId)
            equalizer?.enabled = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun performFadeOut() {
        isFading = true
        val startVolume = player.volume
        val steps = 20
        val interval = crossfadeDurationMs / steps

        for (i in 0..steps) {
            handler.postDelayed({
                if (isFading) {
                    player.volume = startVolume * (1.0f - i.toFloat() / steps)
                }
            }, i * interval)
        }
    }

    /**
     * Fade out dedicado al sleep timer: baja el volumen durante [SLEEP_FADE_DURATION_MS]
     * y luego pausa la reproducción y restaura el volumen a 1.0f.
     * Es independiente del crossfade entre canciones.
     */
    private fun performSleepFadeOut() {
        val startVolume = player.volume
        val steps = 60  // 1 paso cada 500ms → 30 segundos total
        val interval = SLEEP_FADE_DURATION_MS / steps

        for (i in 0..steps) {
            handler.postDelayed({
                val newVolume = startVolume * (1.0f - i.toFloat() / steps)
                player.volume = newVolume.coerceAtLeast(0f)
                if (i == steps) {
                    player.pause()
                    // Restaurar volumen para que la próxima reproducción no empiece en silencio
                    handler.postDelayed({ player.volume = 1.0f }, 500)
                }
            }, i * interval)
        }
    }

    private fun performFadeIn() {
        isFading = true
        player.volume = 0f
        val steps = 20
        val interval = 100L

        for (i in 0..steps) {
            handler.postDelayed({
                player.volume = (i.toFloat() / steps)
                if (i == steps) isFading = false
            }, i * interval)
        }
    }

    private inner class CustomMediaSessionCallback : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val connectionResult = super.onConnect(session, controller)
            val availableSessionCommands = connectionResult.availableSessionCommands.buildUpon()
            availableSessionCommands.add(SessionCommand(COMMAND_GET_AUDIO_SESSION_ID, Bundle.EMPTY))
            availableSessionCommands.add(SessionCommand(COMMAND_SET_CROSSFADE_DURATION, Bundle.EMPTY))
            availableSessionCommands.add(SessionCommand(COMMAND_SET_EQUALIZER_BAND, Bundle.EMPTY))
            availableSessionCommands.add(SessionCommand(COMMAND_GET_EQUALIZER_DATA, Bundle.EMPTY))
            availableSessionCommands.add(SessionCommand(COMMAND_FADE_AND_PAUSE, Bundle.EMPTY))
            return MediaSession.ConnectionResult.accept(
                availableSessionCommands.build(),
                connectionResult.availablePlayerCommands
            )
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            when (customCommand.customAction) {
                COMMAND_GET_AUDIO_SESSION_ID -> {
                    val resultBundle = Bundle().apply {
                        putInt("audio_session_id", player.audioSessionId)
                    }
                    return Futures.immediateFuture(
                        SessionResult(SessionResult.RESULT_SUCCESS, resultBundle)
                    )
                }
                COMMAND_SET_CROSSFADE_DURATION -> {
                    crossfadeDurationMs = args.getLong("duration_ms", 5000L)
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                COMMAND_SET_EQUALIZER_BAND -> {
                    val band = args.getShort("band", -1)
                    val level = args.getShort("level", 0)
                    if (band >= 0) equalizer?.setBandLevel(band, level)
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                COMMAND_FADE_AND_PAUSE -> {
                    if (player.isPlaying) performSleepFadeOut()
                    else player.pause()
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                COMMAND_GET_EQUALIZER_DATA -> {
                    val eq = equalizer
                    if (eq != null) {
                        val numBands = eq.numberOfBands
                        val minLevel = eq.bandLevelRange[0]
                        val maxLevel = eq.bandLevelRange[1]
                        val bands = IntArray(numBands.toInt()) { i ->
                            eq.getCenterFreq(i.toShort()) / 1000
                        }
                        val levels = ShortArray(numBands.toInt()) { i ->
                            eq.getBandLevel(i.toShort())
                        }
                        val resultBundle = Bundle().apply {
                            putShort("num_bands", numBands)
                            putShort("min_level", minLevel)
                            putShort("max_level", maxLevel)
                            putIntArray("center_freqs", bands)
                            putShortArray("band_levels", levels)
                        }
                        return Futures.immediateFuture(
                            SessionResult(SessionResult.RESULT_SUCCESS, resultBundle)
                        )
                    }
                }
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED))
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!player.playWhenReady ||
            player.mediaItemCount == 0 ||
            player.playbackState == Player.STATE_IDLE
        ) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        handler.removeCallbacks(crossfadeCheckRunnable)
        equalizer?.release()
        equalizer = null
        player.release()
        mediaSession?.release()
        mediaSession = null
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private fun SongEntity.toDomainModel() = Song(
        id = id,
        albumId = albumId,
        title = title,
        artist = artist,
        album = album,
        genre = genre,
        folder = folder,
        path = path,
        albumArtUri = albumArtUri
    )
}
