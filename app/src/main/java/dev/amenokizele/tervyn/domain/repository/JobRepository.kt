package dev.amenokizele.tervyn.domain.repository

import dev.amenokizele.tervyn.core.result.AppResult
import dev.amenokizele.tervyn.domain.model.AddAttachmentRequest
import dev.amenokizele.tervyn.domain.model.Attachment
import dev.amenokizele.tervyn.domain.model.Job
import dev.amenokizele.tervyn.domain.model.Note
import kotlinx.coroutines.flow.Flow

interface JobRepository {
    fun observeJobs(): Flow<List<Job>>

    fun observeJob(jobId: String): Flow<Job?>

    suspend fun startJob(jobId: String): AppResult<Unit>

    suspend fun toggleChecklistItem(jobId: String, checklistItemId: String): AppResult<Unit>

    suspend fun addNote(jobId: String, content: String): AppResult<Note>

    suspend fun addAttachment(jobId: String, request: AddAttachmentRequest): AppResult<Attachment>

    suspend fun deleteAttachment(jobId: String, attachmentId: String): AppResult<Unit>

    suspend fun completeJob(jobId: String): AppResult<Unit>
}
