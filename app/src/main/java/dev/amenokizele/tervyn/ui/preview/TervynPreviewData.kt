package dev.amenokizele.tervyn.ui.preview

import dev.amenokizele.tervyn.domain.model.Attachment
import dev.amenokizele.tervyn.domain.model.AttachmentType
import dev.amenokizele.tervyn.domain.model.ChecklistItem
import dev.amenokizele.tervyn.domain.model.Job
import dev.amenokizele.tervyn.domain.model.JobPriority
import dev.amenokizele.tervyn.domain.model.JobStatus
import dev.amenokizele.tervyn.domain.model.Note
import dev.amenokizele.tervyn.domain.model.SyncState
import java.time.Instant

object TervynPreviewData {
    private val scheduled = Instant.parse("2026-09-03T11:00:00Z")

    val jobs: List<Job> = listOf(
        job("job-preview-1", JobStatus.ASSIGNED, JobPriority.HIGH),
        job("job-preview-2", JobStatus.IN_PROGRESS, JobPriority.NORMAL),
        job("job-preview-3", JobStatus.COMPLETED, JobPriority.LOW),
        job("job-preview-4", JobStatus.ASSIGNED, JobPriority.URGENT)
    )

    val inProgressJob: Job = jobs[1]

    private fun job(id: String, status: JobStatus, priority: JobPriority): Job {
        val checklist = listOf(
            checklistItem(id, "preview-check-1", "Relevé températures entrée / sortie", 1, true, true),
            checklistItem(id, "preview-check-2", "Inspection de l'évacuation des condensats", 2, true, true),
            checklistItem(id, "preview-check-3", "Remplacement des filtres anti-poussière", 3, true, status == JobStatus.COMPLETED)
        )
        return Job(
            id = id,
            reference = "JOB-2026-00184",
            title = "Maintenance climatisation",
            description = "Nettoyage des échangeurs thermiques et recharge fluide frigorigène salle serveurs.",
            clientName = "Kivu Services",
            siteName = "Datacenter Est",
            siteAddress = "88 Boulevard de la Libération, Lubumbashi",
            priority = priority,
            status = status,
            scheduledAt = scheduled,
            startedAt = if (status != JobStatus.ASSIGNED) scheduled.plusSeconds(300) else null,
            completedAt = if (status == JobStatus.COMPLETED) scheduled.plusSeconds(3300) else null,
            serverVersion = 1,
            syncState = if (status == JobStatus.IN_PROGRESS) SyncState.PENDING else SyncState.SYNCED,
            createdAt = scheduled.minusSeconds(86_400),
            updatedAt = scheduled,
            lastSyncedAt = scheduled.minusSeconds(3600),
            checklist = checklist,
            notes = listOf(
                Note(
                    id = "preview-note-1",
                    jobId = id,
                    authorUserId = "user-amina",
                    content = "Filtre primaire fortement encrassé par les vents de sable.",
                    createdAt = scheduled.plusSeconds(900),
                    updatedAt = scheduled.plusSeconds(900),
                    syncState = SyncState.PENDING,
                    serverVersion = null,
                    deletedAt = null
                )
            ),
            attachments = listOf(
                Attachment(
                    id = "preview-photo-1",
                    jobId = id,
                    authorUserId = "user-amina",
                    type = AttachmentType.PHOTO,
                    localUri = "tervyn://preview/photo/FILTER",
                    remoteUrl = null,
                    mimeType = "image/jpeg",
                    fileName = "Filtre salle serveurs",
                    sizeBytes = 0,
                    checksumSha256 = null,
                    syncState = SyncState.PENDING,
                    createdAt = scheduled.plusSeconds(1080),
                    uploadedAt = null,
                    deletedAt = null
                )
            )
        )
    }

    private fun checklistItem(
        jobId: String,
        id: String,
        label: String,
        position: Int,
        required: Boolean,
        completed: Boolean
    ) = ChecklistItem(
        id = id,
        jobId = jobId,
        label = label,
        position = position,
        required = required,
        completed = completed,
        completedAt = if (completed) scheduled.plusSeconds(600) else null,
        serverVersion = 1,
        syncState = SyncState.SYNCED,
        updatedAt = scheduled
    )
}
