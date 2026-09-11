package dev.amenokizele.tervyn.data.local.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.amenokizele.tervyn.core.result.AppError
import dev.amenokizele.tervyn.core.result.AppResult
import dev.amenokizele.tervyn.core.time.TervynClock
import dev.amenokizele.tervyn.data.local.db.TervynDatabase
import dev.amenokizele.tervyn.data.local.entity.AttachmentEntity
import dev.amenokizele.tervyn.data.local.entity.ChecklistItemEntity
import dev.amenokizele.tervyn.data.local.entity.JobEntity
import dev.amenokizele.tervyn.data.local.entity.SyncEntityType
import dev.amenokizele.tervyn.data.local.entity.SyncOperationEntity
import dev.amenokizele.tervyn.data.local.entity.SyncOperationStatus
import dev.amenokizele.tervyn.data.local.entity.SyncOperationType
import dev.amenokizele.tervyn.data.local.entity.UserEntity
import dev.amenokizele.tervyn.data.local.sync.AttachmentDeletePayload
import dev.amenokizele.tervyn.data.local.sync.AttachmentUploadPayload
import dev.amenokizele.tervyn.data.local.sync.ChecklistItemPayload
import dev.amenokizele.tervyn.data.local.sync.JobStatusPayload
import dev.amenokizele.tervyn.data.local.sync.NoteCreatePayload
import dev.amenokizele.tervyn.data.local.sync.SyncOperationPayloadCodec
import dev.amenokizele.tervyn.domain.model.AddAttachmentRequest
import dev.amenokizele.tervyn.domain.model.AttachmentType
import dev.amenokizele.tervyn.domain.model.JobPriority
import dev.amenokizele.tervyn.domain.model.JobStatus
import dev.amenokizele.tervyn.domain.model.SyncState
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RoomJobRepositoryTest {
    private lateinit var database: TervynDatabase
    private lateinit var ids: QueueLocalIdGenerator
    private lateinit var repository: RoomJobRepository

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, TervynDatabase::class.java).build()
        ids = QueueLocalIdGenerator()
        repository = RoomJobRepository(database, FixedClock(), ids)
        database.userDao().upsert(user())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun startJob_changesAssignedJobAndWritesJobUpdateOutbox() = runBlocking {
        database.jobDao().insertJob(job(status = JobStatus.ASSIGNED))
        ids.enqueue("op-start", "mutation-start")

        val result = repository.startJob("job-1")

        assertTrue(result is AppResult.Success)
        assertEquals(JobStatus.IN_PROGRESS, repository.observeJob("job-1").first()?.status)
        val operations = database.syncOperationDao().observeAllOrdered().first()
        assertEquals(1, operations.size)
        assertEquals(SyncEntityType.JOB, operations.single().entityType)
        assertEquals(SyncOperationType.UPDATE, operations.single().operation)
        assertEquals("mutation-start", operations.single().clientMutationId)
        assertEquals(
            JobStatusPayload("job-1", "IN_PROGRESS", now.toString()),
            (SyncOperationPayloadCodec().decode(
                operations.single().payloadJson,
                SyncEntityType.JOB,
                SyncOperationType.UPDATE
            ) as AppResult.Success).data
        )
    }

    @Test
    fun fieldMutationsPersistDataAndOutboxOperations() = runBlocking {
        database.jobDao().insertJob(job(status = JobStatus.IN_PROGRESS))
        database.checklistItemDao().upsert(checklist(completed = false))
        database.attachmentDao().upsert(attachment(id = "attachment-existing"))
        ids.enqueue(
            "op-checklist", "mutation-checklist",
            "note-id-00000000-0000-0000-0000-000000000001", "op-note", "mutation-note",
            "attachment-id-00000000-0000-0000-0000-000000000002", "op-attachment", "mutation-attachment",
            "op-delete", "mutation-delete",
            "op-complete", "mutation-complete"
        )

        assertTrue(repository.toggleChecklistItem("job-1", "checklist-1") is AppResult.Success)
        assertTrue(repository.addNote("job-1", "user-1", "  Diagnostic OK  ") is AppResult.Success)
        assertTrue(repository.addAttachment("job-1", "user-1", photoRequest()) is AppResult.Success)
        assertTrue(repository.deleteAttachment("job-1", "attachment-existing") is AppResult.Success)
        assertTrue(repository.completeJob("job-1") is AppResult.Success)

        val job = requireNotNull(repository.observeJob("job-1").first())
        assertEquals(JobStatus.COMPLETED, job.status)
        assertTrue(job.checklist.single().completed)
        assertEquals("Diagnostic OK", job.notes.single().content)
        assertTrue(job.attachments.none { it.id == "attachment-existing" })
        assertEquals(1, job.attachments.size)
        assertEquals(SyncState.PENDING, database.attachmentDao().getById("attachment-existing")?.syncState)
        assertEquals(Instant.parse("2026-09-03T12:00:00Z"), database.attachmentDao().getById("attachment-existing")?.deletedAt)

        val operations = database.syncOperationDao().observeAllOrdered().first()
        assertEquals(
            listOf(
                SyncOperationType.UPDATE,
                SyncOperationType.CREATE,
                SyncOperationType.UPLOAD,
                SyncOperationType.DELETE,
                SyncOperationType.UPDATE
            ),
            operations.map { it.operation }
        )
        assertEquals(5, operations.map { it.clientMutationId }.toSet().size)
        assertEquals(
            ChecklistItemPayload("job-1", "checklist-1", true, now.toString()),
            payloadAt(operations, 0)
        )
        assertEquals(
            NoteCreatePayload(
                "job-1",
                "note-id-00000000-0000-0000-0000-000000000001",
                "Diagnostic OK",
                now.toString()
            ),
            payloadAt(operations, 1)
        )
        assertEquals(
            AttachmentUploadPayload("job-1", "attachment-id-00000000-0000-0000-0000-000000000002"),
            payloadAt(operations, 2)
        )
        assertEquals(
            AttachmentDeletePayload("job-1", "attachment-existing"),
            payloadAt(operations, 3)
        )
        assertEquals(
            JobStatusPayload("job-1", "COMPLETED", now.toString()),
            payloadAt(operations, 4)
        )
    }

    @Test
    fun completedJobRejectsEveryBusinessMutation() = runBlocking {
        database.jobDao().insertJob(job(status = JobStatus.COMPLETED))
        database.checklistItemDao().upsert(checklist(completed = true))
        database.attachmentDao().upsert(attachment(id = "attachment-existing"))

        assertTrue(repository.startJob("job-1") is AppResult.Failure)
        assertTrue(repository.toggleChecklistItem("job-1", "checklist-1") is AppResult.Failure)
        assertTrue(repository.addNote("job-1", "user-1", "Note") is AppResult.Failure)
        assertTrue(repository.addAttachment("job-1", "user-1", photoRequest()) is AppResult.Failure)
        assertTrue(repository.deleteAttachment("job-1", "attachment-existing") is AppResult.Failure)
        assertTrue(repository.completeJob("job-1") is AppResult.Failure)

        assertEquals(0, database.syncOperationDao().observeAllOrdered().first().size)
        assertEquals(JobStatus.COMPLETED, repository.observeJob("job-1").first()?.status)
    }

    @Test
    fun deletingLocalOnlyAttachmentCompactsItsUploadAndHardDeletesRow() = runBlocking {
        database.jobDao().insertJob(job(status = JobStatus.IN_PROGRESS))
        database.attachmentDao().upsert(
            attachment(id = "attachment-local-only").copy(
                remoteUrl = null,
                uploadedAt = null,
                syncState = SyncState.PENDING
            )
        )
        ids.enqueue("upload-op", "upload-mutation")
        database.syncOperationDao().insert(
            SyncOperationEntity(
                id = "upload-op",
                entityType = SyncEntityType.ATTACHMENT,
                entityId = "attachment-local-only",
                operation = SyncOperationType.UPLOAD,
                clientMutationId = "upload-mutation",
                status = SyncOperationStatus.PENDING,
                attemptCount = 0,
                lastErrorCode = null,
                lastErrorMessage = null,
                createdAt = now,
                lastAttemptAt = null,
                nextAttemptAt = null,
                payloadJson = "{}"
            )
        )

        assertTrue(repository.deleteAttachment("job-1", "attachment-local-only") is AppResult.Success)

        assertNull(database.attachmentDao().getById("attachment-local-only"))
        assertTrue(database.syncOperationDao().observeAllOrdered().first().isEmpty())
    }

    @Test
    fun completeJobRejectsAssignedJobWithoutOutboxMutation() = runBlocking {
        database.jobDao().insertJob(job(status = JobStatus.ASSIGNED))
        database.checklistItemDao().upsert(checklist(completed = true))

        val result = repository.completeJob("job-1")

        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).error is AppError.InvalidState)
        assertEquals(JobStatus.ASSIGNED, repository.observeJob("job-1").first()?.status)
        assertEquals(0, database.syncOperationDao().observeAllOrdered().first().size)
    }

    @Test
    fun completeJobRejectsIncompleteRequiredChecklistWithoutOutboxMutation() = runBlocking {
        database.jobDao().insertJob(job(status = JobStatus.IN_PROGRESS))
        database.checklistItemDao().upsert(checklist(completed = false))

        val result = repository.completeJob("job-1")

        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).error is AppError.InvalidState)
        assertEquals(JobStatus.IN_PROGRESS, repository.observeJob("job-1").first()?.status)
        assertNull(database.jobDao().getJobById("job-1")?.completedAt)
        assertEquals(0, database.syncOperationDao().observeAllOrdered().first().size)
    }

    @Test
    fun addNoteRejectsUnknownAuthorWithoutCreatingGhostUser() = runBlocking {
        database.jobDao().insertJob(job(status = JobStatus.IN_PROGRESS))

        val result = repository.addNote("job-1", "missing-user", "Note")

        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).error is AppError.NotFound)
        assertTrue(repository.observeJob("job-1").first()?.notes?.isEmpty() == true)
    }

    @Test
    fun outboxFailureRollsBackBusinessMutation() = runBlocking {
        database.jobDao().insertJob(job(status = JobStatus.ASSIGNED))
        database.syncOperationDao().insert(operation(id = "existing-op", clientMutationId = "duplicate-mutation"))
        ids.enqueue("new-op", "duplicate-mutation")

        val result = repository.startJob("job-1")

        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).error is AppError.Storage)
        assertEquals(JobStatus.ASSIGNED, repository.observeJob("job-1").first()?.status)
        assertEquals(1, database.syncOperationDao().observeAllOrdered().first().size)
    }

    private fun user() = UserEntity(
        id = "user-1",
        email = "amina@tervyn.demo",
        firstName = "Amina",
        lastName = "Kabwe",
        jobTitle = "Technicienne terrain",
        avatarUrl = null,
        createdAt = now,
        updatedAt = now,
        lastSyncedAt = now
    )

    private fun job(status: JobStatus) = JobEntity(
        id = "job-1",
        reference = "JOB-1",
        title = "Installation routeur",
        description = "Installer",
        clientName = "Alpha",
        siteName = "Kolwezi",
        siteAddress = "14 Avenue",
        priority = JobPriority.HIGH,
        status = status,
        scheduledAt = Instant.parse("2026-09-03T08:30:00Z"),
        startedAt = null,
        completedAt = null,
        serverVersion = 1,
        syncState = SyncState.SYNCED,
        createdAt = Instant.parse("2026-09-02T08:30:00Z"),
        updatedAt = Instant.parse("2026-09-03T08:30:00Z"),
        lastSyncedAt = now
    )

    private fun checklist(completed: Boolean) = ChecklistItemEntity(
        id = "checklist-1",
        jobId = "job-1",
        label = "Verifier",
        position = 1,
        required = true,
        completed = completed,
        completedAt = if (completed) now else null,
        serverVersion = 1,
        syncState = SyncState.SYNCED,
        updatedAt = now
    )

    private fun attachment(id: String) = AttachmentEntity(
        id = id,
        jobId = "job-1",
        authorUserId = "user-1",
        type = AttachmentType.PHOTO,
        localUri = "tervyn://demo/photo/$id",
        remoteUrl = null,
        mimeType = "image/jpeg",
        fileName = "$id.jpg",
        sizeBytes = 0,
        checksumSha256 = null,
        syncState = SyncState.SYNCED,
        createdAt = Instant.parse("2026-09-03T11:00:00Z"),
        uploadedAt = now,
        deletedAt = null
    )

    private fun operation(id: String, clientMutationId: String) = SyncOperationEntity(
        id = id,
        entityType = SyncEntityType.JOB,
        entityId = "job-1",
        operation = SyncOperationType.UPDATE,
        clientMutationId = clientMutationId,
        status = SyncOperationStatus.PENDING,
        attemptCount = 0,
        lastErrorCode = null,
        lastErrorMessage = null,
        createdAt = now,
        lastAttemptAt = null,
        nextAttemptAt = null
    )

    private fun photoRequest() = AddAttachmentRequest(
        type = AttachmentType.PHOTO,
        localUri = "tervyn://demo/photo/new",
        mimeType = "image/jpeg",
        fileName = "Photo terrain",
        sizeBytes = 42,
        checksumSha256 = null
    )

    private fun payloadAt(
        operations: List<SyncOperationEntity>,
        index: Int
    ): Any = requireNotNull(
        (SyncOperationPayloadCodec().decode(
            operations[index].payloadJson,
            operations[index].entityType,
            operations[index].operation
        ) as AppResult.Success).data
    )

    private class FixedClock : TervynClock {
        override fun now(): Instant = now
    }

    private class QueueLocalIdGenerator : LocalIdGenerator {
        private val values = ArrayDeque<String>()

        fun enqueue(vararg ids: String) {
            values.addAll(ids)
        }

        override fun newId(): String = values.removeFirst()
    }

    private companion object {
        val now: Instant = Instant.parse("2026-09-03T12:00:00Z")
    }
}
