package dev.amenokizele.tervyn.fake

import dev.amenokizele.tervyn.core.result.AppResult
import dev.amenokizele.tervyn.domain.model.SyncOverview
import dev.amenokizele.tervyn.domain.model.SyncState
import dev.amenokizele.tervyn.domain.repository.SyncRepository
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSyncRepository(
    initialOverview: SyncOverview = SyncOverview(
        state = SyncState.PENDING,
        pendingCount = 1,
        failedCount = 0,
        lastSuccessfulSyncAt = Instant.parse("2026-09-03T06:00:00Z"),
        isOnline = true,
        completedOperations = 0,
        totalOperations = 1
    )
) : SyncRepository {
    private val mutableOverview = MutableStateFlow(initialOverview)

    override val overview: Flow<SyncOverview> = mutableOverview

    override suspend fun retryPendingOperations(): AppResult<Unit> = AppResult.Success(Unit)
}
