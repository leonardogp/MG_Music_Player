package com.lg.monkeymusicplayer.core.player

import android.app.PendingIntent
import android.content.Intent
import android.media.audiofx.Equalizer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.lg.monkeymusicplayer.ui.MainActivity

@UnstableApi
class MusicService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer
    private var equalizer: Equalizer? = null
    
    private val handler = Handler(Looper.getMainLooper())
    private var crossfadeDurationMs = 5000L
    private var isFading = false

    companion object {
        const val COMMAND_GET_AUDIO_SESSION_ID = "COMMAND_GET_AUDIO_SESSION_ID"
        const val COMMAND_SET_CROSSFADE_DURATION = "COMMAND_SET_CROSSFADE_DURATION"
        const val COMMAND_SET_EQUALIZER_BAND = "COMMAND_SET_EQUALIZER_BAND"
        const val COMMAND_GET_EQUALIZER_DATA = "COMMAND_GET_EQUALIZER_DATA"
    }

    override fun onCreate() {
        super.onCreate()
        
        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(), 
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()

        player.addListener(object : Player.Listener {
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
            }
        })

        handler.post(object : Runnable {
            override fun run() {
                if (player.isPlaying && !isFading && crossfadeDurationMs > 0) {
                    val remaining = player.duration - player.currentPosition
                    if (remaining in 1..crossfadeDurationMs) {
                        performFadeOut()
                    }
                }
                handler.postDelayed(this, 500)
            }
        })

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(pendingIntent)
            .setCallback(CustomMediaSessionCallback())
            .build()
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
        val startVolume = 1.0f
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
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS, resultBundle))
                }
                COMMAND_SET_CROSSFADE_DURATION -> {
                    crossfadeDurationMs = args.getLong("duration_ms", 5000L)
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                COMMAND_SET_EQUALIZER_BAND -> {
                    val band = args.getShort("band", -1)
                    val level = args.getShort("level", 0)
                    if (band >= 0) {
                        equalizer?.setBandLevel(band, level)
                    }
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                COMMAND_GET_EQUALIZER_DATA -> {
                    val eq = equalizer
                    if (eq != null) {
                        val numBands = eq.numberOfBands
                        val minLevel = eq.bandLevelRange[0]
                        val maxLevel = eq.bandLevelRange[1]
                        val bands = IntArray(numBands.toInt()) { i -> eq.getCenterFreq(i.toShort()) / 1000 }
                        val levels = ShortArray(numBands.toInt()) { i -> eq.getBandLevel(i.toShort()) }
                        
                        val resultBundle = Bundle().apply {
                            putShort("num_bands", numBands)
                            putShort("min_level", minLevel)
                            putShort("max_level", maxLevel)
                            putIntArray("center_freqs", bands)
                            putShortArray("band_levels", levels)
                        }
                        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS, resultBundle))
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
        if (!player.playWhenReady || player.mediaItemCount == 0 || player.playbackState == Player.STATE_IDLE) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        equalizer?.release()
        player.release()
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }
}
