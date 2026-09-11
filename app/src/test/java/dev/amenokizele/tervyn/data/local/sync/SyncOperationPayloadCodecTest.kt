package dev.amenokizele.tervyn.data.local.sync

import dev.amenokizele.tervyn.core.result.AppResult
import dev.amenokizele.tervyn.data.local.entity.SyncEntityType
import dev.amenokizele.tervyn.data.local.entity.SyncOperationType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncOperationPayloadCodecTest {
    private val codec = SyncOperationPayloadCodec()

    @Test
    fun jobStatusPayloadRoundTripsWithStableType() {
        val payload = JobStatusPayload(
            jobId = "job-1",
            status = "IN_PROGRESS",
            changedAt = "2026-09-10T12:00:00Z"
        )

        val encoded = codec.encode(payload)
        val decoded = codec.decode(
            json = encoded,
            entityType = SyncEntityType.JOB,
            operation = SyncOperationType.UPDATE
        )

        assertTrue(encoded.contains("\"type\":\"job_status\""))
        assertEquals(AppResult.Success(payload), decoded)
    }

    @Test
    fun mismatchedPayloadIsRejectedAsStorageError() {
        val encoded = codec.encode(
            ChecklistItemPayload(
                jobId = "job-1",
                itemId = "item-1",
                completed = true,
                completedAt = "2026-09-10T12:00:00Z"
            )
        )

        val decoded = codec.decode(
            json = encoded,
            entityType = SyncEntityType.JOB,
            operation = SyncOperationType.UPDATE
        )

        assertEquals(AppResult.Failure(dev.amenokizele.tervyn.core.result.AppError.Storage("invalid_sync_payload")), decoded)
    }

    @Test
    fun legacyNullPayloadIsRejected() {
        val decoded = codec.decode(
            json = null,
            entityType = SyncEntityType.NOTE,
            operation = SyncOperationType.CREATE
        )

        assertEquals(AppResult.Failure(dev.amenokizele.tervyn.core.result.AppError.Storage("legacy_sync_payload_missing")), decoded)
    }
}
