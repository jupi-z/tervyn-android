package dev.amenokizele.tervyn.data.local.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.amenokizele.tervyn.data.local.entity.AttachmentEntity
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
import org.junit.Assert.assertEquals
import org.junit.Test

class RoomPersistenceTest {
    @Test
    fun fileDatabasePersistsJobNoteAttachmentAndOutboxAcrossReopen() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "tervyn-persistence-test.db"
        context.deleteDatabase(dbName)

        var database = openDatabase(context, dbName)
        database.jobDao().upsertJob(job(status = JobStatus.IN_PROGRESS))
        database.noteDao().upsert(note())
        database.attachmentDao().upsert(attachment())
        database.syncOperationDao().insert(operation())
        database.close()

        database = openDatabase(context, dbName)
        val reopenedJob = requireNotNull(database.jobDao().getJobWithDetails("job-1"))

        assertEquals(JobStatus.IN_PROGRESS, reopenedJob.job.status)
        assertEquals(listOf("note-1"), reopenedJob.notes.map { it.id })
        assertEquals(listOf("attachment-1"), reopenedJob.attachments.map { it.id })
        assertEquals(1, database.syncOperationDao().observePendingCount().first())

        database.close()
        context.deleteDatabase(dbName)
    }

    private fun openDatabase(context: Context, dbName: String): TervynDatabase {
        return Room.databaseBuilder(context, TervynDatabase::class.java, dbName).build()
    }

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
        scheduledAt = instant("2026-09-03T08:30:00Z"),
        startedAt = instant("2026-09-03T08:35:00Z"),
        completedAt = null,
        serverVersion = 1,
        syncState = SyncState.PENDING,
        createdAt = instant("2026-09-02T08:30:00Z"),
        updatedAt = instant("2026-09-03T08:35:00Z"),
        lastSyncedAt = null
    )

    private fun note() = NoteEntity(
        id = "note-1",
        jobId = "job-1",
        authorUserId = "user-1",
        content = "Persisted note",
        createdAt = instant("2026-09-03T10:00:00Z"),
        updatedAt = instant("2026-09-03T10:00:00Z"),
        syncState = SyncState.PENDING,
        serverVersion = null,
        deletedAt = null
    )

    private fun attachment() = AttachmentEntity(
        id = "attachment-1",
        jobId = "job-1",
        authorUserId = "user-1",
        type = AttachmentType.PHOTO,
        localUri = "tervyn://demo/photo/attachment-1",
        remoteUrl = null,
        mimeType = "image/jpeg",
        fileName = "attachment-1.jpg",
        sizeBytes = 42,
        checksumSha256 = null,
        syncState = SyncState.PENDING,
        createdAt = instant("2026-09-03T11:00:00Z"),
        uploadedAt = null,
        deletedAt = null
    )

    private fun operation() = SyncOperationEntity(
        id = "operation-1",
        entityType = SyncEntityType.JOB,
        entityId = "job-1",
        operation = SyncOperationType.UPDATE,
        clientMutationId = "mutation-1",
        status = SyncOperationStatus.PENDING,
        attemptCount = 0,
        lastErrorCode = null,
        lastErrorMessage = null,
        createdAt = instant("2026-09-03T12:00:00Z"),
        lastAttemptAt = null,
        nextAttemptAt = null
    )

    private fun instant(value: String): Instant = Instant.parse(value)
}
