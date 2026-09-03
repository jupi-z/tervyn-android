package dev.amenokizele.tervyn.model

data class DemoChecklistItem(
    val id: String,
    val label: String,
    val position: Int,
    val required: Boolean,
    val completed: Boolean
)

data class DemoNote(
    val id: String,
    val content: String,
    val author: String = "Vous",
    val createdAt: String,
    val syncState: SyncState = SyncState.SYNCED
)

data class DemoPhoto(
    val id: String,
    val title: String,
    val placeholderTag: String,
    val createdAt: String,
    val syncState: SyncState = SyncState.SYNCED
)

data class DemoJob(
    val id: String,
    val reference: String,
    val title: String,
    val description: String,
    val clientName: String,
    val siteName: String,
    val siteAddress: String,
    val priority: JobPriority,
    val status: JobStatus,
    val scheduledAt: String,
    val startedAt: String? = null,
    val completedAt: String? = null,
    val syncState: SyncState = SyncState.SYNCED,
    val checklist: List<DemoChecklistItem> = emptyList(),
    val notes: List<DemoNote> = emptyList(),
    val photos: List<DemoPhoto> = emptyList()
) {
    val completedChecklistCount: Int
        get() = checklist.count { it.completed }

    val totalChecklistCount: Int
        get() = checklist.size

    val allRequiredCompleted: Boolean
        get() = checklist.filter { it.required }.all { it.completed }
}
