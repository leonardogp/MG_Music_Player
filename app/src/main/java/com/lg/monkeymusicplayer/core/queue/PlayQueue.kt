package com.lg.monkeymusicplayer.core.queue

import com.lg.monkeymusicplayer.data.model.Song

/**
 * Modelo inmutable de una cola de reproducción.
 *
 * @param name        Identificador único (usado como clave en [QueueManager]).
 * @param displayName Nombre legible para mostrar en la UI.
 * @param songs       Lista ordenada de canciones en la cola.
 */
data class PlayQueue(
    val name: String,
    val displayName: String,
    val songs: List<Song> = emptyList()
) {
    val isEmpty: Boolean get() = songs.isEmpty()
    val size: Int get() = songs.size
}
