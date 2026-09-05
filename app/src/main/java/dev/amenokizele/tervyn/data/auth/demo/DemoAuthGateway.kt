package dev.amenokizele.tervyn.data.auth.demo

import dev.amenokizele.tervyn.core.result.AppResult

interface DemoAuthGateway {
    suspend fun authenticate(email: String, password: String): AppResult<AuthenticatedDemoUser>

    data class AuthenticatedDemoUser(val userId: String)
}
