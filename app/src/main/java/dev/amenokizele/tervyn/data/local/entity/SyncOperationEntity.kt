package dev.amenokizele.tervyn.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

enum class SyncEntityType {
    JOB,
    CHECKLIST_ITEM,
    NOTE,
    ATTACHMENT
}

enum class SyncOperationType {
    UPDATE,
    CREATE,
    DELETE,
    UPLOAD
}

enum class SyncOperationStatus {
    PENDING,
    PROCESSING,
    FAILED
}

@Entity(
    tableName = "sync_operations",
    indices = [
        Index("status"),
        Index("createdAt"),
        Index(value = ["entityType", "entityId"]),
        Index(value = ["clientMutationId"], unique = true)
    ]
)
data class SyncOperationEntity(
    @PrimaryKey val id: String,
    val entityType: SyncEntityType,
    val entityId: String,
    val operation: SyncOperationType,
    val clientMutationId: String,
    val status: SyncOperationStatus,
    val attemptCount: Int,
    val lastErrorCode: String?,
    val lastErrorMessage: String?,
    val createdAt: Instant,
    val lastAttemptAt: Instant?,
    val nextAttemptAt: Instant?
)
