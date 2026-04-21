package com.lg.monkeymusicplayer.core.cast

import android.content.Context
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.MediaQueueItem
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.CastState
import com.google.android.gms.cast.framework.CastStateListener
import com.google.android.gms.cast.framework.SessionManagerListener
import com.lg.monkeymusicplayer.data.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * CastManager — fachada sobre el Cast SDK de Google.
 *
 * ## Responsabilidades
 * - Detectar si hay dispositivos Cast disponibles en la red local.
 * - Mantener el estado de la sesión Cast activa.
 * - Cargar canciones y colas en el receptor remoto.
 * - Exponer [castState] y [isConnected] como Flows para que la UI reaccione.
 *
 * ## Integración en la UI
 * 1. El botón MediaRoute se añade al TopAppBar con `MediaRouteButton` (View).
 *    Para Compose, usar `AndroidView { MediaRouteButton(it) }`.
 * 2. Observar [isConnected]: cuando es true, el player local debe pausarse
 *    y la reproducción pasa al receptor Cast.
 *
 * ## Requisito de configuración
 * Añadir en `res/values/cast_options.xml` (o en strings.xml):
 * ```xml
 * <string name="cast_app_id">CC1AD845</string>  <!-- Default Media Receiver -->
 * ```
 * Y registrar `CastOptionsProvider` en el Manifest:
 * ```xml
 * <meta-data
 *     android:name="com.google.android.gms.cast.framework.OPTIONS_PROVIDER_CLASS_NAME"
 *     android:value="com.lg.monkeymusicplayer.core.cast.CastOptionsProvider" />
 * ```
 *
 * ## Nota sobre la arquitectura
 * CastManager NO controla directamente al `MusicPlayerManager`. El ViewModel
 * observa [isConnected] y coordina: pausa local cuando Cast se conecta,
 * reanuda local cuando Cast se desconecta.
 */
