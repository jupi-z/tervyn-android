package dev.amenokizele.tervyn.domain.repository

import dev.amenokizele.tervyn.core.result.AppResult
import dev.amenokizele.tervyn.domain.model.AuthState
import dev.amenokizele.tervyn.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val authState: Flow<AuthState>

    suspend fun login(email: String, password: String): AppResult<User>

    suspend fun logout(): AppResult<Unit>
}
