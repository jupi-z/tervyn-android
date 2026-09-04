package dev.amenokizele.tervyn.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "local_metadata")
data class LocalMetadataEntity(
    @PrimaryKey val key: String,
    val value: String,
    val updatedAt: Instant
)
