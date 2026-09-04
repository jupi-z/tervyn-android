package dev.amenokizele.tervyn.data.inmemory

import dev.amenokizele.tervyn.core.result.AppError
import dev.amenokizele.tervyn.core.result.AppResult
import dev.amenokizele.tervyn.core.time.TervynClock
import dev.amenokizele.tervyn.di.DefaultDispatcher
import dev.amenokizele.tervyn.domain.model.SyncOverview
import dev.amenokizele.tervyn.domain.model.SyncState
import dev.amenokizele.tervyn.domain.repository.SyncRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InMemorySyncRepository @Inject constructor(
    private val store: InMemoryStore,
    private val clock: TervynClock,
    @param:DefaultDispatcher private val dispatcher: CoroutineDispatcher
) : SyncRepository {
    override val overview: Flow<SyncOverview> = combine(
        store.jobs,
        store.isOffline,
        store.isSyncing,
        store.lastSuccessfulSyncAt
    ) { _, isOffline, isSyncing, lastSuccessfulSyncAt ->
        val pending = store.pendingCount()
        SyncOverview(
            state = when {
                isSyncing -> SyncState.SYNCING
                pending > 0 -> SyncState.PENDING
                else -> SyncState.SYNCED
            },
            pendingCount = pending,
            failedCount = 0,
            lastSuccessfulSyncAt = lastSuccessfulSyncAt,
            isOnline = !isOffline,
            completedOperations = if (pending == 0) 1 else 0,
            totalOperations = if (pending == 0) 1 else pending
        )
    }

    override suspend fun retryPendingOperations(): AppResult<Unit> = withContext(dispatcher) {
        if (store.isSyncingValue) {
            return@withContext AppResult.Failure(AppError.InvalidState("sync_already_running"))
        }
        if (store.isOfflineValue) {
            return@withContext AppResult.Failure(AppError.InvalidState("offline_simulation"))
        }
        store.isSyncingValue = true
        delay(600)
        store.markAllPendingSynced(clock.now())
        store.isSyncingValue = false
        AppResult.Success(Unit)
    }
}
