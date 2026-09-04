package dev.amenokizele.tervyn.data.inmemory

import dev.amenokizele.tervyn.core.result.AppError
import dev.amenokizele.tervyn.core.result.AppResult
import dev.amenokizele.tervyn.core.time.TervynClock
import dev.amenokizele.tervyn.domain.model.AddAttachmentRequest
import dev.amenokizele.tervyn.domain.model.Attachment
import dev.amenokizele.tervyn.domain.model.Job
import dev.amenokizele.tervyn.domain.model.JobStatus
import dev.amenokizele.tervyn.domain.model.Note
import dev.amenokizele.tervyn.domain.model.SyncState
import dev.amenokizele.tervyn.domain.repository.JobRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InMemoryJobRepository @Inject constructor(
    private val store: InMemoryStore,
    private val clock: TervynClock
) : JobRepository {

    override fun observeJobs(): Flow<List<Job>> = store.jobs

    override fun observeJob(jobId: String): Flow<Job?> {
        return store.jobs
            .map { jobs -> jobs.find { it.id == jobId } }
            .distinctUntilChanged()
    }

    override suspend fun startJob(jobId: String): AppResult<Unit> {
        val now = clock.now()
        val job = findJob(jobId) ?: return AppResult.Failure(AppError.NotFound("job", jobId))
        if (job.status != JobStatus.ASSIGNED) {
            return AppResult.Failure(AppError.InvalidState("job_start_requires_assigned"))
        }
        replaceJob(
            job.copy(
                status = JobStatus.IN_PROGRESS,
                startedAt = now,
                syncState = SyncState.PENDING,
                updatedAt = now
            )
        )
        return AppResult.Success(Unit)
    }

    override suspend fun toggleChecklistItem(jobId: String, checklistItemId: String): AppResult<Unit> {
        val now = clock.now()
        val job = findJob(jobId) ?: return AppResult.Failure(AppError.NotFound("job", jobId))
        if (job.status != JobStatus.IN_PROGRESS) {
            return AppResult.Failure(AppError.InvalidState("job_must_be_in_progress"))
        }
        if (job.checklist.none { it.id == checklistItemId }) {
            return AppResult.Failure(AppError.NotFound("checklist_item", checklistItemId))
        }
        replaceJob(
            job.copy(
                checklist = job.checklist.map { item ->
                    if (item.id == checklistItemId) {
                        val completed = !item.completed
                        item.copy(
                            completed = completed,
                            completedAt = if (completed) now else null,
                            syncState = SyncState.PENDING,
                            updatedAt = now
                        )
                    } else {
                        item
                    }
                },
                syncState = SyncState.PENDING,
                updatedAt = now
            )
        )
        return AppResult.Success(Unit)
    }

    override suspend fun addNote(jobId: String, authorUserId: String, content: String): AppResult<Note> {
        val trimmedContent = content.trim()
        if (trimmedContent.isBlank()) {
            return AppResult.Failure(AppError.Validation("note_content_required"))
        }
        val now = clock.now()
        val note = Note(
            id = "note-${UUID.randomUUID().toString().take(8)}",
            jobId = jobId,
            authorUserId = authorUserId,
            content = trimmedContent,
            createdAt = now,
            updatedAt = now,
            syncState = SyncState.PENDING,
            serverVersion = null,
            deletedAt = null
        )

        val job = findJob(jobId) ?: return AppResult.Failure(AppError.NotFound("job", jobId))
        if (job.status != JobStatus.IN_PROGRESS) {
            return AppResult.Failure(AppError.InvalidState("job_must_be_in_progress"))
        }
        replaceJob(
            job.copy(
                notes = listOf(note) + job.notes,
                syncState = SyncState.PENDING,
                updatedAt = now
            )
        )
        return AppResult.Success(note)
    }

    override suspend fun addAttachment(
        jobId: String,
        authorUserId: String,
        request: AddAttachmentRequest
    ): AppResult<Attachment> {
        val now = clock.now()
        val attachment = Attachment(
            id = "photo-${UUID.randomUUID().toString().take(8)}",
            jobId = jobId,
            authorUserId = authorUserId,
            type = request.type,
            localUri = request.localUri,
            remoteUrl = null,
            mimeType = request.mimeType,
            fileName = request.fileName,
            sizeBytes = request.sizeBytes,
            checksumSha256 = request.checksumSha256,
            syncState = SyncState.PENDING,
            createdAt = now,
            uploadedAt = null,
            deletedAt = null
        )

        val job = findJob(jobId) ?: return AppResult.Failure(AppError.NotFound("job", jobId))
        if (job.status != JobStatus.IN_PROGRESS) {
            return AppResult.Failure(AppError.InvalidState("job_must_be_in_progress"))
        }
        replaceJob(
            job.copy(
                attachments = job.attachments + attachment,
                syncState = SyncState.PENDING,
                updatedAt = now
            )
        )
        return AppResult.Success(attachment)
    }

    override suspend fun deleteAttachment(jobId: String, attachmentId: String): AppResult<Unit> {
        val now = clock.now()
        val job = findJob(jobId) ?: return AppResult.Failure(AppError.NotFound("job", jobId))
        if (job.status != JobStatus.IN_PROGRESS) {
            return AppResult.Failure(AppError.InvalidState("job_must_be_in_progress"))
        }
        if (job.attachments.none { it.id == attachmentId }) {
            return AppResult.Failure(AppError.NotFound("attachment", attachmentId))
        }
        replaceJob(
            job.copy(
                attachments = job.attachments.filterNot { it.id == attachmentId },
                syncState = SyncState.PENDING,
                updatedAt = now
            )
        )
        return AppResult.Success(Unit)
    }

    override suspend fun completeJob(jobId: String): AppResult<Unit> {
        val now = clock.now()
        val job = findJob(jobId) ?: return AppResult.Failure(AppError.NotFound("job", jobId))
        if (job.status != JobStatus.IN_PROGRESS) {
            return AppResult.Failure(AppError.InvalidState("job_complete_requires_in_progress"))
        }
        if (!job.allRequiredCompleted) {
            return AppResult.Failure(AppError.Validation("required_checklist_incomplete"))
        }
        replaceJob(
            job.copy(
                status = JobStatus.COMPLETED,
                completedAt = now,
                syncState = SyncState.PENDING,
                updatedAt = now
            )
        )
        return AppResult.Success(Unit)
    }

    private fun findJob(jobId: String): Job? = store.jobsValue.find { it.id == jobId }

    private fun replaceJob(updatedJob: Job) {
        store.jobsValue = store.jobsValue.map { job ->
            if (job.id == updatedJob.id) updatedJob else job
        }
    }
}
