package com.lg.monkeymusicplayer.domain.usecase

import com.lg.monkeymusicplayer.data.database.FavoriteEntity
import com.lg.monkeymusicplayer.data.database.MusicDao
import com.lg.monkeymusicplayer.data.model.Song
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Maneja el toggle de favorito para una canción.
 * Encapsula la lógica insert/delete que antes residía directamente en el ViewModel.
 */
class ToggleFavoriteUseCase @Inject constructor(
    private val dao: MusicDao
) {
    suspend operator fun invoke(song: Song) {
        toggle(song.id, song.isFavorite)
    }

    suspend fun toggle(songId: Long, isFavorite: Boolean) {
        if (isFavorite) {
            dao.deleteFavorite(FavoriteEntity(songId))
        } else {
            dao.insertFavorite(FavoriteEntity(songId))
        }
    }

    fun getFavoriteIds(): Flow<List<Long>> = dao.getFavorites()
}
