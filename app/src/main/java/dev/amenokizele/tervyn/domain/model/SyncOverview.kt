package dev.amenokizele.tervyn.domain.model

import java.time.Instant

data class SyncOverview(
    val state: SyncState,
    val pendingCount: Int,
    val failedCount: Int,
    val lastSuccessfulSyncAt: Instant?,
    val isOnline: Boolean,
    val completedOperations: Int,
    val totalOperations: Int
)
