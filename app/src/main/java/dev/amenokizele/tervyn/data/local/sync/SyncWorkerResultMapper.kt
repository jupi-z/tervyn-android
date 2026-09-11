package dev.amenokizele.tervyn.data.local.sync

import androidx.work.ListenableWorker

object SyncWorkerResultMapper {
    fun map(result: SyncRunResult): ListenableWorker.Result = when (result) {
        is SyncRunResult.Success,
        is SyncRunResult.PartialSuccess,
        SyncRunResult.RemoteDisabled,
        SyncRunResult.AuthenticationRequired -> ListenableWorker.Result.success()
        SyncRunResult.AlreadyRunning -> ListenableWorker.Result.retry()
        is SyncRunResult.TransientFailure -> ListenableWorker.Result.retry()
        is SyncRunResult.StorageFailure -> ListenableWorker.Result.failure()
    }
}
