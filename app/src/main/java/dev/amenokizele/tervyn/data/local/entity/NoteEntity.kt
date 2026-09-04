package dev.amenokizele.tervyn.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.amenokizele.tervyn.domain.model.SyncState
import java.time.Instant

@Entity(
    tableName = "notes",
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
        Index("authorUserId"),
        Index("syncState"),
        Index("createdAt")
    ]
)
data class NoteEntity(
    @PrimaryKey val id: String,
    val jobId: String,
    val authorUserId: String,
    val content: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val syncState: SyncState,
    val serverVersion: Long?,
    val deletedAt: Instant?
)
