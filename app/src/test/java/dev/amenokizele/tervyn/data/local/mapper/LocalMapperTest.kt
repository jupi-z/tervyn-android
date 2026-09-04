package dev.amenokizele.tervyn.data.local.mapper

import dev.amenokizele.tervyn.data.local.entity.AttachmentEntity
import dev.amenokizele.tervyn.data.local.entity.ChecklistItemEntity
import dev.amenokizele.tervyn.data.local.entity.JobEntity
import dev.amenokizele.tervyn.data.local.entity.NoteEntity
import dev.amenokizele.tervyn.data.local.entity.UserEntity
import dev.amenokizele.tervyn.data.local.relation.JobWithDetails
import dev.amenokizele.tervyn.domain.model.Attachment
import dev.amenokizele.tervyn.domain.model.AttachmentType
import dev.amenokizele.tervyn.domain.model.ChecklistItem
import dev.amenokizele.tervyn.domain.model.Job
import dev.amenokizele.tervyn.domain.model.JobPriority
import dev.amenokizele.tervyn.domain.model.JobStatus
import dev.amenokizele.tervyn.domain.model.Note
import dev.amenokizele.tervyn.domain.model.SyncState
import dev.amenokizele.tervyn.domain.model.User
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalMapperTest {
    private val scheduledAt = Instant.parse("2026-09-03T08:30:00Z")
    private val updatedAt = Instant.parse("2026-09-03T09:00:00Z")

    @Test
    fun jobEntityMapper_preservesScalarJobFields() {
        val job = job()

        val entity = job.toEntity()
        val domain = entity.toDomain(
            checklist = emptyList(),
            notes = emptyList(),
            attachments = emptyList()
        )

        assertEquals("job-1", entity.id)
        assertEquals("JOB-1", entity.reference)
        assertEquals(JobStatus.ASSIGNED, entity.status)
        assertEquals(job, domain)
    }

    @Test
    fun childMappers_preserveDomainValues() {
        val checklist = checklist(position = 2)
        val note = note(content = "Diagnostic terrain")
        val attachment = attachment(id = "photo-1", deletedAt = null)
        val user = user()

        assertEquals(checklist, checklist.toEntity().toDomain())
        assertEquals(note, note.toEntity().toDomain())
        assertEquals(attachment, attachment.toEntity().toDomain())
        assertEquals(user, user.toEntity().toDomain())
    }

    @Test
    fun jobWithDetailsMapper_ordersChildrenAndHidesSoftDeletedAttachments() {
        val relation = JobWithDetails(
            job = job().toEntity(),
            checklist = listOf(checklist("c-2", 2), checklist("c-1", 1)).map { it.toEntity() },
            notes = listOf(
                note("n-2", "Second", Instant.parse("2026-09-03T10:05:00Z")),
                note("n-1", "First", Instant.parse("2026-09-03T10:00:00Z"))
            ).map { it.toEntity() },
            attachments = listOf(
                attachment("photo-2", createdAt = Instant.parse("2026-09-03T11:05:00Z"), deletedAt = null),
                attachment("photo-deleted", createdAt = Instant.parse("2026-09-03T11:02:00Z"), deletedAt = Instant.parse("2026-09-03T11:03:00Z")),
                attachment("photo-1", createdAt = Instant.parse("2026-09-03T11:00:00Z"), deletedAt = null)
            ).map { it.toEntity() }
        )

        val domain = relation.toDomain()

        assertEquals(listOf("c-1", "c-2"), domain.checklist.map { it.id })
        assertEquals(listOf("n-1", "n-2"), domain.notes.map { it.id })
        assertEquals(listOf("photo-1", "photo-2"), domain.attachments.map { it.id })
        assertTrue(domain.attachments.none { it.deletedAt != null })
    }

    private fun user() = User(
        id = "user-1",
        email = "amina@tervyn.demo",
        firstName = "Amina",
        lastName = "Kabwe",
        jobTitle = "Technicienne terrain",
        avatarUrl = null,
        createdAt = scheduledAt,
        updatedAt = updatedAt,
        lastSyncedAt = updatedAt
    )

    private fun job() = Job(
        id = "job-1",
        reference = "JOB-1",
        title = "Installation routeur",
        description = "Installer et tester",
        clientName = "Alpha",
        siteName = "Kolwezi",
        siteAddress = "14 Avenue",
        priority = JobPriority.HIGH,
        status = JobStatus.ASSIGNED,
        scheduledAt = scheduledAt,
        startedAt = null,
        completedAt = null,
        serverVersion = 7,
        syncState = SyncState.SYNCED,
        createdAt = scheduledAt.minusSeconds(3600),
        updatedAt = updatedAt,
        lastSyncedAt = updatedAt
    )

    private fun checklist(id: String = "c-1", position: Int = 1) = ChecklistItem(
        id = id,
        jobId = "job-1",
        label = "Verifier",
        position = position,
        required = true,
        completed = false,
        completedAt = null,
        serverVersion = 3,
        syncState = SyncState.SYNCED,
        updatedAt = updatedAt
    )

    private fun note(
        id: String = "n-1",
        content: String = "Note",
        createdAt: Instant = updatedAt
    ) = Note(
        id = id,
        jobId = "job-1",
        authorUserId = "user-1",
        content = content,
        createdAt = createdAt,
        updatedAt = createdAt,
        syncState = SyncState.SYNCED,
        serverVersion = 2,
        deletedAt = null
    )

    private fun attachment(
        id: String,
        createdAt: Instant = updatedAt,
        deletedAt: Instant?
    ) = Attachment(
        id = id,
        jobId = "job-1",
        authorUserId = "user-1",
        type = AttachmentType.PHOTO,
        localUri = "tervyn://demo/photo/$id",
        remoteUrl = null,
        mimeType = "image/jpeg",
        fileName = "$id.jpg",
        sizeBytes = 42,
        checksumSha256 = null,
        syncState = SyncState.SYNCED,
        createdAt = createdAt,
        uploadedAt = updatedAt,
        deletedAt = deletedAt
    )
}
