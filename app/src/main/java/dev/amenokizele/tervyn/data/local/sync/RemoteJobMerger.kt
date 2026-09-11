package dev.amenokizele.tervyn.data.local.sync

import androidx.room.withTransaction
import dev.amenokizele.tervyn.core.time.TervynClock
import dev.amenokizele.tervyn.data.local.db.TervynDatabase
import dev.amenokizele.tervyn.data.local.entity.AttachmentEntity
import dev.amenokizele.tervyn.data.local.entity.ChecklistItemEntity
import dev.amenokizele.tervyn.data.local.entity.JobEntity
import dev.amenokizele.tervyn.data.local.entity.NoteEntity
import dev.amenokizele.tervyn.data.remote.job.RemoteAttachmentSnapshot
import dev.amenokizele.tervyn.data.remote.job.RemoteChecklistItemSnapshot
import dev.amenokizele.tervyn.data.remote.job.RemoteJobSnapshot
import dev.amenokizele.tervyn.data.remote.job.RemoteNoteSnapshot
import dev.amenokizele.tervyn.domain.model.SyncState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteJobMerger @Inject constructor(
    private val database: TervynDatabase,
    private val clock: TervynClock
) {
    suspend fun merge(snapshot: RemoteJobSnapshot) {
        mergePage(listOf(snapshot))
    }

    suspend fun mergePage(snapshots: List<RemoteJobSnapshot>) {
        database.withTransaction {
            snapshots.forEach { snapshot ->
                mergeSnapshot(snapshot)
            }
        }
    }

    private suspend fun mergeSnapshot(snapshot: RemoteJobSnapshot) {
        val jobDao = database.jobDao()
        val checklistDao = database.checklistItemDao()
        val noteDao = database.noteDao()
        val attachmentDao = database.attachmentDao()
        val existingJob = jobDao.getJobById(snapshot.id)
        val jobHasLocalOperations = database.syncOperationDao().countForEntity(
            dev.amenokizele.tervyn.data.local.entity.SyncEntityType.JOB,
            snapshot.id
        ) > 0
        jobDao.updateOrInsert(snapshot.toEntity(existingJob, jobHasLocalOperations))

        snapshot.checklist.forEach { item ->
            val existing = checklistDao.getById(item.id)
            val hasLocalOperations = database.syncOperationDao().countForEntity(
                dev.amenokizele.tervyn.data.local.entity.SyncEntityType.CHECKLIST_ITEM,
                item.id
            ) > 0
            checklistDao.upsert(item.toEntity(existing, hasLocalOperations))
        }
        snapshot.notes.forEach { note ->
            val existing = noteDao.getById(note.id)
            val hasLocalOperations = database.syncOperationDao().countForEntity(
                dev.amenokizele.tervyn.data.local.entity.SyncEntityType.NOTE,
                note.id
            ) > 0
            noteDao.upsert(note.toEntity(existing, hasLocalOperations))
        }
        snapshot.attachments.forEach { attachment ->
            val existing = attachmentDao.getById(attachment.id)
            val hasLocalOperations = database.syncOperationDao().countForEntity(
                dev.amenokizele.tervyn.data.local.entity.SyncEntityType.ATTACHMENT,
                attachment.id
            ) > 0
            attachmentDao.upsert(attachment.toEntity(existing, hasLocalOperations))
        }
    }

    private suspend fun dev.amenokizele.tervyn.data.local.dao.JobDao.updateOrInsert(entity: JobEntity) {
        if (getJobById(entity.id) == null) insertJob(entity) else updateJob(entity)
    }

    private fun RemoteJobSnapshot.toEntity(
        existing: JobEntity?,
        preserveLocal: Boolean
    ): JobEntity = JobEntity(
        id = id,
        reference = reference,
        title = title,
        description = description,
        clientName = clientName,
        siteName = siteName,
        siteAddress = siteAddress,
        priority = priority,
        status = if (preserveLocal) existing!!.status else status,
        scheduledAt = scheduledAt,
        startedAt = if (preserveLocal) existing!!.startedAt else startedAt,
        completedAt = if (preserveLocal) existing!!.completedAt else completedAt,
        serverVersion = if (preserveLocal) existing!!.serverVersion else serverVersion,
        syncState = if (preserveLocal) existing!!.syncState else SyncState.SYNCED,
        createdAt = createdAt,
        updatedAt = if (preserveLocal) existing!!.updatedAt else updatedAt,
        lastSyncedAt = if (preserveLocal) existing!!.lastSyncedAt else clock.now()
    )

    private fun RemoteChecklistItemSnapshot.toEntity(
        existing: ChecklistItemEntity?,
        preserveLocal: Boolean
    ): ChecklistItemEntity = ChecklistItemEntity(
        id = id,
        jobId = jobId,
        label = label,
        position = position,
        required = required,
        completed = if (preserveLocal) existing!!.completed else completed,
        completedAt = if (preserveLocal) existing!!.completedAt else completedAt,
        serverVersion = if (preserveLocal) existing!!.serverVersion else serverVersion,
        syncState = if (preserveLocal) existing!!.syncState else SyncState.SYNCED,
        updatedAt = if (preserveLocal) existing!!.updatedAt else updatedAt
    )

    private fun RemoteNoteSnapshot.toEntity(
        existing: NoteEntity?,
        preserveLocal: Boolean
    ): NoteEntity = NoteEntity(
        id = id,
        jobId = jobId,
        authorUserId = authorUserId,
        content = if (preserveLocal) existing!!.content else content,
        createdAt = if (preserveLocal) existing!!.createdAt else createdAt,
        updatedAt = if (preserveLocal) existing!!.updatedAt else updatedAt,
        syncState = if (preserveLocal) existing!!.syncState else SyncState.SYNCED,
        serverVersion = if (preserveLocal) existing!!.serverVersion else serverVersion,
        deletedAt = if (preserveLocal) existing!!.deletedAt else deletedAt
    )

    private fun RemoteAttachmentSnapshot.toEntity(
        existing: AttachmentEntity?,
        preserveLocal: Boolean
    ): AttachmentEntity = AttachmentEntity(
        id = id,
        jobId = jobId,
        authorUserId = authorUserId,
        type = type,
        localUri = existing?.localUri,
        remoteUrl = remoteUrl,
        mimeType = mimeType,
        fileName = fileName,
        sizeBytes = sizeBytes,
        checksumSha256 = checksumSha256,
        syncState = if (preserveLocal) existing!!.syncState else SyncState.SYNCED,
        createdAt = createdAt,
        uploadedAt = uploadedAt,
        deletedAt = if (preserveLocal) existing!!.deletedAt else deletedAt,
        serverVersion = serverVersion
    )
}
