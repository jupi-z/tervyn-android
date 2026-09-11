package dev.amenokizele.tervyn.data.local.sync

import dev.amenokizele.tervyn.core.result.AppError
import dev.amenokizele.tervyn.core.result.AppResult
import dev.amenokizele.tervyn.data.local.entity.SyncEntityType
import dev.amenokizele.tervyn.data.local.entity.SyncOperationType
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

class SyncOperationPayloadCodec(
    private val json: Json = Json {
        classDiscriminator = "type"
        encodeDefaults = true
        ignoreUnknownKeys = false
    }
) {
    fun encode(payload: SyncOperationPayload): String = json.encodeToString(payload)

    fun decode(
        json: String?,
        entityType: SyncEntityType,
        operation: SyncOperationType
    ): AppResult<SyncOperationPayload> {
        if (json == null) {
            return AppResult.Failure(AppError.Storage("legacy_sync_payload_missing"))
        }
        return try {
            val payload = this.json.decodeFromString<SyncOperationPayload>(json)
            if (payload.schemaVersion != CURRENT_SCHEMA_VERSION || !matches(payload, entityType, operation)) {
                invalidPayload()
            } else {
                AppResult.Success(payload)
            }
        } catch (_: SerializationException) {
            invalidPayload()
        } catch (_: IllegalArgumentException) {
            invalidPayload()
        }
    }

    private fun matches(
        payload: SyncOperationPayload,
        entityType: SyncEntityType,
        operation: SyncOperationType
    ): Boolean = when (payload) {
        is JobStatusPayload -> entityType == SyncEntityType.JOB && operation == SyncOperationType.UPDATE
        is ChecklistItemPayload -> entityType == SyncEntityType.CHECKLIST_ITEM && operation == SyncOperationType.UPDATE
        is NoteCreatePayload -> entityType == SyncEntityType.NOTE && operation == SyncOperationType.CREATE
        is AttachmentUploadPayload -> entityType == SyncEntityType.ATTACHMENT && operation == SyncOperationType.UPLOAD
        is AttachmentDeletePayload -> entityType == SyncEntityType.ATTACHMENT && operation == SyncOperationType.DELETE
    }

    private fun invalidPayload(): AppResult<Nothing> =
        AppResult.Failure(AppError.Storage("invalid_sync_payload"))
}
