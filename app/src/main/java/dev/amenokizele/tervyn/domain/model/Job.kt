package dev.amenokizele.tervyn.domain.model

import java.time.Instant

data class Job(
    val id: String,
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
    val lastSyncedAt: Instant?,
    val checklist: List<ChecklistItem> = emptyList(),
    val notes: List<Note> = emptyList(),
    val attachments: List<Attachment> = emptyList()
) {
    val completedChecklistCount: Int
        get() = checklist.count { it.completed }

    val totalChecklistCount: Int
        get() = checklist.size

    val allRequiredCompleted: Boolean
        get() = checklist.filter { it.required }.all { it.completed }
}
