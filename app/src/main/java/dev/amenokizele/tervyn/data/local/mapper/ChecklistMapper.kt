package dev.amenokizele.tervyn.data.local.mapper

import dev.amenokizele.tervyn.data.local.entity.ChecklistItemEntity
import dev.amenokizele.tervyn.domain.model.ChecklistItem

fun ChecklistItem.toEntity() = ChecklistItemEntity(
    id = id,
    jobId = jobId,
    label = label,
    position = position,
    required = required,
    completed = completed,
    completedAt = completedAt,
    serverVersion = serverVersion,
    syncState = syncState,
    updatedAt = updatedAt
)

fun ChecklistItemEntity.toDomain() = ChecklistItem(
    id = id,
    jobId = jobId,
    label = label,
    position = position,
    required = required,
    completed = completed,
    completedAt = completedAt,
    serverVersion = serverVersion,
    syncState = syncState,
    updatedAt = updatedAt
)
