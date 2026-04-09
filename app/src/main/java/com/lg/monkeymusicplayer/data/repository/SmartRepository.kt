package com.lg.monkeymusicplayer.data.repository

import com.lg.monkeymusicplayer.core.smart.SmartEngine
import com.lg.monkeymusicplayer.data.database.MusicDao
import com.lg.monkeymusicplayer.data.database.SongStatDao
import com.lg.monkeymusicplayer.data.model.SmartPlaylist
import com.lg.monkeymusicplayer.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SmartRepository — orquesta [SmartEngine] con datos de Room.
 *
 * Expone [smartPlaylists] como Flow reactivo: se recalcula automáticamente
 * cuando cambian las stats (después de cada reproducción) o el catálogo.
 * El recálculo es ligero: pura lógica Kotlin sobre listas ya cargadas.
 */
@Singleton
class SmartRepository @Inject constructor(
    private val musicDao: MusicDao,
    private val statDao: SongStatDao,
    private val engine: SmartEngine
) {
    /**
     * Flow de Smart Playlists. Se emite un nuevo valor cada vez que
     * [SongStatDao.getAllStats] o el catálogo de canciones cambia.
     *
     * La combinación se ejecuta en [Dispatchers.IO] para no bloquear Main.
     */
    val smartPlaylists: Flow<List<SmartPlaylist>> =
        combine(
            musicDao.getAllSongsFlow(),
            statDao.getAllStats()
        ) { songEntities, stats ->
            val songMap: Map<Long, Song> = songEntities.associate { entity ->
                entity.id to Song(
                    id = entity.id,
                    albumId = entity.albumId,
                    title = entity.title,
                    artist = entity.artist,
                    album = entity.album,
                    genre = entity.genre,
                    folder = entity.folder,
                    path = entity.path,
                    albumArtUri = entity.albumArtUri
                )
            }
            engine.generate(stats, songMap)
        }
        .flowOn(Dispatchers.IO)
}
