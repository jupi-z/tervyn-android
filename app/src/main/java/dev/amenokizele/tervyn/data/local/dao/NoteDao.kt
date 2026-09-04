package dev.amenokizele.tervyn.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dev.amenokizele.tervyn.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(note: NoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(notes: List<NoteEntity>)

    @Query("SELECT * FROM notes WHERE id = :noteId")
    suspend fun getById(noteId: String): NoteEntity?

    @Query("SELECT * FROM notes WHERE jobId = :jobId AND deletedAt IS NULL ORDER BY createdAt ASC")
    fun observeByJobId(jobId: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE jobId = :jobId AND deletedAt IS NULL ORDER BY createdAt ASC")
    suspend fun getByJobId(jobId: String): List<NoteEntity>

    @Update
    suspend fun update(note: NoteEntity)
}
