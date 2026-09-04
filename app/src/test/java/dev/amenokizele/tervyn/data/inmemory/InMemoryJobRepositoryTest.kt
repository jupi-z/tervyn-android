package dev.amenokizele.tervyn.data.inmemory

import dev.amenokizele.tervyn.FakeTervynClock
import dev.amenokizele.tervyn.core.result.AppResult
import dev.amenokizele.tervyn.domain.model.AddAttachmentRequest
import dev.amenokizele.tervyn.domain.model.AttachmentType
import dev.amenokizele.tervyn.domain.model.JobStatus
import dev.amenokizele.tervyn.domain.repository.JobRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InMemoryJobRepositoryTest {
    private lateinit var repository: JobRepository

    @Before
    fun setUp() {
        repository = InMemoryJobRepository(InMemoryStore(), FakeTervynClock())
    }

    @Test
    fun observeJobs_emitsInitialData() = runTest {
        assertEquals(10, repository.observeJobs().first().size)
    }

    @Test
    fun startJob_onlyAllowsAssignedToInProgress() = runTest {
        assertTrue(repository.startJob("job-001") is AppResult.Success)
        assertEquals(JobStatus.IN_PROGRESS, repository.observeJob("job-001").first()?.status)

        assertTrue(repository.startJob("job-001") is AppResult.Failure)
        assertTrue(repository.startJob("job-007") is AppResult.Failure)
        assertEquals(JobStatus.COMPLETED, repository.observeJob("job-007").first()?.status)
    }

    @Test
    fun completeJob_requiresInProgressJobWithRequiredChecklistComplete() = runTest {
        assertTrue(repository.completeJob("job-001") is AppResult.Failure)
        assertTrue(repository.completeJob("job-003") is AppResult.Failure)

        assertTrue(repository.toggleChecklistItem("job-003", "c3-3") is AppResult.Success)
        assertTrue(repository.completeJob("job-003") is AppResult.Success)
        assertEquals(JobStatus.COMPLETED, repository.observeJob("job-003").first()?.status)

        assertTrue(repository.completeJob("job-003") is AppResult.Failure)
    }

    @Test
    fun completedJob_isReadOnlyForBusinessChanges() = runTest {
        assertTrue(repository.addNote("job-007", AUTHOR_USER_ID, "Nouvelle note") is AppResult.Failure)
        assertTrue(repository.addAttachment("job-007", AUTHOR_USER_ID, photoRequest()) is AppResult.Failure)
        assertTrue(repository.deleteAttachment("job-007", "p7-1") is AppResult.Failure)
        assertTrue(repository.toggleChecklistItem("job-007", "c7-1") is AppResult.Failure)

        val completedJob = repository.observeJob("job-007").first()
        assertEquals(JobStatus.COMPLETED, completedJob?.status)
        assertEquals(1, completedJob?.notes?.size)
        assertEquals(1, completedJob?.attachments?.size)
    }

    @Test
    fun mutations_areObservable() = runTest {
        val observedStatuses = mutableListOf<JobStatus?>()
        val collectJob = backgroundScope.launch {
            repository.observeJob("job-003").collect { observedStatuses.add(it?.status) }
        }
        runCurrent()

        assertTrue(repository.addNote("job-003", AUTHOR_USER_ID, "Note observable") is AppResult.Success)
        assertTrue(repository.addAttachment("job-003", AUTHOR_USER_ID, photoRequest()) is AppResult.Success)
        assertTrue(repository.toggleChecklistItem("job-003", "c3-3") is AppResult.Success)
        assertTrue(repository.completeJob("job-003") is AppResult.Success)
        runCurrent()

        val latest = repository.observeJob("job-003").first()
        assertEquals(JobStatus.COMPLETED, latest?.status)
        assertTrue(latest?.notes?.any { it.content == "Note observable" } == true)
        assertTrue(latest?.attachments?.any { it.fileName == "Photo observable" } == true)
        assertTrue(observedStatuses.contains(JobStatus.COMPLETED))
        collectJob.cancel()
    }

    @Test
    fun invalidIds_failSafely() = runTest {
        assertTrue(repository.startJob("missing") is AppResult.Failure)
        assertTrue(repository.toggleChecklistItem("job-003", "missing") is AppResult.Failure)
        assertTrue(repository.deleteAttachment("job-003", "missing") is AppResult.Failure)
        assertFalse(repository.observeJobs().first().isEmpty())
    }

    private fun photoRequest() = AddAttachmentRequest(
        type = AttachmentType.PHOTO,
        localUri = "tervyn://test/photo/CABLING",
        mimeType = "image/jpeg",
        fileName = "Photo observable",
        sizeBytes = 0,
        checksumSha256 = null
    )

    private companion object {
        const val AUTHOR_USER_ID = "user-test"
    }
}
