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

    @Query("SELECT * FROM sync_operations WHERE status = 'PENDING' ORDER BY createdAt ASC LIMIT :limit")
    suspend fun getNextPendingOperations(limit: Int): List<SyncOperationEntity>

    @Update
    suspend fun update(operation: SyncOperationEntity)

    @Delete
    suspend fun delete(operation: SyncOperationEntity)

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
