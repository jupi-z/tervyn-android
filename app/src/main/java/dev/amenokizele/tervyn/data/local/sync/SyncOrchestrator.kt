package dev.amenokizele.tervyn.data.local.sync

import dev.amenokizele.tervyn.data.local.db.TervynDatabase
import dev.amenokizele.tervyn.data.remote.auth.SessionCoordinator
import dev.amenokizele.tervyn.data.remote.config.RemoteApiConfig
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex

@Singleton
class SyncOrchestrator @Inject constructor(
    private val database: TervynDatabase,
    private val config: RemoteApiConfig,
    private val sessionCoordinator: SessionCoordinator,
    private val outboxExecutor: OutboxExecutor,
    private val pullSynchronizer: RemotePullSynchronizer,
    val runtimeState: SyncRuntimeStateStore
) {
    private val runMutex = Mutex()

    suspend fun run(trigger: SyncRunTrigger): SyncRunResult {
        if (!config.enabled) return SyncRunResult.RemoteDisabled
        if (!runMutex.tryLock()) return SyncRunResult.AlreadyRunning
        try {
            if (sessionCoordinator.snapshot() == null) return SyncRunResult.AuthenticationRequired
            runtimeState.syncing(database.syncOperationDao().countOutstanding())
            val push = outboxExecutor.execute(trigger)
            runtimeState.progress(push.processedOperations)
            push.stopResult?.let {
                runtimeState.failed(errorCode(it))
                return it
            }
            val pull = pullSynchronizer.pull()
            pull.error?.let {
                runtimeState.failed(errorCode(it))
                return it
            }
            val result = if (push.failedOperations == 0) {
                SyncRunResult.Success(push.processedOperations, pull.pages)
            } else {
                SyncRunResult.PartialSuccess(push.processedOperations, pull.pages, push.failedOperations)
            }
            runtimeState.idle()
            return result
        } finally {
            runMutex.unlock()
        }
    }

    private fun errorCode(result: SyncRunResult): String? = when (result) {
        is SyncRunResult.TransientFailure -> result.error.code()
        is SyncRunResult.StorageFailure -> result.error.code()
        else -> null
    }

    private fun dev.amenokizele.tervyn.core.result.AppError.code(): String = when (this) {
        is dev.amenokizele.tervyn.core.result.AppError.NotFound -> "not_found"
        is dev.amenokizele.tervyn.core.result.AppError.Validation -> code
        is dev.amenokizele.tervyn.core.result.AppError.InvalidState -> code
        is dev.amenokizele.tervyn.core.result.AppError.Authentication -> code
        is dev.amenokizele.tervyn.core.result.AppError.Network -> code
        is dev.amenokizele.tervyn.core.result.AppError.Conflict -> code
        is dev.amenokizele.tervyn.core.result.AppError.Storage -> code
        is dev.amenokizele.tervyn.core.result.AppError.Unknown -> code ?: "unknown"
    }
}
