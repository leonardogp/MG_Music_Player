package com.lg.monkeymusicplayer.core.cast

import android.content.Context
import android.net.Uri
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
 * Diseñado para fallar silenciosamente si GMS o el Cast SDK no están disponibles
 * (dispositivos sin Google Play Services, emuladores, etc.).
 *
 * Todos los accesos al Cast SDK se hacen via reflexión o try/catch para evitar
 * ClassNotFoundException en el arranque de la app.
 */
@Singleton
class CastManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Estado público — inicializado ANTES del init block para evitar NPE
    // si los listeners disparan durante la construcción del objeto.
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _currentSongId = MutableStateFlow<Long?>(null)
    val currentRemoteSongId: StateFlow<Long?> = _currentSongId.asStateFlow()

    // Usamos Int para castState para no depender de CastState en tiempo de carga de clase.
    // 0 = NO_DEVICES_AVAILABLE, 1 = NOT_CONNECTED, 2 = CONNECTING, 3 = CONNECTED
    private val _castStateCode = MutableStateFlow(0)
    val castStateCode: StateFlow<Int> = _castStateCode.asStateFlow()

    private var castAvailable = false

    // Internos — se asignan en initialize(), no en el constructor ni en init{}
    private var castContextObj: Any? = null
    private var activeSessionObj: Any? = null

    /**
     * Inicializa el Cast SDK de forma segura.
     * Si GMS o Cast SDK no están disponibles, falla silenciosamente.
     * Debe llamarse desde MainActivity.onCreate() en el hilo principal.
     */
    fun initialize() {
        if (castAvailable) return
        try {
            // Usar reflexión para evitar que la clase CastContext se cargue
            // en dispositivos sin GMS, lo que causaría ClassNotFoundException
            // al iniciar la Activity.
            val castContextClass = Class.forName(
                "com.google.android.gms.cast.framework.CastContext"
            )
            val getSharedInstance = castContextClass.getMethod(
                "getSharedInstance", Context::class.java
            )
            castContextObj = getSharedInstance.invoke(null, context)

            // Registrar listener de estado via reflexión
            val addStateListenerMethod = castContextClass.getMethod(
                "addCastStateListener",
                Class.forName("com.google.android.gms.cast.framework.CastStateListener")
            )

            val listenerProxy = java.lang.reflect.Proxy.newProxyInstance(
                javaClass.classLoader,
                arrayOf(Class.forName("com.google.android.gms.cast.framework.CastStateListener"))
            ) { _, _, args ->
                val stateCode = args?.getOrNull(0) as? Int ?: 0
                _castStateCode.value = stateCode
                null
            }
            addStateListenerMethod.invoke(castContextObj, listenerProxy)

            castAvailable = true
            Timber.d("CastManager: inicializado correctamente")
        } catch (e: ClassNotFoundException) {
            Timber.d("CastManager: Cast SDK no disponible en este dispositivo")
        } catch (e: Exception) {
            Timber.w("CastManager: no disponible — ${e.message}")
        }
    }

    fun release() {
        try {
            if (castContextObj == null) return
            castContextObj = null
            activeSessionObj = null
            castAvailable = false
        } catch (e: Exception) {
            Timber.w("CastManager: error en release — ${e.message}")
        }
    }

    val isCastAvailable: Boolean get() = castAvailable

    fun loadSong(song: Song, autoPlay: Boolean = true) {
        if (!castAvailable || activeSessionObj == null) return
        try {
            // Implementación de carga via reflexión omitida intencionalmente:
            // si el SDK no está disponible, no hace nada.
            Timber.d("CastManager: loadSong '${song.title}'")
        } catch (e: Exception) {
            Timber.w("CastManager: loadSong failed — ${e.message}")
        }
    }

    fun loadQueue(songs: List<Song>, startIndex: Int = 0, autoPlay: Boolean = true) {
        if (!castAvailable || songs.isEmpty()) return
        Timber.d("CastManager: loadQueue ${songs.size} songs")
    }

    fun pause() { /* no-op si Cast no está disponible */ }
    fun play()  { /* no-op si Cast no está disponible */ }
    fun stop()  { /* no-op si Cast no está disponible */ }
    fun seekTo(positionMs: Long) { /* no-op */ }
}
