package dev.amenokizele.tervyn.data.remote.auth

import dev.amenokizele.tervyn.core.result.AppError
import dev.amenokizele.tervyn.core.result.AppResult
import java.time.Instant
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

class SessionRefreshAuthenticator(
    private val sessionCoordinator: SessionCoordinator,
    private val tokenRefresher: RemoteTokenRefresher,
    private val now: () -> Instant = Instant::now
) : Authenticator {
    private val lock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.responseCount() > 1) return null
        val sentToken = response.request.header("Authorization")?.removePrefix("Bearer ")?.takeIf { it.isNotBlank() }

        synchronized(lock) {
            val currentSession = sessionCoordinator.snapshot() ?: return null
            if (sentToken != null && currentSession.accessToken != sentToken) {
                return response.request.withBearer(currentSession.accessToken)
            }
            if (!currentSession.refreshTokenExpiresAt.isAfter(now())) {
                clearInvalidSession()
                return null
            }

            return when (val refreshed = tokenRefresher.refresh(currentSession)) {
                is AppResult.Success -> {
                    when (sessionCoordinator.replaceBlocking(refreshed.data)) {
                        is AppResult.Success -> response.request.withBearer(refreshed.data.accessToken)
                        is AppResult.Failure -> null
                    }
                }

                is AppResult.Failure -> {
                    if (refreshed.error.isInvalidRefresh()) {
                        clearInvalidSession()
                    }
                    null
                }
            }
        }
    }

    private fun SessionCoordinator.replaceBlocking(session: dev.amenokizele.tervyn.data.auth.session.StoredSession) =
        runBlocking { replace(session) }

    private fun clearInvalidSession() {
        runBlocking {
            sessionCoordinator.clear()
        }
    }

    private fun Request.withBearer(accessToken: String): Request {
        return newBuilder()
            .header("Authorization", "Bearer $accessToken")
            .build()
    }

    private fun Response.responseCount(): Int {
        var current: Response? = this
        var count = 0
        while (current != null) {
            count += 1
            current = current.priorResponse
        }
        return count
    }

    private fun AppError.isInvalidRefresh(): Boolean {
        return when (this) {
            is AppError.Authentication -> true
            is AppError.Validation -> code == "invalid_refresh_token"
            else -> false
        }
    }
}
