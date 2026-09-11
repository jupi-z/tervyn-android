package dev.amenokizele.tervyn.data.local.sync

import androidx.room.withTransaction
import dev.amenokizele.tervyn.core.result.AppError
import dev.amenokizele.tervyn.core.result.AppResult
import dev.amenokizele.tervyn.core.time.TervynClock
import dev.amenokizele.tervyn.data.local.db.TervynDatabase
import dev.amenokizele.tervyn.data.local.entity.AttachmentEntity
import dev.amenokizele.tervyn.data.local.entity.ChecklistItemEntity
import dev.amenokizele.tervyn.data.local.entity.JobEntity
import dev.amenokizele.tervyn.data.local.entity.NoteEntity
import dev.amenokizele.tervyn.data.local.entity.SyncEntityType
import dev.amenokizele.tervyn.data.local.entity.SyncOperationEntity
import dev.amenokizele.tervyn.data.local.entity.SyncOperationStatus
import dev.amenokizele.tervyn.data.local.entity.SyncOperationType
import dev.amenokizele.tervyn.data.remote.job.RemoteJobDataSource
import dev.amenokizele.tervyn.data.remote.job.RemoteJobMutationResult
import dev.amenokizele.tervyn.data.remote.job.RemoteNoteSnapshot
import dev.amenokizele.tervyn.domain.model.SyncState
import javax.inject.Inject
import javax.inject.Singleton
import java.time.Duration

data class PushResult(
    val processedOperations: Int,
    val failedOperations: Int,
    val stopResult: SyncRunResult? = null
)

