package dev.amenokizele.tervyn.demo

import dev.amenokizele.tervyn.model.DemoJob
import dev.amenokizele.tervyn.model.JobStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DemoRepositoryTest {

    @Before
    fun resetRepository() {
        DemoRepository.resetDemoData()
    }

    @Test
    fun startJob_onlyAllowsAssignedToInProgress() {
        assertTrue(DemoRepository.startJob("job-001"))
        assertEquals(JobStatus.IN_PROGRESS, DemoRepository.getJobById("job-001")?.status)

        assertFalse(DemoRepository.startJob("job-001"))
        assertFalse(DemoRepository.startJob("job-007"))
        assertEquals(JobStatus.COMPLETED, DemoRepository.getJobById("job-007")?.status)
    }

    @Test
    fun completeJob_requiresInProgressJobWithRequiredChecklistComplete() {
        assertFalse(DemoRepository.completeJob("job-001"))
        assertFalse(DemoRepository.completeJob("job-003"))

        assertTrue(DemoRepository.toggleChecklistItem("job-003", "c3-3"))
        assertTrue(DemoRepository.completeJob("job-003"))
        assertEquals(JobStatus.COMPLETED, DemoRepository.getJobById("job-003")?.status)

        assertFalse(DemoRepository.completeJob("job-003"))
    }

    @Test
    fun completedJob_isReadOnlyForBusinessChanges() {
        assertNull(DemoRepository.addNote("job-007", "Nouvelle note"))
        assertNull(DemoRepository.addPhoto("job-007", "Photo refusée"))
        assertFalse(DemoRepository.deletePhoto("job-007", "p7-1"))
        assertFalse(DemoRepository.toggleChecklistItem("job-007", "c7-1"))
        assertFalse(DemoRepository.completeJob("job-007"))

        val completedJob = DemoRepository.getJobById("job-007")
        assertEquals(JobStatus.COMPLETED, completedJob?.status)
        assertEquals(1, completedJob?.notes?.size)
        assertEquals(1, completedJob?.photos?.size)
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeJob_emitsRepositoryUpdates() = runTest {
        val observed = mutableListOf<DemoJob?>()
        val collectJob = backgroundScope.launch {
            DemoRepository.observeJob("job-001").collect { observed.add(it) }
        }
        runCurrent()

        assertNotNull(observed.last())
        assertEquals(JobStatus.ASSIGNED, observed.last()?.status)

        assertTrue(DemoRepository.startJob("job-001"))
        runCurrent()

        assertEquals(JobStatus.IN_PROGRESS, observed.last()?.status)
        collectJob.cancel()
    }
}
