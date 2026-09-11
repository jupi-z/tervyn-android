package dev.amenokizele.tervyn.data.local.repository

import dev.amenokizele.tervyn.app.SimulationController
import dev.amenokizele.tervyn.core.result.AppError
import dev.amenokizele.tervyn.core.result.AppResult
import dev.amenokizele.tervyn.data.local.dao.LocalMetadataDao
import dev.amenokizele.tervyn.data.local.sync.LocalMetadataKeys
import dev.amenokizele.tervyn.data.local.sync.SyncOrchestrator
import dev.amenokizele.tervyn.data.local.sync.SyncRunResult
import dev.amenokizele.tervyn.data.local.sync.SyncRunTrigger
import dev.amenokizele.tervyn.data.local.sync.SyncRuntimePhase
import dev.amenokizele.tervyn.data.local.sync.SyncRuntimeStateStore
import dev.amenokizele.tervyn.data.local.dao.SyncOperationDao
import dev.amenokizele.tervyn.domain.model.SyncOverview
import dev.amenokizele.tervyn.domain.model.SyncState
import dev.amenokizele.tervyn.domain.repository.SyncRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomSyncRepository @Inject constructor(
    private val syncOperationDao: SyncOperationDao,
    private val localMetadataDao: LocalMetadataDao?,
    private val simulationController: SimulationController,
    private val orchestrator: SyncOrchestrator?,
    private val runtimeStateStore: SyncRuntimeStateStore
) : SyncRepository {
    constructor(
        syncOperationDao: SyncOperationDao,
        simulationController: SimulationController
    ) : this(
        syncOperationDao = syncOperationDao,
        localMetadataDao = null,
        simulationController = simulationController,
        orchestrator = null,
        runtimeStateStore = SyncRuntimeStateStore()
    )

    override val overview: Flow<SyncOverview> = combine(
        syncOperationDao.observePendingCount(),
        syncOperationDao.observeFailedCount(),
        simulationController.isOffline,
        runtimeStateStore.state,
        localMetadataDao?.observe(LocalMetadataKeys.LAST_SUCCESSFUL_SYNC_AT) ?: flowOf(null)
    ) { pendingCount, failedCount, isOffline, runtime, lastSync ->
        val lastSuccessfulSyncAt = lastSync?.value?.let { runCatching { java.time.Instant.parse(it) }.getOrNull() }
        val total = pendingCount + failedCount
        SyncOverview(
            state = when {
                runtime.phase == SyncRuntimePhase.SYNCING -> SyncState.SYNCING
                runtime.phase == SyncRuntimePhase.FAILED -> SyncState.FAILED
                failedCount > 0 -> SyncState.FAILED
                pendingCount > 0 -> SyncState.PENDING
                else -> SyncState.SYNCED
            },
            pendingCount = pendingCount,
            failedCount = failedCount,
            lastSuccessfulSyncAt = lastSuccessfulSyncAt,
            isOnline = !isOffline,
            completedOperations = runtime.completedOperations,
            totalOperations = if (total == 0) 1 else total
        )
    }

    override suspend fun retryPendingOperations(): AppResult<Unit> {
        if (simulationController.isOffline.value) {
            return AppResult.Failure(AppError.InvalidState("offline_simulation"))
        }
        val activeOrchestrator = orchestrator
            ?: return AppResult.Failure(AppError.Network("remote_sync_not_configured"))
        return when (val result = activeOrchestrator.run(SyncRunTrigger.MANUAL)) {
            is SyncRunResult.Success,
            is SyncRunResult.PartialSuccess,
            SyncRunResult.RemoteDisabled -> AppResult.Success(Unit)
            SyncRunResult.AuthenticationRequired -> AppResult.Failure(AppError.Authentication("session_required"))
            SyncRunResult.AlreadyRunning -> AppResult.Failure(AppError.InvalidState("sync_already_running"))
            is SyncRunResult.TransientFailure -> AppResult.Failure(result.error)
            is SyncRunResult.StorageFailure -> AppResult.Failure(result.error)
        }
    }
}
