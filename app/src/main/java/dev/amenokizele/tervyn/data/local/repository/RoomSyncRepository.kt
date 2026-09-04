package dev.amenokizele.tervyn.data.local.repository

import dev.amenokizele.tervyn.app.SimulationController
import dev.amenokizele.tervyn.core.result.AppError
import dev.amenokizele.tervyn.core.result.AppResult
import dev.amenokizele.tervyn.data.local.dao.SyncOperationDao
import dev.amenokizele.tervyn.domain.model.SyncOverview
import dev.amenokizele.tervyn.domain.model.SyncState
import dev.amenokizele.tervyn.domain.repository.SyncRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomSyncRepository @Inject constructor(
    private val syncOperationDao: SyncOperationDao,
    private val simulationController: SimulationController
) : SyncRepository {
    override val overview: Flow<SyncOverview> = combine(
        syncOperationDao.observePendingCount(),
        syncOperationDao.observeFailedCount(),
        simulationController.isOffline
    ) { pendingCount, failedCount, isOffline ->
        val total = pendingCount + failedCount
        SyncOverview(
            state = when {
                failedCount > 0 -> SyncState.FAILED
                pendingCount > 0 -> SyncState.PENDING
                else -> SyncState.SYNCED
            },
            pendingCount = pendingCount,
            failedCount = failedCount,
            lastSuccessfulSyncAt = null,
            isOnline = !isOffline,
            completedOperations = 0,
            totalOperations = if (total == 0) 1 else total
        )
    }

    override suspend fun retryPendingOperations(): AppResult<Unit> {
        if (simulationController.isOffline.value) {
            return AppResult.Failure(AppError.InvalidState("offline_simulation"))
        }
        return AppResult.Failure(AppError.Network("remote_sync_not_configured"))
    }
}