@Singleton
class CastManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    // ── Estado público ───────────────────────────────────────────────────────

    private val _castState = MutableStateFlow(CastState.NO_DEVICES_AVAILABLE)
    /** Estado actual del framework Cast. Ver [CastState] para los valores posibles. */
    val castState: StateFlow<Int> = _castState.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    /** True cuando hay una sesión Cast activa y el receptor está listo. */
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _currentSongId = MutableStateFlow<Long?>(null)
    /** ID de la canción que se está reproduciendo remotamente. */
    val currentRemoteSongId: StateFlow<Long?> = _currentSongId.asStateFlow()

    // ── Internos ─────────────────────────────────────────────────────────────

    private var castContext: CastContext? = null
    private var activeSession: CastSession? = null

    private val castStateListener = CastStateListener { state ->
        _castState.value = state
        Timber.d("CastManager: state=$state")
    }

    private val sessionListener = object : SessionManagerListener<CastSession> {
        override fun onSessionStarted(session: CastSession, sessionId: String) {
            activeSession = session
            _isConnected.value = true
            Timber.d("CastManager: session started — id=$sessionId")
        }

        override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
            activeSession = session
            _isConnected.value = true
            Timber.d("CastManager: session resumed")
        }

        override fun onSessionEnded(session: CastSession, error: Int) {
            activeSession = null
            _isConnected.value = false
            _currentSongId.value = null
            Timber.d("CastManager: session ended — error=$error")
        }

        override fun onSessionSuspended(session: CastSession, reason: Int) {
            _isConnected.value = false
            Timber.d("CastManager: session suspended — reason=$reason")
        }

        // Callbacks de inicio/fin con error (requeridos por la interfaz)
        override fun onSessionStarting(session: CastSession) {}
        override fun onSessionStartFailed(session: CastSession, error: Int) {
            Timber.w("CastManager: session start failed — error=$error")
        }
        override fun onSessionEnding(session: CastSession) {}
        override fun onSessionResuming(session: CastSession, sessionId: String) {}
        override fun onSessionResumeFailed(session: CastSession, error: Int) {
            Timber.w("CastManager: session resume failed — error=$error")
        }
    }

    // ── Ciclo de vida ────────────────────────────────────────────────────────

    /**
     * Inicializa el CastContext. Debe llamarse desde [MainActivity.onCreate].
     * CastContext requiere el hilo principal y un contexto de Activity.
     *
     * Si Google Play Services no está disponible (ej. emulador sin GMS),
     * la inicialización falla silenciosamente — la app sigue funcionando sin Cast.
     */
    fun initialize() {
        try {
            castContext = CastContext.getSharedInstance(context)
            castContext?.addCastStateListener(castStateListener)
            castContext?.sessionManager?.addSessionManagerListener(sessionListener, CastSession::class.java)
            Timber.d("CastManager: initialized")
        } catch (e: Exception) {
            Timber.w("CastManager: Cast not available — ${e.message}")
        }
    }

    /** Limpia los listeners. Llamar desde [MainActivity.onDestroy]. */
    fun release() {
        castContext?.removeCastStateListener(castStateListener)
        castContext?.sessionManager?.removeSessionManagerListener(sessionListener, CastSession::class.java)
        activeSession = null
    }

    // ── Reproducción remota ──────────────────────────────────────────────────

    /**
     * Carga una canción en el receptor Cast activo.
     * No hace nada si no hay sesión activa.
     *
     * @param song     La canción a reproducir.
     * @param autoPlay true para iniciar reproducción automáticamente.
     */
    fun loadSong(song: Song, autoPlay: Boolean = true) {
        val client = activeSession?.remoteMediaClient ?: return
        val mediaInfo = song.toMediaInfo()
        val request = MediaLoadRequestData.Builder()
            .setMediaInfo(mediaInfo)
            .setAutoplay(autoPlay)
            .build()
        client.load(request)
            .addStatusListener { status ->
                if (status.isSuccess) {
                    _currentSongId.value = song.id
                    Timber.d("CastManager: loaded song '${song.title}'")
                } else {
                    Timber.w("CastManager: load failed — ${status.statusMessage}")
                }
            }
    }

    /**
     * Carga una cola de canciones en el receptor Cast.
     * La reproducción comienza desde [startIndex].
     */
    fun loadQueue(songs: List<Song>, startIndex: Int = 0, autoPlay: Boolean = true) {
        val client = activeSession?.remoteMediaClient ?: return
        if (songs.isEmpty()) return

        val queueItems = songs.map { song ->
            MediaQueueItem.Builder(song.toMediaInfo()).build()
        }
        client.queueLoad(
            queueItems.toTypedArray(),
            startIndex,
            com.google.android.gms.cast.MediaStatus.REPEAT_MODE_REPEAT_OFF,
            null
        ).addStatusListener { status ->
            if (status.isSuccess) {
                _currentSongId.value = songs.getOrNull(startIndex)?.id
                Timber.d("CastManager: queue loaded (${songs.size} songs, start=$startIndex)")
            } else {
                Timber.w("CastManager: queue load failed — ${status.statusMessage}")
            }
        }
    }

    fun pause() { activeSession?.remoteMediaClient?.pause() }
    fun play()  { activeSession?.remoteMediaClient?.play()  }
    fun stop()  { activeSession?.remoteMediaClient?.stop()  }
    fun seekTo(positionMs: Long) { activeSession?.remoteMediaClient?.seek(positionMs) }

    // ── Mapper ───────────────────────────────────────────────────────────────

    private fun Song.toMediaInfo(): MediaInfo {
        val metadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MUSIC_TRACK).apply {
            putString(MediaMetadata.KEY_TITLE, title)
            putString(MediaMetadata.KEY_ARTIST, artist)
            putString(MediaMetadata.KEY_ALBUM_TITLE, album)
        }
        return MediaInfo.Builder(path)
            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
            .setContentType("audio/mpeg")
            .setMetadata(metadata)
            .build()
    }
}
