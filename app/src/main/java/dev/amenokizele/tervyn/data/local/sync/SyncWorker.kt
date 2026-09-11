package dev.amenokizele.tervyn.data.local.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val orchestrator: SyncOrchestrator
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result = SyncWorkerResultMapper.map(
        orchestrator.run(SyncRunTrigger.BACKGROUND)
    )
}
