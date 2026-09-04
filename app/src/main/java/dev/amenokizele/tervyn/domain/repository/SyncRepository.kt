package dev.amenokizele.tervyn.domain.repository

import dev.amenokizele.tervyn.core.result.AppResult
import dev.amenokizele.tervyn.domain.model.SyncOverview
import kotlinx.coroutines.flow.Flow

interface SyncRepository {
    val overview: Flow<SyncOverview>

    suspend fun retryPendingOperations(): AppResult<Unit>
}
