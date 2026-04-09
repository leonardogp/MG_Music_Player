package com.lg.monkeymusicplayer.data.model

/**
 * Playlist generada por el Smart Engine.
 *
 * @param type   Tipo semántico — determina icono y descripción en la UI.
 * @param songs  Canciones ordenadas por score descendente.
 */
data class SmartPlaylist(
    val type: SmartPlaylistType,
    val songs: List<Song>
) {
    val isEmpty get() = songs.isEmpty()
}

enum class SmartPlaylistType {
    /** Top score global — mezcla de artistas, reproducción reciente y alta completionRate. */
    DAILY_MIX,

    /** Score histórico alto, sin reproducción reciente — canciones olvidadas con potencial. */
    REDISCOVER,

    /** Las más escuchadas de todos los tiempos por playCount + completeCount. */
    TOP_SONGS
}
