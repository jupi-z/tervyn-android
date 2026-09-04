package dev.amenokizele.tervyn.fake

import dev.amenokizele.tervyn.core.result.AppError
import dev.amenokizele.tervyn.core.result.AppResult
import dev.amenokizele.tervyn.core.time.TervynClock
import dev.amenokizele.tervyn.data.fixtures.TervynDemoFixtures
import dev.amenokizele.tervyn.domain.model.AddAttachmentRequest
import dev.amenokizele.tervyn.domain.model.Attachment
import dev.amenokizele.tervyn.domain.model.Job
import dev.amenokizele.tervyn.domain.model.JobStatus
import dev.amenokizele.tervyn.domain.model.Note
import dev.amenokizele.tervyn.domain.model.SyncState
import dev.amenokizele.tervyn.domain.repository.JobRepository
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class FakeJobRepository(
    private val clock: TervynClock,
    initialJobs: List<Job> = TervynDemoFixtures.initialJobs()
) : JobRepository {
    private val jobs = MutableStateFlow(initialJobs)

    override fun observeJobs(): Flow<List<Job>> = jobs

    override fun observeJob(jobId: String): Flow<Job?> {
        return jobs.map { list -> list.find { it.id == jobId } }.distinctUntilChanged()
    }

    override suspend fun startJob(jobId: String): AppResult<Unit> {
        val job = findJob(jobId) ?: return AppResult.Failure(AppError.NotFound("job", jobId))
        if (job.status != JobStatus.ASSIGNED) {
            return AppResult.Failure(AppError.InvalidState("job_start_requires_assigned"))
        }
        val now = clock.now()
        replaceJob(job.copy(status = JobStatus.IN_PROGRESS, startedAt = now, updatedAt = now, syncState = SyncState.PENDING))
        return AppResult.Success(Unit)
    }

    override suspend fun toggleChecklistItem(jobId: String, checklistItemId: String): AppResult<Unit> {
        val job = findJob(jobId) ?: return AppResult.Failure(AppError.NotFound("job", jobId))
        if (job.status != JobStatus.IN_PROGRESS) {
            return AppResult.Failure(AppError.InvalidState("job_must_be_in_progress"))
        }
        val now = clock.now()
        if (job.checklist.none { it.id == checklistItemId }) {
            return AppResult.Failure(AppError.NotFound("checklist_item", checklistItemId))
        }
        replaceJob(
            job.copy(
                checklist = job.checklist.map { item ->
                    if (item.id == checklistItemId) {
                        val completed = !item.completed
                        item.copy(completed = completed, completedAt = if (completed) now else null, updatedAt = now, syncState = SyncState.PENDING)
                    } else {
                        item
                    }
                }
            )
        )
        return AppResult.Success(Unit)
    }

    override suspend fun addNote(jobId: String, authorUserId: String, content: String): AppResult<Note> {
        val job = findJob(jobId) ?: return AppResult.Failure(AppError.NotFound("job", jobId))
        if (job.status != JobStatus.IN_PROGRESS) {
            return AppResult.Failure(AppError.InvalidState("job_must_be_in_progress"))
        }
        val now = clock.now()
        val note = Note(
            id = UUID.randomUUID().toString(),
            jobId = jobId,
            authorUserId = authorUserId,
            content = content.trim(),
            createdAt = now,
            updatedAt = now,
            syncState = SyncState.PENDING,
            serverVersion = null,
            deletedAt = null
        )
        replaceJob(job.copy(notes = listOf(note) + job.notes))
        return AppResult.Success(note)
    }

    override suspend fun addAttachment(
        jobId: String,
        authorUserId: String,
        request: AddAttachmentRequest
    ): AppResult<Attachment> {
        val job = findJob(jobId) ?: return AppResult.Failure(AppError.NotFound("job", jobId))
        if (job.status != JobStatus.IN_PROGRESS) {
            return AppResult.Failure(AppError.InvalidState("job_must_be_in_progress"))
        }
        val now = clock.now()
        val attachment = Attachment(
            id = UUID.randomUUID().toString(),
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
        replaceJob(job.copy(attachments = job.attachments + attachment))
        return AppResult.Success(attachment)
    }

    override suspend fun deleteAttachment(jobId: String, attachmentId: String): AppResult<Unit> {
        val job = findJob(jobId) ?: return AppResult.Failure(AppError.NotFound("job", jobId))
        if (job.status != JobStatus.IN_PROGRESS) {
            return AppResult.Failure(AppError.InvalidState("job_must_be_in_progress"))
        }
        if (job.attachments.none { it.id == attachmentId }) {
            return AppResult.Failure(AppError.NotFound("attachment", attachmentId))
        }
        replaceJob(job.copy(attachments = job.attachments.filterNot { it.id == attachmentId }))
        return AppResult.Success(Unit)
    }

    override suspend fun completeJob(jobId: String): AppResult<Unit> {
        val job = findJob(jobId) ?: return AppResult.Failure(AppError.NotFound("job", jobId))
        if (job.status != JobStatus.IN_PROGRESS) {
            return AppResult.Failure(AppError.InvalidState("job_complete_requires_in_progress"))
        }
        if (!job.allRequiredCompleted) {
            return AppResult.Failure(AppError.InvalidState("required_checklist_incomplete"))
        }
        val now = clock.now()
        replaceJob(job.copy(status = JobStatus.COMPLETED, completedAt = now, updatedAt = now, syncState = SyncState.PENDING))
        return AppResult.Success(Unit)
    }

    private fun findJob(jobId: String): Job? = jobs.value.find { it.id == jobId }

    private fun replaceJob(updated: Job) {
        jobs.value = jobs.value.map { if (it.id == updated.id) updated else it }
    }
}
