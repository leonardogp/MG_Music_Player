package com.lg.monkeymusicplayer.domain.usecase

import com.lg.monkeymusicplayer.core.result.Result
import com.lg.monkeymusicplayer.data.repository.MusicRepository
import javax.inject.Inject

/**
 * Lanza un escaneo de MediaStore y actualiza Room.
 * Devuelve [Result] para que el ViewModel pueda mapear el estado a [UiState].
 *
 * @param onProgress callback opcional (procesadas, total) para feedback de progreso.
 */
class RefreshMusicLibraryUseCase @Inject constructor(
    private val repository: MusicRepository
) {
    suspend operator fun invoke(
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): Result<Unit> = runCatching {
        repository.refreshMusicDatabase(onProgress)
    }.fold(
        onSuccess = { Result.Success(Unit) },
        onFailure = { Result.Error(it.message ?: "Unknown error during library refresh") }
    )
}
