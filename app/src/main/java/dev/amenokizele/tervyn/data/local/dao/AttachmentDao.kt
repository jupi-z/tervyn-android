package dev.amenokizele.tervyn.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dev.amenokizele.tervyn.data.local.entity.AttachmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttachmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(attachment: AttachmentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(attachments: List<AttachmentEntity>)

    @Query("SELECT * FROM attachments WHERE id = :attachmentId")
    suspend fun getById(attachmentId: String): AttachmentEntity?

    @Query("SELECT * FROM attachments WHERE jobId = :jobId AND deletedAt IS NULL ORDER BY createdAt ASC")
    fun observeVisibleByJobId(jobId: String): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM attachments WHERE jobId = :jobId AND deletedAt IS NULL ORDER BY createdAt ASC")
    suspend fun getVisibleByJobId(jobId: String): List<AttachmentEntity>

    @Query("SELECT * FROM attachments WHERE jobId = :jobId ORDER BY createdAt ASC")
    suspend fun getByJobIdIncludingDeleted(jobId: String): List<AttachmentEntity>

    @Update
    suspend fun update(attachment: AttachmentEntity)
}
