package dev.amenokizele.tervyn.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.amenokizele.tervyn.domain.model.SyncState
import java.time.Instant

@Entity(
    tableName = "checklist_items",
    foreignKeys = [
        ForeignKey(
            entity = JobEntity::class,
            parentColumns = ["id"],
            childColumns = ["jobId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("jobId"),
        Index(value = ["jobId", "position"], unique = true),
        Index("syncState")
    ]
)
data class ChecklistItemEntity(
    @PrimaryKey val id: String,
    val jobId: String,
    val label: String,
    val position: Int,
    val required: Boolean,
    val completed: Boolean,
    val completedAt: Instant?,
    val serverVersion: Long,
    val syncState: SyncState,
    val updatedAt: Instant
)
