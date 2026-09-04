package dev.amenokizele.tervyn.data.local.repository

import android.database.SQLException
import androidx.room.withTransaction
import dev.amenokizele.tervyn.core.result.AppError
import dev.amenokizele.tervyn.core.result.AppResult
import dev.amenokizele.tervyn.core.time.TervynClock
import dev.amenokizele.tervyn.data.local.db.TervynDatabase
import dev.amenokizele.tervyn.data.local.entity.AttachmentEntity
import dev.amenokizele.tervyn.data.local.entity.NoteEntity
import dev.amenokizele.tervyn.data.local.entity.SyncEntityType
import dev.amenokizele.tervyn.data.local.entity.SyncOperationEntity
import dev.amenokizele.tervyn.data.local.entity.SyncOperationStatus
import dev.amenokizele.tervyn.data.local.entity.SyncOperationType
import dev.amenokizele.tervyn.data.local.mapper.toDomain
import dev.amenokizele.tervyn.domain.model.AddAttachmentRequest
import dev.amenokizele.tervyn.domain.model.Attachment
import dev.amenokizele.tervyn.domain.model.Job
import dev.amenokizele.tervyn.domain.model.JobStatus
import dev.amenokizele.tervyn.domain.model.Note
import dev.amenokizele.tervyn.domain.model.SyncState
import dev.amenokizele.tervyn.domain.repository.JobRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomJobRepository @Inject constructor(
    private val database: TervynDatabase,
    private val clock: TervynClock,
    private val idGenerator: LocalIdGenerator
) : JobRepository {
    private val jobDao = database.jobDao()
    private val checklistItemDao = database.checklistItemDao()
    private val noteDao = database.noteDao()
    private val attachmentDao = database.attachmentDao()
    private val userDao = database.userDao()
    private val syncOperationDao = database.syncOperationDao()

    override fun observeJobs(): Flow<List<Job>> {
        return jobDao.observeJobsWithDetails().map { relations ->
            relations.map { it.toDomain() }
        }
    }

    override fun observeJob(jobId: String): Flow<Job?> {
        return jobDao.observeJobWithDetails(jobId).map { relation -> relation?.toDomain() }
    }

    override suspend fun startJob(jobId: String): AppResult<Unit> = storageResult {
        val now = clock.now()
        database.withTransaction {
            val job = jobDao.getJobById(jobId)
                ?: return@withTransaction AppResult.Failure(AppError.NotFound("job", jobId))
            if (job.status != JobStatus.ASSIGNED) {
                return@withTransaction AppResult.Failure(AppError.InvalidState("job_start_requires_assigned"))
            }
            jobDao.updateJob(
                job.copy(
                    status = JobStatus.IN_PROGRESS,
                    startedAt = now,
                    syncState = SyncState.PENDING,
                    updatedAt = now
                )
            )
            enqueue(SyncEntityType.JOB, jobId, SyncOperationType.UPDATE, now)
            AppResult.Success(Unit)
        }
    }

    override suspend fun toggleChecklistItem(jobId: String, checklistItemId: String): AppResult<Unit> = storageResult {
        val now = clock.now()
        database.withTransaction {
            val job = jobDao.getJobById(jobId)
                ?: return@withTransaction AppResult.Failure(AppError.NotFound("job", jobId))
            if (job.status != JobStatus.IN_PROGRESS) {
                return@withTransaction AppResult.Failure(AppError.InvalidState("job_must_be_in_progress"))
            }
            val item = checklistItemDao.getById(checklistItemId)
                ?: return@withTransaction AppResult.Failure(AppError.NotFound("checklist_item", checklistItemId))
            if (item.jobId != jobId) {
                return@withTransaction AppResult.Failure(AppError.NotFound("checklist_item", checklistItemId))
            }
            val completed = !item.completed
            checklistItemDao.update(
                item.copy(
                    completed = completed,
                    completedAt = if (completed) now else null,
                    syncState = SyncState.PENDING,
                    updatedAt = now
                )
            )
            enqueue(SyncEntityType.CHECKLIST_ITEM, checklistItemId, SyncOperationType.UPDATE, now)
            AppResult.Success(Unit)
        }
    }

    override suspend fun addNote(jobId: String, authorUserId: String, content: String): AppResult<Note> = storageResult {
        val trimmedContent = content.trim()
        if (trimmedContent.isBlank()) {
            return@storageResult AppResult.Failure(AppError.Validation("note_content_required"))
        }
        val now = clock.now()
        database.withTransaction {
            val job = jobDao.getJobById(jobId)
                ?: return@withTransaction AppResult.Failure(AppError.NotFound("job", jobId))
            if (job.status != JobStatus.IN_PROGRESS) {
                return@withTransaction AppResult.Failure(AppError.InvalidState("job_must_be_in_progress"))
            }
            userDao.getById(authorUserId)
                ?: return@withTransaction AppResult.Failure(AppError.NotFound("user", authorUserId))
            val note = NoteEntity(
                id = idGenerator.newId(),
                jobId = jobId,
                authorUserId = authorUserId,
                content = trimmedContent,
                createdAt = now,
                updatedAt = now,
                syncState = SyncState.PENDING,
                serverVersion = null,
                deletedAt = null
            )
            noteDao.upsert(note)
            enqueue(SyncEntityType.NOTE, note.id, SyncOperationType.CREATE, now)
            AppResult.Success(note.toDomain())
        }
    }

    override suspend fun addAttachment(
        jobId: String,
        authorUserId: String,
        request: AddAttachmentRequest
    ): AppResult<Attachment> = storageResult {
        val now = clock.now()
        database.withTransaction {
            val job = jobDao.getJobById(jobId)
                ?: return@withTransaction AppResult.Failure(AppError.NotFound("job", jobId))
            if (job.status != JobStatus.IN_PROGRESS) {
                return@withTransaction AppResult.Failure(AppError.InvalidState("job_must_be_in_progress"))
            }
            userDao.getById(authorUserId)
                ?: return@withTransaction AppResult.Failure(AppError.NotFound("user", authorUserId))
            val attachment = AttachmentEntity(
                id = idGenerator.newId(),
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
            attachmentDao.upsert(attachment)
            enqueue(SyncEntityType.ATTACHMENT, attachment.id, SyncOperationType.UPLOAD, now)
            AppResult.Success(attachment.toDomain())
        }
    }

    override suspend fun deleteAttachment(jobId: String, attachmentId: String): AppResult<Unit> = storageResult {
        val now = clock.now()
        database.withTransaction {
            val job = jobDao.getJobById(jobId)
                ?: return@withTransaction AppResult.Failure(AppError.NotFound("job", jobId))
            if (job.status != JobStatus.IN_PROGRESS) {
                return@withTransaction AppResult.Failure(AppError.InvalidState("job_must_be_in_progress"))
            }
            val attachment = attachmentDao.getById(attachmentId)
                ?: return@withTransaction AppResult.Failure(AppError.NotFound("attachment", attachmentId))
            if (attachment.jobId != jobId || attachment.deletedAt != null) {
                return@withTransaction AppResult.Failure(AppError.NotFound("attachment", attachmentId))
            }
            attachmentDao.update(
                attachment.copy(
                    deletedAt = now,
                    syncState = SyncState.PENDING
                )
            )
            enqueue(SyncEntityType.ATTACHMENT, attachmentId, SyncOperationType.DELETE, now)
            AppResult.Success(Unit)
        }
    }

    override suspend fun completeJob(jobId: String): AppResult<Unit> = storageResult {
        val now = clock.now()
        database.withTransaction {
            val job = jobDao.getJobById(jobId)
                ?: return@withTransaction AppResult.Failure(AppError.NotFound("job", jobId))
            if (job.status != JobStatus.IN_PROGRESS) {
                return@withTransaction AppResult.Failure(AppError.InvalidState("job_complete_requires_in_progress"))
            }
            val requiredIncomplete = checklistItemDao.getByJobId(jobId)
                .any { it.required && !it.completed }
            if (requiredIncomplete) {
                return@withTransaction AppResult.Failure(AppError.InvalidState("required_checklist_incomplete"))
            }
            jobDao.updateJob(
                job.copy(
                    status = JobStatus.COMPLETED,
                    completedAt = now,
                    syncState = SyncState.PENDING,
                    updatedAt = now
                )
            )
            enqueue(SyncEntityType.JOB, jobId, SyncOperationType.UPDATE, now)
            AppResult.Success(Unit)
        }
    }

    private suspend fun enqueue(
        entityType: SyncEntityType,
        entityId: String,
        operation: SyncOperationType,
        now: java.time.Instant
    ) {
        syncOperationDao.insert(
            SyncOperationEntity(
                id = idGenerator.newId(),
                entityType = entityType,
                entityId = entityId,
                operation = operation,
                clientMutationId = idGenerator.newId(),
                status = SyncOperationStatus.PENDING,
                attemptCount = 0,
                lastErrorCode = null,
                lastErrorMessage = null,
                createdAt = now,
                lastAttemptAt = null,
                nextAttemptAt = null
            )
        )
    }

    private suspend fun <T> storageResult(block: suspend () -> AppResult<T>): AppResult<T> {
        return try {
            block()
        } catch (_: SQLException) {
            AppResult.Failure(AppError.Storage("local_database_error"))
        }
    }
}
