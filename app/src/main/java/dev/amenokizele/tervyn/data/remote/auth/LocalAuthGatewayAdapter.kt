package dev.amenokizele.tervyn.data.remote.auth

import dev.amenokizele.tervyn.core.result.AppError
import dev.amenokizele.tervyn.core.result.AppResult
import dev.amenokizele.tervyn.core.time.TervynClock
import dev.amenokizele.tervyn.data.auth.demo.DemoAuthGateway
import dev.amenokizele.tervyn.data.auth.local.LocalUserDataSource
import dev.amenokizele.tervyn.data.auth.session.StoredSession
import dev.amenokizele.tervyn.di.IoDispatcher
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class LocalAuthGatewayAdapter @Inject constructor(
    private val demoAuthGateway: DemoAuthGateway,
    private val userDataSource: LocalUserDataSource,
    private val clock: TervynClock,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : AuthGateway {
    override suspend fun login(email: String, password: String): AppResult<AuthGatewayResult> {
        return when (val result = demoAuthGateway.authenticate(email, password)) {
            is AppResult.Failure -> AppResult.Failure(result.error)
            is AppResult.Success -> {
                val user = userDataSource.getUser(result.data.userId)
                    ?: return AppResult.Failure(AppError.Authentication("local_user_missing"))
                AppResult.Success(AuthGatewayResult(user, createDemoSession(user.id, clock.now())))
            }
        }
    }

    override suspend fun revoke(session: StoredSession): AppResult<Unit> = AppResult.Success(Unit)

    private suspend fun createDemoSession(userId: String, issuedAt: Instant): StoredSession = withContext(ioDispatcher) {
        StoredSession(
            userId = userId,
            accessToken = "local_demo_access_${randomToken()}",
            refreshToken = "local_demo_refresh_${randomToken()}",
            issuedAt = issuedAt,
            accessTokenExpiresAt = issuedAt.plusSeconds(ACCESS_TOKEN_TTL_SECONDS),
            refreshTokenExpiresAt = issuedAt.plusSeconds(REFRESH_TOKEN_TTL_SECONDS),
            schemaVersion = StoredSession.SCHEMA_VERSION
        )
    }

    private fun randomToken(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private companion object {
        private const val ACCESS_TOKEN_TTL_SECONDS = 15 * 60L
        private const val REFRESH_TOKEN_TTL_SECONDS = 7 * 24 * 60 * 60L
        private val secureRandom = SecureRandom()
    }
}
