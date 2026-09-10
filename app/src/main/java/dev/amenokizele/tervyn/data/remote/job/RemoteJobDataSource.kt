package dev.amenokizele.tervyn.data.remote.job

import dev.amenokizele.tervyn.core.result.AppResult
import dev.amenokizele.tervyn.domain.model.JobStatus
import java.time.Instant

interface RemoteJobDataSource {
    suspend fun fetchJobs(
        cursor: String? = null,
        limit: Int = 50,
        updatedAfter: Instant? = null
    ): AppResult<RemoteJobsPage>

    suspend fun fetchJob(jobId: String): AppResult<RemoteJobSnapshot>

    suspend fun updateJobStatus(
        jobId: String,
        clientMutationId: String,
        serverVersion: Long,
        status: JobStatus,
        changedAt: Instant
    ): AppResult<RemoteJobMutationResult>

    suspend fun updateChecklistItem(
        jobId: String,
        itemId: String,
        clientMutationId: String,
        serverVersion: Long,
        completed: Boolean,
        completedAt: Instant?
    ): AppResult<RemoteChecklistItemSnapshot>

    suspend fun createNote(
        jobId: String,
        noteId: String,
        clientMutationId: String,
        content: String,
        createdAt: Instant
    ): AppResult<RemoteNoteSnapshot>

    suspend fun deleteAttachmentMetadata(
        jobId: String,
        attachmentId: String,
        clientMutationId: String,
        serverVersion: Long
    ): AppResult<Unit>
}
