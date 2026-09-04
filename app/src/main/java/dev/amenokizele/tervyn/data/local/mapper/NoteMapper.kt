package dev.amenokizele.tervyn.data.local.mapper

import dev.amenokizele.tervyn.data.local.entity.NoteEntity
import dev.amenokizele.tervyn.domain.model.Note

fun Note.toEntity() = NoteEntity(
    id = id,
    jobId = jobId,
    authorUserId = authorUserId,
    content = content,
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncState = syncState,
    serverVersion = serverVersion,
    deletedAt = deletedAt
)

fun NoteEntity.toDomain() = Note(
    id = id,
    jobId = jobId,
    authorUserId = authorUserId,
    content = content,
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncState = syncState,
    serverVersion = serverVersion,
    deletedAt = deletedAt
)
