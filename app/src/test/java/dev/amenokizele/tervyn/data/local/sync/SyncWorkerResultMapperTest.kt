package dev.amenokizele.tervyn.data.local.sync

import dev.amenokizele.tervyn.core.result.AppError
import org.junit.Assert.assertEquals
import org.junit.Test

class SyncWorkerResultMapperTest {
    @Test
    fun transientFailureRequestsRetry() {
        assertEquals(
            androidx.work.ListenableWorker.Result.retry(),
            SyncWorkerResultMapper.map(
                SyncRunResult.TransientFailure(AppError.Network("network_unavailable"))
            )
        )
    }

    @Test
    fun storageFailureStopsWithoutRetry() {
        assertEquals(
            androidx.work.ListenableWorker.Result.failure(),
            SyncWorkerResultMapper.map(
                SyncRunResult.StorageFailure(AppError.Storage("db_failure"))
            )
        )
    }

    @Test
    fun remoteDisabledIsSuccessfulNoOp() {
        assertEquals(
            androidx.work.ListenableWorker.Result.success(),
            SyncWorkerResultMapper.map(SyncRunResult.RemoteDisabled)
        )
    }
}
