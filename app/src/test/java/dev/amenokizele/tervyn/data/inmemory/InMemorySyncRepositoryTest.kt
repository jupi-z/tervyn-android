package dev.amenokizele.tervyn.data.inmemory

import dev.amenokizele.tervyn.FakeTervynClock
import dev.amenokizele.tervyn.core.result.AppResult
import dev.amenokizele.tervyn.domain.model.SyncState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InMemorySyncRepositoryTest {
    @Test
    fun retryPendingOperations_usesSharedStoreAndPropagatesSyncedState() = runTest {
        val store = InMemoryStore()
        val repository = InMemorySyncRepository(
            store = store,
            clock = FakeTervynClock(),
            dispatcher = StandardTestDispatcher(testScheduler)
        )

        assertTrue(store.pendingCount() > 0)

        val result = repository.retryPendingOperations()
        advanceTimeBy(600)
        runCurrent()

        assertTrue(result is AppResult.Success)
        assertEquals(0, store.pendingCount())
        assertEquals(SyncState.SYNCED, repository.overview.first().state)
        assertTrue(store.jobsValue.all { job ->
            job.syncState == SyncState.SYNCED &&
                job.checklist.all { it.syncState == SyncState.SYNCED } &&
                job.notes.all { it.syncState == SyncState.SYNCED } &&
                job.attachments.all { it.syncState == SyncState.SYNCED }
        })
    }
}
