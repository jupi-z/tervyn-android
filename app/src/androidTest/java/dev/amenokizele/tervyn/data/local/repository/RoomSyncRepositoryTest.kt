package dev.amenokizele.tervyn.data.local.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.amenokizele.tervyn.app.SimulationController
import dev.amenokizele.tervyn.core.result.AppError
import dev.amenokizele.tervyn.core.result.AppResult
import dev.amenokizele.tervyn.data.local.db.TervynDatabase
import dev.amenokizele.tervyn.data.local.entity.SyncEntityType
import dev.amenokizele.tervyn.data.local.entity.SyncOperationEntity
import dev.amenokizele.tervyn.data.local.entity.SyncOperationStatus
import dev.amenokizele.tervyn.data.local.entity.SyncOperationType
import dev.amenokizele.tervyn.domain.model.SyncState
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RoomSyncRepositoryTest {
    private lateinit var database: TervynDatabase
    private lateinit var simulationController: FakeSimulationController
    private lateinit var repository: RoomSyncRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, TervynDatabase::class.java).build()
        simulationController = FakeSimulationController()
        repository = RoomSyncRepository(database.syncOperationDao(), simulationController)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun overviewReflectsPersistentOutboxCounts() = runBlocking {
        database.syncOperationDao().insert(operation("op-pending", "mutation-pending", SyncOperationStatus.PENDING))
        database.syncOperationDao().insert(operation("op-failed", "mutation-failed", SyncOperationStatus.FAILED))

        val overview = repository.overview.first()

        assertEquals(SyncState.FAILED, overview.state)
        assertEquals(1, overview.pendingCount)
        assertEquals(1, overview.failedCount)
        assertTrue(overview.isOnline)
    }

    @Test
    fun retryPendingOperationsDoesNotClearOutboxOrPretendRemoteSyncSucceeded() = runBlocking {
        database.syncOperationDao().insert(operation("op-pending", "mutation-pending", SyncOperationStatus.PENDING))

        val result = repository.retryPendingOperations()

        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).error is AppError.Network)
        assertEquals(1, database.syncOperationDao().observePendingCount().first())
    }

    @Test
    fun retryPendingOperationsRespectsOfflineSimulation() = runBlocking {
        database.syncOperationDao().insert(operation("op-pending", "mutation-pending", SyncOperationStatus.PENDING))
        simulationController.toggleOffline()

        val result = repository.retryPendingOperations()

        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).error is AppError.InvalidState)
        assertEquals(1, database.syncOperationDao().observePendingCount().first())
    }

    private fun operation(
        id: String,
        clientMutationId: String,
        status: SyncOperationStatus
    ) = SyncOperationEntity(
        id = id,
        entityType = SyncEntityType.JOB,
        entityId = "job-1",
        operation = SyncOperationType.UPDATE,
        clientMutationId = clientMutationId,
        status = status,
        attemptCount = 0,
        lastErrorCode = null,
        lastErrorMessage = null,
        createdAt = Instant.parse("2026-09-03T12:00:00Z"),
        lastAttemptAt = null,
        nextAttemptAt = null
    )

    private class FakeSimulationController : SimulationController {
        private val offline = MutableStateFlow(false)
        override val isOffline: StateFlow<Boolean> = offline

        override fun toggleOffline() {
            offline.value = !offline.value
        }
    }
}
