package dev.amenokizele.tervyn.domain.model

import java.time.Instant

data class ChecklistItem(
    val id: String,
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
