package dev.amenokizele.tervyn.domain.model

import java.time.Instant

data class Note(
    val id: String,
    val jobId: String,
    val authorUserId: String,
    val content: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val syncState: SyncState,
    val serverVersion: Long?,
    val deletedAt: Instant?
)
