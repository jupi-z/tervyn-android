package dev.amenokizele.tervyn.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import dev.amenokizele.tervyn.data.local.entity.AttachmentEntity
import dev.amenokizele.tervyn.data.local.entity.ChecklistItemEntity
import dev.amenokizele.tervyn.data.local.entity.JobEntity
import dev.amenokizele.tervyn.data.local.entity.NoteEntity

data class JobWithDetails(
    @Embedded val job: JobEntity,
    @Relation(parentColumn = "id", entityColumn = "jobId")
    val checklist: List<ChecklistItemEntity>,
    @Relation(parentColumn = "id", entityColumn = "jobId")
    val notes: List<NoteEntity>,
    @Relation(parentColumn = "id", entityColumn = "jobId")
    val attachments: List<AttachmentEntity>
)
