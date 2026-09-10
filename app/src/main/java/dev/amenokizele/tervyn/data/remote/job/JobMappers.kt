package dev.amenokizele.tervyn.data.remote.job

import dev.amenokizele.tervyn.domain.model.AttachmentType
import dev.amenokizele.tervyn.domain.model.JobPriority
import dev.amenokizele.tervyn.domain.model.JobStatus
import java.time.Instant

fun JobsPageDto.toRemoteJobsPage() = RemoteJobsPage(
    items = items.map { it.toRemoteJobSnapshot() },
    nextCursor = nextCursor,
    serverTime = Instant.parse(serverTime)
)

fun JobDto.toRemoteJobSnapshot() = RemoteJobSnapshot(
    id = id.requireNotBlank(),
    reference = reference.requireNotBlank(),
    title = title.requireNotBlank(),
    description = description,
    clientName = clientName.requireNotBlank(),
    siteName = siteName.requireNotBlank(),
    siteAddress = siteAddress,
    priority = priority.toJobPriority(),
    status = status.toJobStatus(),
    scheduledAt = Instant.parse(scheduledAt),
    startedAt = startedAt?.let(Instant::parse),
    completedAt = completedAt?.let(Instant::parse),
    serverVersion = serverVersion,
    createdAt = Instant.parse(createdAt),
    updatedAt = Instant.parse(updatedAt),
    checklist = checklist.map { it.toRemoteChecklistItemSnapshot() },
    notes = notes.map { it.toRemoteNoteSnapshot() },
    attachments = attachments.map { it.toRemoteAttachmentSnapshot() }
)

fun ChecklistItemDto.toRemoteChecklistItemSnapshot() = RemoteChecklistItemSnapshot(
    id = id.requireNotBlank(),
    jobId = jobId.requireNotBlank(),
    label = label.requireNotBlank(),
    position = position,
    required = required,
    completed = completed,
    completedAt = completedAt?.let(Instant::parse),
    serverVersion = serverVersion,
    updatedAt = Instant.parse(updatedAt)
)

fun NoteDto.toRemoteNoteSnapshot() = RemoteNoteSnapshot(
    id = id.requireNotBlank(),
    jobId = jobId.requireNotBlank(),
    authorUserId = authorUserId.requireNotBlank(),
    content = content.requireNotBlank(),
    createdAt = Instant.parse(createdAt),
    updatedAt = Instant.parse(updatedAt),
    serverVersion = serverVersion,
    deletedAt = deletedAt?.let(Instant::parse)
)

fun AttachmentDto.toRemoteAttachmentSnapshot() = RemoteAttachmentSnapshot(
    id = id.requireNotBlank(),
    jobId = jobId.requireNotBlank(),
    authorUserId = authorUserId.requireNotBlank(),
    type = type.toAttachmentType(),
    remoteUrl = remoteUrl,
    mimeType = mimeType.requireNotBlank(),
    fileName = fileName,
    sizeBytes = sizeBytes,
    checksumSha256 = checksumSha256,
    serverVersion = serverVersion,
    createdAt = Instant.parse(createdAt),
    uploadedAt = uploadedAt?.let(Instant::parse),
    deletedAt = deletedAt?.let(Instant::parse)
)

fun JobMutationResponseDto.toRemoteJobMutationResult() = RemoteJobMutationResult(
    id = id.requireNotBlank(),
    serverVersion = serverVersion,
    updatedAt = Instant.parse(updatedAt),
    status = status.toJobStatus(),
    startedAt = startedAt?.let(Instant::parse),
    completedAt = completedAt?.let(Instant::parse)
)

private fun String.toJobPriority(): JobPriority = when (this) {
    "LOW" -> JobPriority.LOW
    "NORMAL" -> JobPriority.NORMAL
    "HIGH" -> JobPriority.HIGH
    "URGENT" -> JobPriority.URGENT
    else -> throw IllegalArgumentException("unknown job priority")
}

private fun String.toJobStatus(): JobStatus = when (this) {
    "ASSIGNED" -> JobStatus.ASSIGNED
    "IN_PROGRESS" -> JobStatus.IN_PROGRESS
    "COMPLETED" -> JobStatus.COMPLETED
    else -> throw IllegalArgumentException("unknown job status")
}

private fun String.toAttachmentType(): AttachmentType = when (this) {
    "PHOTO" -> AttachmentType.PHOTO
    else -> throw IllegalArgumentException("unknown attachment type")
}

private fun String.requireNotBlank(): String {
    require(isNotBlank())
    return this
}
