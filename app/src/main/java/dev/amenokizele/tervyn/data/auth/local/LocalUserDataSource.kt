package dev.amenokizele.tervyn.data.auth.local

import dev.amenokizele.tervyn.core.result.AppResult
import dev.amenokizele.tervyn.domain.model.User

interface LocalUserDataSource {
    suspend fun getUser(userId: String): User?

    suspend fun upsertUser(user: User): AppResult<Unit>
}
