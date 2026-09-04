package dev.amenokizele.tervyn.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dev.amenokizele.tervyn.data.local.entity.ChecklistItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChecklistItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ChecklistItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<ChecklistItemEntity>)

    @Query("SELECT * FROM checklist_items WHERE id = :itemId")
    suspend fun getById(itemId: String): ChecklistItemEntity?

    @Query("SELECT * FROM checklist_items WHERE jobId = :jobId ORDER BY position ASC")
    fun observeByJobId(jobId: String): Flow<List<ChecklistItemEntity>>

    @Query("SELECT * FROM checklist_items WHERE jobId = :jobId ORDER BY position ASC")
    suspend fun getByJobId(jobId: String): List<ChecklistItemEntity>

    @Update
    suspend fun update(item: ChecklistItemEntity)
}
