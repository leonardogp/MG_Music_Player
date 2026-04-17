package com.lg.monkeymusicplayer.domain.usecase

import com.lg.monkeymusicplayer.data.model.UserStats
import com.lg.monkeymusicplayer.data.repository.StatsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Expone [UserStats] como Flow reactivo.
 * Se recalcula automáticamente tras cada evento de reproducción registrado por [StatTracker].
 */
class GetUserStatsUseCase @Inject constructor(
    private val repository: StatsRepository
) {
    operator fun invoke(): Flow<UserStats> = repository.userStats
}
