package dev.amenokizele.tervyn.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.amenokizele.tervyn.data.local.db.TervynDatabase
import dev.amenokizele.tervyn.data.local.entity.AttachmentEntity
import dev.amenokizele.tervyn.data.local.entity.ChecklistItemEntity
import dev.amenokizele.tervyn.data.local.entity.JobEntity
import dev.amenokizele.tervyn.data.local.entity.NoteEntity
import dev.amenokizele.tervyn.data.local.entity.SyncEntityType
import dev.amenokizele.tervyn.data.local.entity.SyncOperationEntity
import dev.amenokizele.tervyn.data.local.entity.SyncOperationStatus
import dev.amenokizele.tervyn.data.local.entity.SyncOperationType
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

class RoomDaoTest {
    private lateinit var database: TervynDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, TervynDatabase::class.java)
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun jobRelations_readChecklistNotesAndVisibleAttachmentsInDeterministicOrder() = runBlocking {
        database.jobDao().upsertJob(job())
        database.checklistItemDao().upsertAll(
            listOf(checklist("c-2", 2), checklist("c-1", 1))
        )
        database.noteDao().upsertAll(
            listOf(
                note("n-2", createdAt = instant("2026-09-03T10:05:00Z")),
                note("n-1", createdAt = instant("2026-09-03T10:00:00Z"))
            )
        )
        database.attachmentDao().upsertAll(
            listOf(
                attachment("a-2", createdAt = instant("2026-09-03T11:05:00Z")),
                attachment(
                    "a-deleted",
                    createdAt = instant("2026-09-03T11:02:00Z"),
                    deletedAt = instant("2026-09-03T11:03:00Z")
                ),
                attachment("a-1", createdAt = instant("2026-09-03T11:00:00Z"))
            )
        )

        val relation = database.jobDao().observeJobWithDetails("job-1").first()

        requireNotNull(relation)
        assertEquals(listOf("c-1", "c-2"), relation.checklist.map { it.id })
        assertEquals(listOf("n-1", "n-2"), relation.notes.map { it.id })
        assertEquals(listOf("a-1", "a-2"), relation.attachments.map { it.id })
    }

    @Test
    fun jobForeignKeyCascade_removesChildrenWhenJobIsDeleted() = runBlocking {
        database.jobDao().upsertJob(job())
        database.checklistItemDao().upsert(checklist("c-1", 1))
        database.noteDao().upsert(note("n-1"))
        database.attachmentDao().upsert(attachment("a-1"))

        database.jobDao().deleteJobById("job-1")

        assertEquals(0, database.checklistItemDao().getByJobId("job-1").size)
        assertEquals(0, database.noteDao().getByJobId("job-1").size)
        assertEquals(0, database.attachmentDao().getByJobIdIncludingDeleted("job-1").size)
    }

    @Test
    fun uniqueIndexes_rejectDuplicateJobReferenceAndClientMutationId() = runBlocking {
        database.jobDao().upsertJob(job(id = "job-1", reference = "JOB-1"))
        try {
            database.jobDao().upsertJob(job(id = "job-2", reference = "JOB-1"))
        } catch (_: android.database.sqlite.SQLiteConstraintException) {
            // Expected unique reference violation.
        }
        assertNull(database.jobDao().getJobById("job-2"))

        database.syncOperationDao().insert(operation("op-1", "mutation-1"))
        try {
            database.syncOperationDao().insert(operation("op-2", "mutation-1"))
        } catch (_: android.database.sqlite.SQLiteConstraintException) {
            // Expected unique clientMutationId violation.
        }
        assertEquals(1, database.syncOperationDao().observePendingCount().first())
    }

    @Test
    fun syncOperationDao_countsAndOrdersPersistentOutbox() = runBlocking {
        database.syncOperationDao().insert(
            operation("op-2", "mutation-2", createdAt = instant("2026-09-03T10:05:00Z"))
        )
        database.syncOperationDao().insert(
            operation("op-1", "mutation-1", createdAt = instant("2026-09-03T10:00:00Z"))
        )
        database.syncOperationDao().insert(
            operation(
                id = "op-3",
                clientMutationId = "mutation-3",
                status = SyncOperationStatus.FAILED,
                createdAt = instant("2026-09-03T10:10:00Z")
            )
        )

        assertEquals(2, database.syncOperationDao().observePendingCount().first())
        assertEquals(1, database.syncOperationDao().observeFailedCount().first())
        assertEquals(listOf("op-1", "op-2", "op-3"), database.syncOperationDao().observeAllOrdered().first().map { it.id })
        assertEquals(listOf("op-1", "op-2"), database.syncOperationDao().getNextPendingOperations(limit = 5).map { it.id })
    }

    @Test
    fun databaseCanBeCreatedForInitialSchemaWithoutMigrations() {
        assertTrue(database.isOpen)
    }

    private fun job(
        id: String = "job-1",
        reference: String = "JOB-1"
    ) = JobEntity(
        id = id,
        reference = reference,
        title = "Installation routeur",
        description = "Installer",
        clientName = "Alpha",
        siteName = "Kolwezi",
        siteAddress = "14 Avenue",
        priority = JobPriority.HIGH,
        status = JobStatus.ASSIGNED,
        scheduledAt = instant("2026-09-03T08:30:00Z"),
        startedAt = null,
        completedAt = null,
        serverVersion = 1,
        syncState = SyncState.SYNCED,
        createdAt = instant("2026-09-02T08:30:00Z"),
        updatedAt = instant("2026-09-03T08:30:00Z"),
        lastSyncedAt = instant("2026-09-03T06:00:00Z")
    )

    private fun checklist(id: String, position: Int) = ChecklistItemEntity(
        id = id,
        jobId = "job-1",
        label = "Verifier",
        position = position,
        required = true,
        completed = false,
        completedAt = null,
        serverVersion = 1,
        syncState = SyncState.SYNCED,
        updatedAt = instant("2026-09-03T08:30:00Z")
    )

    private fun note(
        id: String,
        createdAt: Instant = instant("2026-09-03T10:00:00Z")
    ) = NoteEntity(
        id = id,
        jobId = "job-1",
        authorUserId = "user-1",
        content = "Note",
        createdAt = createdAt,
        updatedAt = createdAt,
        syncState = SyncState.SYNCED,
        serverVersion = 1,
        deletedAt = null
    )

    private fun attachment(
        id: String,
        createdAt: Instant = instant("2026-09-03T11:00:00Z"),
        deletedAt: Instant? = null
    ) = AttachmentEntity(
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
        createdAt = createdAt,
        uploadedAt = instant("2026-09-03T12:00:00Z"),
        deletedAt = deletedAt
    )

    private fun operation(
        id: String,
        clientMutationId: String,
        status: SyncOperationStatus = SyncOperationStatus.PENDING,
        createdAt: Instant = instant("2026-09-03T10:00:00Z")
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
        createdAt = createdAt,
        lastAttemptAt = null,
        nextAttemptAt = null
    )

    private fun instant(value: String) = Instant.parse(value)
}
