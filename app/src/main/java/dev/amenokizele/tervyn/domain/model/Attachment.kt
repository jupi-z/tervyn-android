package dev.amenokizele.tervyn.domain.model

import java.time.Instant

data class Attachment(
    val id: String,
    val jobId: String,
    val authorUserId: String,
    val type: AttachmentType,
    val localUri: String?,
    val remoteUrl: String?,
    val mimeType: String,
    val fileName: String?,
    val sizeBytes: Long,
    val checksumSha256: String?,
    val syncState: SyncState,
    val createdAt: Instant,
    val uploadedAt: Instant?,
    val deletedAt: Instant?
)