@Singleton
class OutboxExecutor @Inject constructor(
    private val database: TervynDatabase,
    private val remote: RemoteJobDataSource,
    private val clock: TervynClock,
    private val payloadCodec: SyncOperationPayloadCodec = SyncOperationPayloadCodec(),
    private val backoffPolicy: SyncBackoffPolicy = SyncBackoffPolicy()
) {
    suspend fun execute(trigger: SyncRunTrigger): PushResult {
        recoverStaleProcessing()
        var processed = 0
        var failed = 0
        val blockedEntities = mutableSetOf<String>()
        repeat(MAX_BATCHES) {
            val operations = if (trigger == SyncRunTrigger.MANUAL) {
                database.syncOperationDao().getManualOperations(BATCH_SIZE)
            } else {
                database.syncOperationDao().getDueOperations(clock.now(), BATCH_SIZE)
            }
            if (operations.isEmpty()) return PushResult(processed, failed)
            for (operation in operations) {
                val entityKey = "${operation.entityType}:${operation.entityId}"
                if (entityKey in blockedEntities) continue
                when (val result = executeOne(operation)) {
                    is OneOperationResult.Success -> processed++
                    is OneOperationResult.Failed -> {
                        processed++
                        failed++
                        if (result.blockEntity) blockedEntities += entityKey
                        if (result.stopResult != null) return PushResult(processed, failed, result.stopResult)
                    }
                }
            }
        }
        return PushResult(processed, failed)
    }

    private suspend fun executeOne(operation: SyncOperationEntity): OneOperationResult {
        val decoded = payloadCodec.decode(operation.payloadJson, operation.entityType, operation.operation)
        if (decoded is AppResult.Failure) {
            markFailed(operation, decoded.errorCode())
            return OneOperationResult.Failed(null, blockEntity = true)
        }
        val payload = (decoded as AppResult.Success).data
        val processing = markProcessing(operation) ?: return OneOperationResult.Failed(null)
        val result = when (payload) {
            is JobStatusPayload -> {
                val job = database.jobDao().getJobById(payload.jobId)
                if (job == null) AppResult.Failure(AppError.NotFound("job", payload.jobId))
                else remote.updateJobStatus(
                    jobId = payload.jobId,
                    clientMutationId = processing.clientMutationId,
                    serverVersion = job.serverVersion,
                    status = dev.amenokizele.tervyn.domain.model.JobStatus.valueOf(payload.status),
                    changedAt = java.time.Instant.parse(payload.changedAt)
                )
            }
            is ChecklistItemPayload -> {
                val item = database.checklistItemDao().getById(payload.itemId)
                if (item == null) AppResult.Failure(AppError.NotFound("checklist_item", payload.itemId))
                else remote.updateChecklistItem(
                    jobId = payload.jobId,
                    itemId = payload.itemId,
                    clientMutationId = processing.clientMutationId,
                    serverVersion = item.serverVersion,
                    completed = payload.completed,
                    completedAt = payload.completedAt?.let(java.time.Instant::parse)
                )
            }
            is NoteCreatePayload -> {
                val note = database.noteDao().getById(payload.noteId)
                if (note == null) AppResult.Failure(AppError.NotFound("note", payload.noteId))
                else remote.createNote(
                    jobId = payload.jobId,
                    noteId = payload.noteId,
                    clientMutationId = processing.clientMutationId,
                    content = payload.content,
                    createdAt = java.time.Instant.parse(payload.createdAt)
                )
            }
            is AttachmentDeletePayload -> {
                val attachment = database.attachmentDao().getById(payload.attachmentId)
                if (attachment == null) AppResult.Failure(AppError.NotFound("attachment", payload.attachmentId))
                else if (attachment.serverVersion == null) AppResult.Failure(AppError.Storage("attachment_server_version_missing"))
                else remote.deleteAttachmentMetadata(
                    jobId = payload.jobId,
                    attachmentId = payload.attachmentId,
                    clientMutationId = processing.clientMutationId,
                    serverVersion = attachment.serverVersion
                )
            }
            is AttachmentUploadPayload -> AppResult.Failure(AppError.InvalidState("attachment_upload_deferred"))
        }
        return when (result) {
            is AppResult.Success -> {
                acknowledge(processing, payload, result.data)
                OneOperationResult.Success
            }
            is AppResult.Failure -> {
                val classification = classify(result.error)
                when (classification) {
                    FailureClass.TRANSIENT -> {
                        markPending(processing, result.error.code())
                        OneOperationResult.Failed(SyncRunResult.TransientFailure(result.error))
                    }
                    FailureClass.AUTHENTICATION -> {
                        markPending(processing, result.error.code())
                        OneOperationResult.Failed(SyncRunResult.AuthenticationRequired)
                    }
                    FailureClass.STORAGE -> {
                        markFailed(processing, result.error.code())
                        OneOperationResult.Failed(SyncRunResult.StorageFailure(result.error), blockEntity = true)
                    }
                    FailureClass.PERMANENT -> {
                        markFailed(processing, result.error.code())
                        OneOperationResult.Failed(null, blockEntity = true)
                    }
                }
            }
        }
    }

    private suspend fun markProcessing(operation: SyncOperationEntity): SyncOperationEntity? {
        val now = clock.now()
        return database.withTransaction {
            val current = database.syncOperationDao().getById(operation.id) ?: return@withTransaction null
            if (current.status != SyncOperationStatus.PENDING) return@withTransaction null
            val updated = current.copy(
                status = SyncOperationStatus.PROCESSING,
                attemptCount = current.attemptCount + 1,
                lastAttemptAt = now,
                nextAttemptAt = null,
                lastErrorMessage = null
            )
            database.syncOperationDao().update(updated)
            updated
        }
    }

    private suspend fun acknowledge(
        operation: SyncOperationEntity,
        payload: SyncOperationPayload,
        response: Any
    ) {
        database.withTransaction {
            when (payload) {
                is JobStatusPayload -> acknowledgeJob(operation, payload, response as RemoteJobMutationResult)
                is ChecklistItemPayload -> acknowledgeChecklist(operation, payload, response as dev.amenokizele.tervyn.data.remote.job.RemoteChecklistItemSnapshot)
                is NoteCreatePayload -> acknowledgeNote(operation, payload, response as RemoteNoteSnapshot)
                is AttachmentDeletePayload -> acknowledgeAttachmentDelete(payload)
                is AttachmentUploadPayload -> Unit
            }
            database.syncOperationDao().deleteById(operation.id)
        }
    }

    private suspend fun acknowledgeJob(operation: SyncOperationEntity, payload: JobStatusPayload, response: RemoteJobMutationResult) {
        val job = database.jobDao().getJobById(payload.jobId) ?: return
        val remaining = database.syncOperationDao().countForEntity(SyncEntityType.JOB, payload.jobId) - 1
        database.jobDao().updateJob(
            job.copy(
                serverVersion = response.serverVersion,
                status = if (remaining == 0) response.status else job.status,
                startedAt = if (remaining == 0) response.startedAt else job.startedAt,
                completedAt = if (remaining == 0) response.completedAt else job.completedAt,
                updatedAt = if (remaining == 0) response.updatedAt else job.updatedAt,
                syncState = if (remaining == 0) SyncState.SYNCED else job.syncState,
                lastSyncedAt = if (remaining == 0) clock.now() else job.lastSyncedAt
            )
        )
    }

    private suspend fun acknowledgeChecklist(
        operation: SyncOperationEntity,
        payload: ChecklistItemPayload,
        response: dev.amenokizele.tervyn.data.remote.job.RemoteChecklistItemSnapshot
    ) {
        val item = database.checklistItemDao().getById(payload.itemId) ?: return
        val remaining = database.syncOperationDao().countForEntity(SyncEntityType.CHECKLIST_ITEM, payload.itemId) - 1
        database.checklistItemDao().update(
            item.copy(
                serverVersion = response.serverVersion,
                completed = if (remaining == 0) response.completed else item.completed,
                completedAt = if (remaining == 0) response.completedAt else item.completedAt,
                updatedAt = if (remaining == 0) response.updatedAt else item.updatedAt,
                syncState = if (remaining == 0) SyncState.SYNCED else item.syncState
            )
        )
    }

    private suspend fun acknowledgeNote(operation: SyncOperationEntity, payload: NoteCreatePayload, response: RemoteNoteSnapshot) {
        val note = database.noteDao().getById(payload.noteId) ?: return
        database.noteDao().update(
            note.copy(
                updatedAt = response.updatedAt,
                syncState = SyncState.SYNCED,
                serverVersion = response.serverVersion,
                deletedAt = response.deletedAt
            )
        )
    }

    private suspend fun acknowledgeAttachmentDelete(payload: AttachmentDeletePayload) {
        database.attachmentDao().deleteById(payload.attachmentId)
    }

    private suspend fun markPending(operation: SyncOperationEntity, code: String) {
        val now = clock.now()
        val attempt = operation.attemptCount
        database.withTransaction {
            database.syncOperationDao().updateStatus(
                id = operation.id,
                status = SyncOperationStatus.PENDING,
                attemptCount = attempt,
                lastErrorCode = code,
                lastErrorMessage = null,
                lastAttemptAt = operation.lastAttemptAt,
                nextAttemptAt = backoffPolicy.nextAttemptAt(now, attempt)
            )
        }
    }

    private suspend fun markFailed(operation: SyncOperationEntity, code: String) {
        database.withTransaction {
            database.syncOperationDao().updateStatus(
                id = operation.id,
                status = SyncOperationStatus.FAILED,
                attemptCount = operation.attemptCount,
                lastErrorCode = code,
                lastErrorMessage = null,
                lastAttemptAt = operation.lastAttemptAt,
                nextAttemptAt = null
            )
            markEntityFailed(operation)
        }
    }

    private suspend fun markEntityFailed(operation: SyncOperationEntity) {
        when (operation.entityType) {
            SyncEntityType.JOB -> database.jobDao().getJobById(operation.entityId)?.let {
                database.jobDao().updateJob(it.copy(syncState = SyncState.FAILED))
            }
            SyncEntityType.CHECKLIST_ITEM -> database.checklistItemDao().getById(operation.entityId)?.let {
                database.checklistItemDao().update(it.copy(syncState = SyncState.FAILED))
            }
            SyncEntityType.NOTE -> database.noteDao().getById(operation.entityId)?.let {
                database.noteDao().update(it.copy(syncState = SyncState.FAILED))
            }
            SyncEntityType.ATTACHMENT -> database.attachmentDao().getById(operation.entityId)?.let {
                database.attachmentDao().update(it.copy(syncState = SyncState.FAILED))
            }
        }
    }

    private suspend fun recoverStaleProcessing() {
        val cutoff = clock.now().minus(STALE_PROCESSING)
        database.withTransaction {
            database.syncOperationDao().getStaleProcessing(cutoff).forEach { operation ->
                database.syncOperationDao().updateStatus(
                    id = operation.id,
                    status = SyncOperationStatus.PENDING,
                    attemptCount = operation.attemptCount,
                    lastErrorCode = operation.lastErrorCode,
                    lastErrorMessage = null,
                    lastAttemptAt = operation.lastAttemptAt,
                    nextAttemptAt = null
                )
            }
        }
    }

    private fun classify(error: AppError): FailureClass = when (error) {
        is AppError.Authentication -> FailureClass.AUTHENTICATION
        is AppError.Network -> if (error.code in TRANSIENT_CODES) FailureClass.TRANSIENT else FailureClass.PERMANENT
        is AppError.Storage -> FailureClass.STORAGE
        is AppError.Unknown -> FailureClass.TRANSIENT
        else -> FailureClass.PERMANENT
    }

    private fun AppError.code(): String = when (this) {
        is AppError.NotFound -> "not_found"
        is AppError.Validation -> code
        is AppError.InvalidState -> code
        is AppError.Authentication -> code
        is AppError.Network -> code
        is AppError.Conflict -> code
        is AppError.Storage -> code
        is AppError.Unknown -> code ?: "unknown"
    }

    private fun AppResult.Failure.errorCode(): String = error.code()

    private sealed interface OneOperationResult {
        data object Success : OneOperationResult
        data class Failed(
            val stopResult: SyncRunResult?,
            val blockEntity: Boolean = false
        ) : OneOperationResult
    }

    private enum class FailureClass { TRANSIENT, AUTHENTICATION, STORAGE, PERMANENT }

    private companion object {
        val STALE_PROCESSING: Duration = Duration.ofMinutes(10)
        const val BATCH_SIZE = 50
        const val MAX_BATCHES = 10
        val TRANSIENT_CODES = setOf("network_unavailable", "server_error", "rate_limited")
    }
}
