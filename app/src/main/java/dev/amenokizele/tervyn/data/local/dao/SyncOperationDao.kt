package dev.amenokizele.tervyn.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import dev.amenokizele.tervyn.data.local.entity.SyncOperationEntity
import dev.amenokizele.tervyn.data.local.entity.SyncOperationStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncOperationDao {
    @Insert
    suspend fun insert(operation: SyncOperationEntity)

    @Query("SELECT COUNT(*) FROM sync_operations WHERE status = 'PENDING'")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM sync_operations WHERE status = 'FAILED'")
    fun observeFailedCount(): Flow<Int>

    @Query("SELECT * FROM sync_operations ORDER BY createdAt ASC")
    fun observeAllOrdered(): Flow<List<SyncOperationEntity>>

    @Query(
        "SELECT * FROM sync_operations " +
            "WHERE status = 'PENDING' AND operation != 'UPLOAD' " +
            "AND (nextAttemptAt IS NULL OR nextAttemptAt <= :now) " +
            "ORDER BY createdAt ASC LIMIT :limit"
    )
    suspend fun getDueOperations(now: java.time.Instant, limit: Int): List<SyncOperationEntity>

    @Query(
        "SELECT * FROM sync_operations " +
            "WHERE status = 'PENDING' AND operation != 'UPLOAD' " +
            "ORDER BY createdAt ASC LIMIT :limit"
    )
    suspend fun getManualOperations(limit: Int): List<SyncOperationEntity>

    @Query("SELECT * FROM sync_operations WHERE id = :id")
    suspend fun getById(id: String): SyncOperationEntity?

    @Query("SELECT * FROM sync_operations WHERE status = 'PROCESSING' AND lastAttemptAt <= :cutoff")
    suspend fun getStaleProcessing(cutoff: java.time.Instant): List<SyncOperationEntity>

    @Query("SELECT COUNT(*) FROM sync_operations WHERE entityType = :entityType AND entityId = :entityId")
    suspend fun countForEntity(entityType: dev.amenokizele.tervyn.data.local.entity.SyncEntityType, entityId: String): Int

    @Query("DELETE FROM sync_operations WHERE entityType = 'ATTACHMENT' AND entityId = :attachmentId AND operation = 'UPLOAD' AND status != 'PROCESSING'")
    suspend fun deletePendingUploadForAttachment(attachmentId: String)

    @Query("SELECT COUNT(*) FROM sync_operations WHERE status = 'PROCESSING'")
    suspend fun countProcessing(): Int

    @Query("SELECT COUNT(*) FROM sync_operations WHERE status IN ('PENDING', 'PROCESSING', 'FAILED')")
    suspend fun countOutstanding(): Int

    @Update
    suspend fun update(operation: SyncOperationEntity)

    @Delete
    suspend fun delete(operation: SyncOperationEntity)

    @Query("DELETE FROM sync_operations WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE sync_operations SET status = :status, attemptCount = :attemptCount, lastErrorCode = :lastErrorCode, lastErrorMessage = :lastErrorMessage, lastAttemptAt = :lastAttemptAt, nextAttemptAt = :nextAttemptAt WHERE id = :id")
    suspend fun updateStatus(
        id: String,
        status: SyncOperationStatus,
        attemptCount: Int,
        lastErrorCode: String?,
        lastErrorMessage: String?,
        lastAttemptAt: java.time.Instant?,
        nextAttemptAt: java.time.Instant?
    )
}
