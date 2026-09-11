package dev.amenokizele.tervyn.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.amenokizele.tervyn.domain.model.AttachmentType
import dev.amenokizele.tervyn.domain.model.SyncState
import java.time.Instant

@Entity(
    tableName = "attachments",
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
data class AttachmentEntity(
    @PrimaryKey val id: String,
    val jobId: String,
    val authorUserId: String,
    val type: AttachmentType,
    val localUri: String?,
    val remoteUrl: String?,
    val mimeType: String,
    val fileName: String?,
    val sizeBytes: Long,
    val checksumSha256: String?,
    val syncState: SyncState,
    val createdAt: Instant,
    val uploadedAt: Instant?,
    val deletedAt: Instant?,
    val serverVersion: Long? = null
)
