package com.lg.monkeymusicplayer.core.queue

import com.lg.monkeymusicplayer.data.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * QueueManager — gestiona múltiples colas de reproducción nombradas.
 *
 * ## Modelo de datos
 * - Una aplicación tiene N [PlayQueue]s identificadas por un [String] (nombre).
 * - Solo una cola puede estar **activa** en un momento dado; es la que alimenta
 *   al [MusicPlayerManager].
 * - Las colas son volátiles (in-memory); no se persisten entre sesiones porque
 *   el historial de Room ya cubre ese caso de uso.
 *
 * ## Colas predefinidas
 * - [QUEUE_MAIN]: cola principal, creada automáticamente al iniciar la app.
 * - [QUEUE_SMART]: usada por Smart Playlists para no contaminar la cola del usuario.
 *
 * ## Uso típico en ViewModel
 * ```kotlin
 * // Crear una cola secundaria y hacer que la UI la gestione
 * queueManager.createQueue("Party Mix")
 * queueManager.addToQueue("Party Mix", song)
 * queueManager.switchToQueue("Party Mix") // el player carga esta cola
 *
 * // Volver a la cola principal
 * queueManager.switchToQueue(QueueManager.QUEUE_MAIN)
 * ```
 */
@Singleton
class QueueManager @Inject constructor() {

    companion object {
        const val QUEUE_MAIN  = "main"
        const val QUEUE_SMART = "smart"
    }

    // ── Estado interno ───────────────────────────────────────────────────────

    private val _queues = MutableStateFlow<Map<String, PlayQueue>>(
        mapOf(
            QUEUE_MAIN  to PlayQueue(name = QUEUE_MAIN,  displayName = "Main Queue"),
            QUEUE_SMART to PlayQueue(name = QUEUE_SMART, displayName = "Smart Queue")
        )
    )
    val queues: StateFlow<Map<String, PlayQueue>> = _queues.asStateFlow()

    private val _activeQueueName = MutableStateFlow(QUEUE_MAIN)
    val activeQueueName: StateFlow<String> = _activeQueueName.asStateFlow()

    // ── Lectura ──────────────────────────────────────────────────────────────

    /** Cola actualmente activa. Nunca null — siempre hay al menos una cola. */
    val activeQueue: PlayQueue
        get() = _queues.value[_activeQueueName.value] ?: _queues.value[QUEUE_MAIN]!!

    /** Canciones de la cola activa. Shortcut para el ViewModel. */
    val activeSongs: List<Song>
        get() = activeQueue.songs

    fun getQueue(name: String): PlayQueue? = _queues.value[name]

    fun getAllQueues(): List<PlayQueue> = _queues.value.values.toList()

    // ── Gestión de colas ─────────────────────────────────────────────────────

    /**
     * Crea una cola nueva con el nombre dado.
     * Si ya existe una cola con ese nombre, no hace nada.
     * @return true si la cola fue creada, false si ya existía.
     */
    fun createQueue(name: String, displayName: String = name): Boolean {
        if (_queues.value.containsKey(name)) return false
        _queues.update { it + (name to PlayQueue(name = name, displayName = displayName)) }
        return true
    }

    /**
     * Elimina una cola. Las colas predefinidas ([QUEUE_MAIN], [QUEUE_SMART])
     * no pueden eliminarse. Si la cola activa es eliminada, se vuelve a [QUEUE_MAIN].
     * @return true si fue eliminada.
     */
    fun deleteQueue(name: String): Boolean {
        if (name == QUEUE_MAIN || name == QUEUE_SMART) return false
        _queues.update { it - name }
        if (_activeQueueName.value == name) {
            _activeQueueName.value = QUEUE_MAIN
        }
        return true
    }

    /**
     * Cambia la cola activa. El llamador (ViewModel) es responsable de
     * notificar al [MusicPlayerManager] con las canciones de la nueva cola.
     */
    fun switchToQueue(name: String) {
        if (_queues.value.containsKey(name)) {
            _activeQueueName.value = name
        }
    }

    // ── Manipulación de canciones ────────────────────────────────────────────

    /** Reemplaza completamente el contenido de una cola. */
    fun setQueueSongs(name: String, songs: List<Song>) {
        _queues.update { queues ->
            val queue = queues[name] ?: return@update queues
            queues + (name to queue.copy(songs = songs))
        }
    }

    /** Añade una canción al final de una cola. Crea la cola si no existe. */
    fun addToQueue(name: String, song: Song) {
        _queues.update { queues ->
            val queue = queues[name] ?: PlayQueue(name = name, displayName = name)
            queues + (name to queue.copy(songs = queue.songs + song))
        }
    }

    /** Añade una canción a la cola activa. */
    fun addToActiveQueue(song: Song) = addToQueue(_activeQueueName.value, song)

    /** Elimina una canción de una cola por su id. */
    fun removeFromQueue(name: String, songId: Long) {
        _queues.update { queues ->
            val queue = queues[name] ?: return@update queues
            queues + (name to queue.copy(songs = queue.songs.filter { it.id != songId }))
        }
    }

    /** Reordena una canción dentro de una cola (drag & drop). */
    fun moveInQueue(name: String, fromIndex: Int, toIndex: Int) {
        _queues.update { queues ->
            val queue = queues[name] ?: return@update queues
            val songs = queue.songs.toMutableList()
            if (fromIndex !in songs.indices || toIndex !in songs.indices) return@update queues
            val song = songs.removeAt(fromIndex)
            songs.add(toIndex, song)
            queues + (name to queue.copy(songs = songs))
        }
    }

    /** Vacía el contenido de una cola sin eliminarla. */
    fun clearQueue(name: String) {
        _queues.update { queues ->
            val queue = queues[name] ?: return@update queues
            queues + (name to queue.copy(songs = emptyList()))
        }
    }
}
