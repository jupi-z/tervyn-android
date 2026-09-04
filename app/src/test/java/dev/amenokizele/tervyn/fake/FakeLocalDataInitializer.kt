package dev.amenokizele.tervyn.fake

import dev.amenokizele.tervyn.app.LocalDataInitializer
import dev.amenokizele.tervyn.core.result.AppResult
import kotlinx.coroutines.CompletableDeferred

class FakeLocalDataInitializer : LocalDataInitializer {
    private val completion = CompletableDeferred<Unit>()

    override suspend fun initialize(): AppResult<Unit> {
        completion.await()
        return AppResult.Success(Unit)
    }

    fun complete() {
        completion.complete(Unit)
    }
}
