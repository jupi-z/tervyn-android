package dev.amenokizele.tervyn.data.remote.auth

import dev.amenokizele.tervyn.core.result.AppResult
import dev.amenokizele.tervyn.data.auth.session.StoredSession
import dev.amenokizele.tervyn.data.remote.config.RemoteApiConfig
import javax.inject.Inject

class ConfiguredAuthGateway @Inject constructor(
    private val config: RemoteApiConfig,
    private val localGateway: LocalAuthGatewayAdapter,
    private val remoteGateway: RemoteAuthGateway
) : AuthGateway {
    override suspend fun login(email: String, password: String): AppResult<AuthGatewayResult> {
        return selected().login(email, password)
    }

    override suspend fun revoke(session: StoredSession): AppResult<Unit> {
        return if (config.enabled) remoteGateway.revoke(session) else AppResult.Success(Unit)
    }

    private fun selected(): AuthGateway = if (config.enabled) remoteGateway else localGateway
}
