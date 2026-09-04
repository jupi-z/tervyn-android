package dev.amenokizele.tervyn.data.local.mapper

import dev.amenokizele.tervyn.data.local.entity.AttachmentEntity
import dev.amenokizele.tervyn.data.local.entity.ChecklistItemEntity
import dev.amenokizele.tervyn.data.local.entity.JobEntity
import dev.amenokizele.tervyn.data.local.entity.NoteEntity
import dev.amenokizele.tervyn.data.local.relation.JobWithDetails
import dev.amenokizele.tervyn.domain.model.Attachment
import dev.amenokizele.tervyn.domain.model.ChecklistItem
import dev.amenokizele.tervyn.domain.model.Job
import dev.amenokizele.tervyn.domain.model.Note

fun Job.toEntity() = JobEntity(
    id = id,
    reference = reference,
    title = title,
    description = description,
    clientName = clientName,
    siteName = siteName,
    siteAddress = siteAddress,
    priority = priority,
    status = status,
    scheduledAt = scheduledAt,
    startedAt = startedAt,
    completedAt = completedAt,
    serverVersion = serverVersion,
    syncState = syncState,
    createdAt = createdAt,
    updatedAt = updatedAt,
    lastSyncedAt = lastSyncedAt
)

fun JobEntity.toDomain(
    checklist: List<ChecklistItem>,
    notes: List<Note>,
    attachments: List<Attachment>
) = Job(
    id = id,
    reference = reference,
    title = title,
    description = description,
    clientName = clientName,
    siteName = siteName,
    siteAddress = siteAddress,
    priority = priority,
    status = status,
    scheduledAt = scheduledAt,
    startedAt = startedAt,
    completedAt = completedAt,
    serverVersion = serverVersion,
    syncState = syncState,
    createdAt = createdAt,
    updatedAt = updatedAt,
    lastSyncedAt = lastSyncedAt,
    checklist = checklist,
    notes = notes,
    attachments = attachments
)

fun JobWithDetails.toDomain() = job.toDomain(
    checklist = checklist.sortedBy(ChecklistItemEntity::position).map(ChecklistItemEntity::toDomain),
    notes = notes.sortedBy(NoteEntity::createdAt).map(NoteEntity::toDomain),
    attachments = attachments
        .asSequence()
        .filter { it.deletedAt == null }
        .sortedBy(AttachmentEntity::createdAt)
        .map(AttachmentEntity::toDomain)
        .toList()
)
