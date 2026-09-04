package dev.amenokizele.tervyn.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import dev.amenokizele.tervyn.data.local.converter.RoomConverters
import dev.amenokizele.tervyn.data.local.dao.AttachmentDao
import dev.amenokizele.tervyn.data.local.dao.ChecklistItemDao
import dev.amenokizele.tervyn.data.local.dao.JobDao
import dev.amenokizele.tervyn.data.local.dao.LocalMetadataDao
import dev.amenokizele.tervyn.data.local.dao.NoteDao
import dev.amenokizele.tervyn.data.local.dao.SyncOperationDao
import dev.amenokizele.tervyn.data.local.dao.UserDao
import dev.amenokizele.tervyn.data.local.entity.AttachmentEntity
import dev.amenokizele.tervyn.data.local.entity.ChecklistItemEntity
import dev.amenokizele.tervyn.data.local.entity.JobEntity
import dev.amenokizele.tervyn.data.local.entity.LocalMetadataEntity
import dev.amenokizele.tervyn.data.local.entity.NoteEntity
import dev.amenokizele.tervyn.data.local.entity.SyncOperationEntity
import dev.amenokizele.tervyn.data.local.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        JobEntity::class,
        ChecklistItemEntity::class,
        NoteEntity::class,
        AttachmentEntity::class,
        SyncOperationEntity::class,
        LocalMetadataEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(RoomConverters::class)
abstract class TervynDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun jobDao(): JobDao
    abstract fun checklistItemDao(): ChecklistItemDao
    abstract fun noteDao(): NoteDao
    abstract fun attachmentDao(): AttachmentDao
    abstract fun syncOperationDao(): SyncOperationDao
    abstract fun localMetadataDao(): LocalMetadataDao

    companion object {
        const val DATABASE_NAME = "tervyn.db"
    }
}
