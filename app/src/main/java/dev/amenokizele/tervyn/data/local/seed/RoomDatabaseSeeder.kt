package dev.amenokizele.tervyn.data.local.seed

import android.database.SQLException
import androidx.room.withTransaction
import dev.amenokizele.tervyn.app.LocalDataInitializer
import dev.amenokizele.tervyn.core.result.AppError
import dev.amenokizele.tervyn.core.result.AppResult
import dev.amenokizele.tervyn.core.time.TervynClock
import dev.amenokizele.tervyn.data.fixtures.TervynDemoFixtures
import dev.amenokizele.tervyn.data.local.db.TervynDatabase
import dev.amenokizele.tervyn.data.local.entity.LocalMetadataEntity
import dev.amenokizele.tervyn.data.local.mapper.toEntity
import dev.amenokizele.tervyn.domain.model.Attachment
import dev.amenokizele.tervyn.domain.model.ChecklistItem
import dev.amenokizele.tervyn.domain.model.Job
import dev.amenokizele.tervyn.domain.model.Note
import dev.amenokizele.tervyn.domain.model.SyncState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomDatabaseSeeder @Inject constructor(
    private val database: TervynDatabase,
    private val clock: TervynClock
) : LocalDataInitializer {
    override suspend fun initialize(): AppResult<Unit> = seedIfNeeded()

    suspend fun seedIfNeeded(): AppResult<Unit> {
        return try {
            database.withTransaction {
                val marker = database.localMetadataDao().get(SEED_VERSION_KEY)
                if (marker?.value == SEED_VERSION) {
                    return@withTransaction AppResult.Success(Unit)
                }

                val jobs = TervynDemoFixtures.initialJobs().map { it.asSeededSynced() }
                database.userDao().upsert(TervynDemoFixtures.currentUser.toEntity())
                database.jobDao().upsertJobs(jobs.map { it.toEntity() })
                database.checklistItemDao().upsertAll(jobs.flatMap { job -> job.checklist.map { it.toEntity() } })
                database.noteDao().upsertAll(jobs.flatMap { job -> job.notes.map { it.toEntity() } })
                database.attachmentDao().upsertAll(jobs.flatMap { job -> job.attachments.map { it.toEntity() } })
                database.localMetadataDao().upsert(
                    LocalMetadataEntity(
                        key = SEED_VERSION_KEY,
                        value = SEED_VERSION,
                        updatedAt = clock.now()
                    )
                )
                AppResult.Success(Unit)
            }
        } catch (_: SQLException) {
            AppResult.Failure(AppError.Storage("local_database_seed_failed"))
        }
    }

    private fun Job.asSeededSynced(): Job = copy(
        syncState = SyncState.SYNCED,
        checklist = checklist.map { it.asSeededSynced() },
        notes = notes.map { it.asSeededSynced() },
        attachments = attachments.map { it.asSeededSynced() }
    )

    private fun ChecklistItem.asSeededSynced(): ChecklistItem = copy(syncState = SyncState.SYNCED)

    private fun Note.asSeededSynced(): Note = copy(syncState = SyncState.SYNCED)

    private fun Attachment.asSeededSynced(): Attachment = copy(syncState = SyncState.SYNCED)

    companion object {
        const val SEED_VERSION_KEY = "demo_seed_version"
        const val SEED_VERSION = "1"
    }
}
