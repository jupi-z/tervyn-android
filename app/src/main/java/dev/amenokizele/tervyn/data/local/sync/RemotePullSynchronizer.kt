package dev.amenokizele.tervyn.data.local.sync

import androidx.room.withTransaction
import dev.amenokizele.tervyn.core.result.AppError
import dev.amenokizele.tervyn.core.result.AppResult
import dev.amenokizele.tervyn.core.time.TervynClock
import dev.amenokizele.tervyn.data.local.db.TervynDatabase
import dev.amenokizele.tervyn.data.local.entity.LocalMetadataEntity
import dev.amenokizele.tervyn.data.remote.job.RemoteJobDataSource
import dev.amenokizele.tervyn.data.remote.job.RemoteJobsPage
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

data class PullResult(
    val pages: Int,
    val error: SyncRunResult? = null
)

@Singleton
class RemotePullSynchronizer @Inject constructor(
    private val database: TervynDatabase,
    private val remote: RemoteJobDataSource,
    private val merger: RemoteJobMerger,
    private val clock: TervynClock
) {
    suspend fun pull(): PullResult {
        val metadata = database.localMetadataDao().get(LocalMetadataKeys.REMOTE_WATERMARK)
        val watermark = metadata?.value?.let {
            runCatching { Instant.parse(it) }.getOrElse {
                return PullResult(0, SyncRunResult.StorageFailure(AppError.Storage("invalid_remote_watermark")))
            }
        }
        var cursor: String? = null
        var pages = 0
        var stableServerTime: Instant? = null
        val seenCursors = mutableSetOf<String>()

        while (pages < MAX_PAGES) {
            val pageResult = remote.fetchJobs(cursor = cursor, limit = PAGE_SIZE, updatedAfter = watermark)
            val page = when (pageResult) {
                is AppResult.Success -> pageResult.data
                is AppResult.Failure -> return PullResult(pages, classify(pageResult.error))
            }
            if (stableServerTime == null) stableServerTime = page.serverTime
            if (stableServerTime != page.serverTime) {
                return PullResult(pages, SyncRunResult.StorageFailure(AppError.Storage("remote_server_time_changed")))
            }
            pages++
            merger.mergePage(page.items)
            val nextCursor = page.nextCursor
            if (nextCursor == null) {
                val finalWatermark = stableServerTime ?: clock.now()
                database.withTransaction {
                    database.localMetadataDao().upsert(
                        LocalMetadataEntity(
                            key = LocalMetadataKeys.REMOTE_WATERMARK,
                            value = finalWatermark.toString(),
                            updatedAt = clock.now()
                        )
                    )
                    database.localMetadataDao().upsert(
                        LocalMetadataEntity(
                            key = LocalMetadataKeys.LAST_SUCCESSFUL_SYNC_AT,
                            value = clock.now().toString(),
                            updatedAt = clock.now()
                        )
                    )
                }
                return PullResult(pages)
            }
            if (!seenCursors.add(nextCursor)) {
                return PullResult(pages, SyncRunResult.StorageFailure(AppError.Storage("remote_cursor_loop")))
            }
            cursor = nextCursor
        }
        return PullResult(pages, SyncRunResult.StorageFailure(AppError.Storage("remote_page_limit_exceeded")))
    }

    private fun classify(error: AppError): SyncRunResult = when (error) {
        is AppError.Authentication -> SyncRunResult.AuthenticationRequired
        is AppError.Network -> if (error.code in TRANSIENT_CODES) {
            SyncRunResult.TransientFailure(error)
        } else {
            SyncRunResult.StorageFailure(error)
        }
        is AppError.Storage -> SyncRunResult.StorageFailure(error)
        is AppError.Unknown -> SyncRunResult.TransientFailure(error)
        else -> SyncRunResult.StorageFailure(error)
    }

    private companion object {
        const val PAGE_SIZE = 50
        const val MAX_PAGES = 100
        val TRANSIENT_CODES = setOf("network_unavailable", "server_error", "rate_limited")
    }
}
