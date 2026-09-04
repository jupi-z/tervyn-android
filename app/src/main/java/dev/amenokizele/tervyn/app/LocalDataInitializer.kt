package dev.amenokizele.tervyn.app

import dev.amenokizele.tervyn.core.result.AppResult

interface LocalDataInitializer {
    suspend fun initialize(): AppResult<Unit>
}
