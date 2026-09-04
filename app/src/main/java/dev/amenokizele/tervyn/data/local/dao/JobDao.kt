package dev.amenokizele.tervyn.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import dev.amenokizele.tervyn.data.local.entity.JobEntity
import dev.amenokizele.tervyn.data.local.relation.JobWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface JobDao {
    @Transaction
    @Query("SELECT * FROM jobs ORDER BY scheduledAt ASC")
    fun observeJobsWithDetails(): Flow<List<JobWithDetails>>

    @Transaction
    @Query("SELECT * FROM jobs WHERE id = :jobId")
    fun observeJobWithDetails(jobId: String): Flow<JobWithDetails?>

    @Transaction
    @Query("SELECT * FROM jobs WHERE id = :jobId")
    suspend fun getJobWithDetails(jobId: String): JobWithDetails?

    @Query("SELECT * FROM jobs WHERE id = :jobId")
    suspend fun getJobById(jobId: String): JobEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertJob(job: JobEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertJobs(jobs: List<JobEntity>)

    @Update
    suspend fun updateJob(job: JobEntity)

    @Query("SELECT COUNT(*) FROM jobs")
    suspend fun countJobs(): Int

    @Query("DELETE FROM jobs WHERE id = :jobId")
    suspend fun deleteJobById(jobId: String)
}
