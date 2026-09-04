package dev.amenokizele.tervyn.data.local.converter

import androidx.room.TypeConverter
import dev.amenokizele.tervyn.data.local.entity.SyncEntityType
import dev.amenokizele.tervyn.data.local.entity.SyncOperationStatus
import dev.amenokizele.tervyn.data.local.entity.SyncOperationType
import dev.amenokizele.tervyn.domain.model.AttachmentType
import dev.amenokizele.tervyn.domain.model.JobPriority
import dev.amenokizele.tervyn.domain.model.JobStatus
import dev.amenokizele.tervyn.domain.model.SyncState
import java.time.Instant

class RoomConverters {
    @TypeConverter
    fun instantToEpochMillis(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun epochMillisToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun jobPriorityToString(value: JobPriority?): String? = value?.name

    @TypeConverter
    fun stringToJobPriority(value: String?): JobPriority? = value?.let(JobPriority::valueOf)

    @TypeConverter
    fun jobStatusToString(value: JobStatus?): String? = value?.name

    @TypeConverter
    fun stringToJobStatus(value: String?): JobStatus? = value?.let(JobStatus::valueOf)

    @TypeConverter
    fun syncStateToString(value: SyncState?): String? = value?.name

    @TypeConverter
    fun stringToSyncState(value: String?): SyncState? = value?.let(SyncState::valueOf)

    @TypeConverter
    fun attachmentTypeToString(value: AttachmentType?): String? = value?.name

    @TypeConverter
    fun stringToAttachmentType(value: String?): AttachmentType? = value?.let(AttachmentType::valueOf)

    @TypeConverter
    fun syncEntityTypeToString(value: SyncEntityType?): String? = value?.name

    @TypeConverter
    fun stringToSyncEntityType(value: String?): SyncEntityType? = value?.let(SyncEntityType::valueOf)

    @TypeConverter
    fun syncOperationTypeToString(value: SyncOperationType?): String? = value?.name

    @TypeConverter
    fun stringToSyncOperationType(value: String?): SyncOperationType? = value?.let(SyncOperationType::valueOf)

    @TypeConverter
    fun syncOperationStatusToString(value: SyncOperationStatus?): String? = value?.name

    @TypeConverter
    fun stringToSyncOperationStatus(value: String?): SyncOperationStatus? = value?.let(SyncOperationStatus::valueOf)
}
