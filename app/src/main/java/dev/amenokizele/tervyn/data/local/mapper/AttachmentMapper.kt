package dev.amenokizele.tervyn.data.local.mapper

import dev.amenokizele.tervyn.data.local.entity.AttachmentEntity
import dev.amenokizele.tervyn.domain.model.Attachment

fun Attachment.toEntity() = AttachmentEntity(
    id = id,
    jobId = jobId,
    authorUserId = authorUserId,
    type = type,
    localUri = localUri,
    remoteUrl = remoteUrl,
    mimeType = mimeType,
    fileName = fileName,
    sizeBytes = sizeBytes,
    checksumSha256 = checksumSha256,
    syncState = syncState,
    createdAt = createdAt,
    uploadedAt = uploadedAt,
    deletedAt = deletedAt
)

fun AttachmentEntity.toDomain() = Attachment(
    id = id,
    jobId = jobId,
    authorUserId = authorUserId,
    type = type,
    localUri = localUri,
    remoteUrl = remoteUrl,
    mimeType = mimeType,
    fileName = fileName,
    sizeBytes = sizeBytes,
    checksumSha256 = checksumSha256,
    syncState = syncState,
    createdAt = createdAt,
    uploadedAt = uploadedAt,
    deletedAt = deletedAt
)
