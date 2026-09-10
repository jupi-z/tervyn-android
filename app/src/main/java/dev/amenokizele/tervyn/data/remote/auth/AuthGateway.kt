package dev.amenokizele.tervyn.data.remote.auth

import dev.amenokizele.tervyn.core.result.AppResult
import dev.amenokizele.tervyn.data.auth.session.StoredSession
import dev.amenokizele.tervyn.domain.model.User

interface AuthGateway {
    suspend fun login(email: String, password: String): AppResult<AuthGatewayResult>

    suspend fun revoke(session: StoredSession): AppResult<Unit>
}

data class AuthGatewayResult(
    val user: User,
    val session: StoredSession
)
