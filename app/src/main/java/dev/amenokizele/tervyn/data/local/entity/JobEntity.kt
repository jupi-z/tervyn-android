package dev.amenokizele.tervyn.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.amenokizele.tervyn.domain.model.JobPriority
import dev.amenokizele.tervyn.domain.model.JobStatus
import dev.amenokizele.tervyn.domain.model.SyncState
import java.time.Instant

@Entity(
    tableName = "jobs",
    indices = [
        Index(value = ["reference"], unique = true),
        Index("status"),
        Index("scheduledAt"),
        Index("syncState")
    ]
)
data class JobEntity(
    @PrimaryKey val id: String,
    val reference: String,
    val title: String,
    val description: String?,
    val clientName: String,
    val siteName: String,
    val siteAddress: String?,
    val priority: JobPriority,
    val status: JobStatus,
    val scheduledAt: Instant,
    val startedAt: Instant?,
    val completedAt: Instant?,
    val serverVersion: Long,
    val syncState: SyncState,
    val createdAt: Instant,
    val updatedAt: Instant,
    val lastSyncedAt: Instant?
)
