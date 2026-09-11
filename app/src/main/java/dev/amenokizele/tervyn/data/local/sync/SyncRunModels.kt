package dev.amenokizele.tervyn.data.local.sync

import dev.amenokizele.tervyn.core.result.AppError
import java.time.Duration
import java.time.Instant

enum class SyncRunTrigger {
    MANUAL,
    BACKGROUND
}

sealed interface SyncRunResult {
    data class Success(val processedOperations: Int, val pulledPages: Int) : SyncRunResult
    data class PartialSuccess(
        val processedOperations: Int,
        val pulledPages: Int,
        val failedOperations: Int
    ) : SyncRunResult

    data object RemoteDisabled : SyncRunResult
    data object AuthenticationRequired : SyncRunResult
    data object AlreadyRunning : SyncRunResult
    data class TransientFailure(val error: AppError) : SyncRunResult
    data class StorageFailure(val error: AppError) : SyncRunResult
}

enum class SyncRuntimePhase {
    IDLE,
    SYNCING,
    FAILED
}

data class SyncRuntimeState(
    val phase: SyncRuntimePhase = SyncRuntimePhase.IDLE,
    val completedOperations: Int = 0,
    val totalOperations: Int = 0,
    val errorCode: String? = null
)

class SyncRuntimeStateStore {
    private val mutableState = kotlinx.coroutines.flow.MutableStateFlow(SyncRuntimeState())
    val state: kotlinx.coroutines.flow.StateFlow<SyncRuntimeState> = mutableState

    fun syncing(totalOperations: Int) {
        mutableState.value = SyncRuntimeState(
            phase = SyncRuntimePhase.SYNCING,
            totalOperations = totalOperations
        )
    }

    fun progress(completedOperations: Int) {
        mutableState.value = mutableState.value.copy(completedOperations = completedOperations)
    }

    fun failed(errorCode: String?) {
        mutableState.value = mutableState.value.copy(
            phase = SyncRuntimePhase.FAILED,
            errorCode = errorCode
        )
    }

    fun idle() {
        mutableState.value = SyncRuntimeState()
    }
}

class SyncBackoffPolicy(
    private val baseDelay: Duration = Duration.ofSeconds(30),
    private val maxDelay: Duration = Duration.ofHours(1)
) {
    fun nextAttemptAt(now: Instant, attemptCount: Int): Instant {
        require(attemptCount > 0)
        val multiplier = 1L shl minOf(attemptCount - 1, 20)
        val delay = baseDelay.multipliedBy(multiplier).coerceAtMost(maxDelay)
        return now.plus(delay)
    }

    private fun Duration.coerceAtMost(maximum: Duration): Duration =
        if (this > maximum) maximum else this
}
