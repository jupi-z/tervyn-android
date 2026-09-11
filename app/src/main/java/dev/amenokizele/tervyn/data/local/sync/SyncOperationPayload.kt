package dev.amenokizele.tervyn.data.local.sync

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface SyncOperationPayload {
    val schemaVersion: Int
}

@Serializable
@SerialName("job_status")
data class JobStatusPayload(
    val jobId: String,
    val status: String,
    val changedAt: String,
    override val schemaVersion: Int = CURRENT_SCHEMA_VERSION
) : SyncOperationPayload

@Serializable
@SerialName("checklist_item")
data class ChecklistItemPayload(
    val jobId: String,
    val itemId: String,
    val completed: Boolean,
    val completedAt: String?,
    override val schemaVersion: Int = CURRENT_SCHEMA_VERSION
) : SyncOperationPayload

@Serializable
@SerialName("note_create")
data class NoteCreatePayload(
    val jobId: String,
    val noteId: String,
    val content: String,
    val createdAt: String,
    override val schemaVersion: Int = CURRENT_SCHEMA_VERSION
) : SyncOperationPayload

@Serializable
@SerialName("attachment_upload")
data class AttachmentUploadPayload(
    val jobId: String,
    val attachmentId: String,
    override val schemaVersion: Int = CURRENT_SCHEMA_VERSION
) : SyncOperationPayload

@Serializable
@SerialName("attachment_delete")
data class AttachmentDeletePayload(
    val jobId: String,
    val attachmentId: String,
    override val schemaVersion: Int = CURRENT_SCHEMA_VERSION
) : SyncOperationPayload

const val CURRENT_SCHEMA_VERSION = 1
