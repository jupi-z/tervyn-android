package dev.amenokizele.tervyn.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "users",
    indices = [
        Index("email")
    ]
)
data class UserEntity(
    @PrimaryKey val id: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val jobTitle: String?,
    val avatarUrl: String?,
    val createdAt: Instant?,
    val updatedAt: Instant?,
    val lastSyncedAt: Instant?
)
