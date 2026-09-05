package dev.amenokizele.tervyn.fake

import dev.amenokizele.tervyn.app.LocalDataInitializer
import dev.amenokizele.tervyn.core.result.AppResult
import kotlinx.coroutines.CompletableDeferred

class FakeLocalDataInitializer(
    initialResult: AppResult<Unit> = AppResult.Success(Unit)
) : LocalDataInitializer {
    private val results = ArrayDeque<AppResult<Unit>>().apply {
        add(initialResult)
    }
    private var completion: CompletableDeferred<Unit>? = CompletableDeferred()

    var invocationCount: Int = 0
        private set

    override suspend fun initialize(): AppResult<Unit> {
        invocationCount += 1
        completion?.await()
        return results.removeFirstOrNull() ?: AppResult.Success(Unit)
    }

    fun complete() {
        completion?.complete(Unit)
    }

    fun enqueueResult(result: AppResult<Unit>) {
        results.add(result)
    }

    fun completeImmediately() {
        completion = null
    }
}
