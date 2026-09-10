package dev.amenokizele.tervyn.data.remote.job

import dev.amenokizele.tervyn.domain.model.AttachmentType
import dev.amenokizele.tervyn.domain.model.JobPriority
import dev.amenokizele.tervyn.domain.model.JobStatus
import java.time.Instant

data class RemoteJobsPage(
    val items: List<RemoteJobSnapshot>,
    val nextCursor: String?,
    val serverTime: Instant
)

data class RemoteJobSnapshot(
    val id: String,
    val reference: String,
    val title: String,
    val description: String?,
    val clientName: String,
    val siteName: String,
    val siteAddress: String?,
    val priority: JobPriority,
    val status: JobStatus,
    val scheduledAt: Instant,
    val startedAt: Instant?,
    val completedAt: Instant?,
    val serverVersion: Long,
    val createdAt: Instant,
    val updatedAt: Instant,
    val checklist: List<RemoteChecklistItemSnapshot>,
    val notes: List<RemoteNoteSnapshot>,
    val attachments: List<RemoteAttachmentSnapshot>
)

data class RemoteChecklistItemSnapshot(
    val id: String,
    val jobId: String,
    val label: String,
    val position: Int,
    val required: Boolean,
    val completed: Boolean,
    val completedAt: Instant?,
    val serverVersion: Long,
    val updatedAt: Instant
)

data class RemoteNoteSnapshot(
    val id: String,
    val jobId: String,
    val authorUserId: String,
    val content: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val serverVersion: Long?,
    val deletedAt: Instant?
)

data class RemoteAttachmentSnapshot(
    val id: String,
    val jobId: String,
    val authorUserId: String,
    val type: AttachmentType,
    val remoteUrl: String?,
    val mimeType: String,
    val fileName: String?,
    val sizeBytes: Long,
    val checksumSha256: String?,
    val serverVersion: Long?,
    val createdAt: Instant,
    val uploadedAt: Instant?,
    val deletedAt: Instant?
)

data class RemoteJobMutationResult(
    val id: String,
    val serverVersion: Long,
    val updatedAt: Instant,
    val status: JobStatus,
    val startedAt: Instant?,
    val completedAt: Instant?
)
