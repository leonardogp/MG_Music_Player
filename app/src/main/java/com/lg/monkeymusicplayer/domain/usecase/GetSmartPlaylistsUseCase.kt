package com.lg.monkeymusicplayer.domain.usecase

import com.lg.monkeymusicplayer.data.model.SmartPlaylist
import com.lg.monkeymusicplayer.data.repository.SmartRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Expone las Smart Playlists calculadas por [SmartEngine] como Flow reactivo.
 * Se recalcula automáticamente ante cambios en stats o en el catálogo.
 */
class GetSmartPlaylistsUseCase @Inject constructor(
    private val repository: SmartRepository
) {
    operator fun invoke(): Flow<List<SmartPlaylist>> = repository.smartPlaylists
}
