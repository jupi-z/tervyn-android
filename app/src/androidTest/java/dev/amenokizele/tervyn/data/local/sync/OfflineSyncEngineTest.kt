package dev.amenokizele.tervyn.data.local.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.amenokizele.tervyn.core.result.AppError
import dev.amenokizele.tervyn.core.result.AppResult
import dev.amenokizele.tervyn.core.time.TervynClock
import dev.amenokizele.tervyn.data.auth.session.StoredSession
import dev.amenokizele.tervyn.data.local.db.TervynDatabase
import dev.amenokizele.tervyn.data.local.entity.JobEntity
import dev.amenokizele.tervyn.data.local.entity.SyncEntityType
import dev.amenokizele.tervyn.data.local.entity.SyncOperationEntity
import dev.amenokizele.tervyn.data.local.entity.SyncOperationStatus
import dev.amenokizele.tervyn.data.local.entity.SyncOperationType
import dev.amenokizele.tervyn.data.remote.auth.SessionCoordinator
import dev.amenokizele.tervyn.data.remote.auth.SessionState
import dev.amenokizele.tervyn.data.remote.config.RemoteApiConfig
import dev.amenokizele.tervyn.data.remote.job.RemoteAttachmentSnapshot
import dev.amenokizele.tervyn.data.remote.job.RemoteChecklistItemSnapshot
import dev.amenokizele.tervyn.data.remote.job.RemoteJobDataSource
import dev.amenokizele.tervyn.data.remote.job.RemoteJobMutationResult
import dev.amenokizele.tervyn.data.remote.job.RemoteJobSnapshot
import dev.amenokizele.tervyn.data.remote.job.RemoteJobsPage
import dev.amenokizele.tervyn.data.remote.job.RemoteNoteSnapshot
import dev.amenokizele.tervyn.domain.model.JobPriority
import dev.amenokizele.tervyn.domain.model.JobStatus
import dev.amenokizele.tervyn.domain.model.SyncState
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OfflineSyncEngineTest {
    private lateinit var database: TervynDatabase
    private lateinit var remote: FakeRemoteJobDataSource
    private lateinit var repository: dev.amenokizele.tervyn.data.local.repository.RoomJobRepository
    private lateinit var orchestrator: SyncOrchestrator

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, TervynDatabase::class.java).build()
        remote = FakeRemoteJobDataSource()
        val clock = FixedClock()
        repository = dev.amenokizele.tervyn.data.local.repository.RoomJobRepository(
            database,
            clock,
            QueueIdGenerator()
        )
        val merger = RemoteJobMerger(database, clock)
        val pull = RemotePullSynchronizer(database, remote, merger, clock)
        orchestrator = SyncOrchestrator(
            database = database,
            config = RemoteApiConfig(enabled = true, baseUrl = "https://example.test/"),
            sessionCoordinator = FakeSessionCoordinator(),
            outboxExecutor = OutboxExecutor(database, remote, clock),
            pullSynchronizer = pull,
            runtimeState = SyncRuntimeStateStore()
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun jobStatusReplayUsesPersistedMutationIdAndAcknowledgesOperation() = runBlocking {
        database.jobDao().insertJob(job(JobStatus.ASSIGNED))

        assertTrue(repository.startJob("job-1") is AppResult.Success)
        val result = orchestrator.run(SyncRunTrigger.MANUAL)

        assertTrue(result is SyncRunResult.Success)
        assertEquals("mutation-1", remote.lastMutationId)
        assertEquals(JobStatus.IN_PROGRESS, remote.lastStatus)
        assertEquals(0, database.syncOperationDao().countOutstanding())
        assertEquals(SyncState.SYNCED, database.jobDao().getJobById("job-1")?.syncState)
    }

    @Test
    fun transientRemoteFailureKeepsOperationPendingWithBackoff() = runBlocking {
        database.jobDao().insertJob(job(JobStatus.ASSIGNED))
        repository.startJob("job-1")
        remote.mutationResult = AppResult.Failure(AppError.Network("network_unavailable"))

        val result = orchestrator.run(SyncRunTrigger.MANUAL)
        val operation = database.syncOperationDao().observeAllOrdered().first().single()

        assertTrue(result is SyncRunResult.TransientFailure)
        assertEquals(SyncOperationStatus.PENDING, operation.status)
        assertEquals(1, operation.attemptCount)
        assertEquals(Instant.parse("2026-09-03T12:00:30Z"), operation.nextAttemptAt)
    }

    @Test
    fun permanentConflictMarksOperationAndEntityFailed() = runBlocking {
        database.jobDao().insertJob(job(JobStatus.ASSIGNED))
        repository.startJob("job-1")
        remote.mutationResult = AppResult.Failure(AppError.Conflict("version_conflict"))

        orchestrator.run(SyncRunTrigger.MANUAL)

        assertEquals(SyncOperationStatus.FAILED, database.syncOperationDao().observeAllOrdered().first().single().status)
        assertEquals(SyncState.FAILED, database.jobDao().getJobById("job-1")?.syncState)
    }

    @Test
    fun attachmentUploadRemainsPersistedAndIsNotExecuted() = runBlocking {
        database.jobDao().insertJob(job(JobStatus.IN_PROGRESS))
        database.userDao().upsert(
            dev.amenokizele.tervyn.data.local.entity.UserEntity(
                id = "user-1", email = "user@example.test", firstName = "A", lastName = "B",
                jobTitle = "Technician", avatarUrl = null, createdAt = NOW, updatedAt = NOW, lastSyncedAt = NOW
            )
        )
        repository.addAttachment(
            "job-1",
            "user-1",
            dev.amenokizele.tervyn.domain.model.AddAttachmentRequest(
                type = dev.amenokizele.tervyn.domain.model.AttachmentType.PHOTO,
                localUri = "content://photo",
                mimeType = "image/jpeg",
                fileName = "photo.jpg",
                sizeBytes = 10,
                checksumSha256 = null
            )
        )

        orchestrator.run(SyncRunTrigger.MANUAL)

        assertEquals(SyncOperationType.UPLOAD, database.syncOperationDao().observeAllOrdered().first().single().operation)
        assertEquals(0, remote.uploadCalls)
    }

    @Test
    fun pullPreservesPendingLocalStatusButUpdatesRemoteFields() = runBlocking {
        database.jobDao().insertJob(job(JobStatus.IN_PROGRESS).copy(title = "Local title", syncState = SyncState.PENDING))
        database.syncOperationDao().insert(operation())
        remote.pages += RemoteJobsPage(
            items = listOf(remoteJob(status = JobStatus.ASSIGNED, title = "Remote title")),
            nextCursor = null,
            serverTime = NOW
        )

        RemotePullSynchronizer(database, remote, RemoteJobMerger(database, FixedClock()), FixedClock()).pull()

        val merged = requireNotNull(database.jobDao().getJobById("job-1"))
        assertEquals("Remote title", merged.title)
        assertEquals(JobStatus.IN_PROGRESS, merged.status)
        assertEquals(1L, merged.serverVersion)
        assertEquals(SyncState.PENDING, merged.syncState)
    }

    @Test
    fun failedLaterPageDoesNotCommitNewWatermark() = runBlocking {
        database.localMetadataDao().upsert(
            dev.amenokizele.tervyn.data.local.entity.LocalMetadataEntity(
                LocalMetadataKeys.REMOTE_WATERMARK, "2026-09-03T11:00:00Z", NOW
            )
        )
        remote.pages += RemoteJobsPage(emptyList(), "cursor-2", NOW)
        remote.pageFailure = AppResult.Failure(AppError.Network("network_unavailable"))

        val result = RemotePullSynchronizer(database, remote, RemoteJobMerger(database, FixedClock()), FixedClock()).pull()

        assertTrue(result.error is SyncRunResult.TransientFailure)
        assertEquals("2026-09-03T11:00:00Z", database.localMetadataDao().get(LocalMetadataKeys.REMOTE_WATERMARK)?.value)
    }

    private fun job(status: JobStatus) = JobEntity(
        id = "job-1", reference = "REF-1", title = "Title", description = null,
        clientName = "Client", siteName = "Site", siteAddress = null, priority = JobPriority.NORMAL,
        status = status, scheduledAt = NOW, startedAt = null, completedAt = null,
        serverVersion = 1, syncState = SyncState.SYNCED, createdAt = NOW, updatedAt = NOW, lastSyncedAt = NOW
    )

    private fun operation() = SyncOperationEntity(
        id = "op-1", entityType = SyncEntityType.JOB, entityId = "job-1",
        operation = SyncOperationType.UPDATE, clientMutationId = "mutation-1",
        status = SyncOperationStatus.PENDING, attemptCount = 0, lastErrorCode = null,
        lastErrorMessage = null, createdAt = NOW, lastAttemptAt = null, nextAttemptAt = null,
        payloadJson = SyncOperationPayloadCodec().encode(JobStatusPayload("job-1", "IN_PROGRESS", NOW.toString()))
    )

    private fun remoteJob(status: JobStatus, title: String) = RemoteJobSnapshot(
        id = "job-1", reference = "REF-1", title = title, description = null, clientName = "Client",
        siteName = "Site", siteAddress = null, priority = JobPriority.NORMAL, status = status,
        scheduledAt = NOW, startedAt = null, completedAt = null, serverVersion = 2,
        createdAt = NOW, updatedAt = NOW, checklist = emptyList(), notes = emptyList(), attachments = emptyList()
    )

    private class FixedClock : TervynClock {
        override fun now(): Instant = NOW
    }

    private class QueueIdGenerator : dev.amenokizele.tervyn.data.local.repository.LocalIdGenerator {
        private val ids = ArrayDeque(listOf("operation-1", "mutation-1", "attachment-1"))
        override fun newId(): String = ids.removeFirst()
    }

    private class FakeSessionCoordinator : SessionCoordinator {
        private val session = StoredSession("user-1", "access", "refresh", NOW, NOW.plusSeconds(3600), NOW.plusSeconds(7200))
        private val mutableState = MutableStateFlow<SessionState>(SessionState.Active(session))
        override val state: StateFlow<SessionState> = mutableState
        override suspend fun restore() = AppResult.Success(session)
        override suspend fun replace(session: StoredSession) = AppResult.Success(Unit)
        override suspend fun clear() = AppResult.Success(Unit)
        override fun snapshot(): StoredSession = session
    }

    private class FakeRemoteJobDataSource : RemoteJobDataSource {
        val pages = ArrayDeque<RemoteJobsPage>()
        var pageFailure: AppResult<RemoteJobsPage>? = null
        var mutationResult: AppResult<RemoteJobMutationResult> = AppResult.Success(
            RemoteJobMutationResult("job-1", 2, NOW, JobStatus.IN_PROGRESS, NOW, null)
        )
        var lastMutationId: String? = null
        var lastStatus: JobStatus? = null
        var uploadCalls = 0

        override suspend fun fetchJobs(cursor: String?, limit: Int, updatedAfter: Instant?): AppResult<RemoteJobsPage> {
            pageFailure?.let { failure -> pageFailure = null; return failure }
            return AppResult.Success(if (pages.isEmpty()) RemoteJobsPage(emptyList(), null, NOW) else pages.removeFirst())
        }
        override suspend fun fetchJob(jobId: String): AppResult<RemoteJobSnapshot> = AppResult.Failure(AppError.NotFound("job", jobId))
        override suspend fun updateJobStatus(jobId: String, clientMutationId: String, serverVersion: Long, status: JobStatus, changedAt: Instant): AppResult<RemoteJobMutationResult> {
            lastMutationId = clientMutationId
            lastStatus = status
            return mutationResult
        }
        override suspend fun updateChecklistItem(jobId: String, itemId: String, clientMutationId: String, serverVersion: Long, completed: Boolean, completedAt: Instant?): AppResult<RemoteChecklistItemSnapshot> = AppResult.Failure(AppError.NotFound("checklist", itemId))
        override suspend fun createNote(jobId: String, noteId: String, clientMutationId: String, content: String, createdAt: Instant): AppResult<RemoteNoteSnapshot> = AppResult.Failure(AppError.NotFound("note", noteId))
        override suspend fun deleteAttachmentMetadata(jobId: String, attachmentId: String, clientMutationId: String, serverVersion: Long) = AppResult.Success(Unit)
    }

    private companion object {
        val NOW: Instant = Instant.parse("2026-09-03T12:00:00Z")
    }
}
