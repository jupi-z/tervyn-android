package dev.amenokizele.tervyn.data.remote.auth

import dev.amenokizele.tervyn.core.result.AppResult
import dev.amenokizele.tervyn.data.auth.session.StoredSession
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeSessionCoordinator(initial: StoredSession?) : SessionCoordinator {
    private val mutableState = MutableStateFlow<SessionState>(
        initial?.let(SessionState::Active) ?: SessionState.Empty
    )
    override val state: StateFlow<SessionState> = mutableState
    var current: StoredSession? = initial
    var replaceResult: AppResult<Unit> = AppResult.Success(Unit)
    var clearResult: AppResult<Unit> = AppResult.Success(Unit)
    var replaceCalls = 0
    var clearCalls = 0

    override suspend fun restore(): AppResult<StoredSession?> = AppResult.Success(current)

    override suspend fun replace(session: StoredSession): AppResult<Unit> {
        replaceCalls += 1
        if (replaceResult is AppResult.Success) {
            current = session
            mutableState.value = SessionState.Active(session)
        }
        return replaceResult
    }

    override suspend fun clear(): AppResult<Unit> {
        clearCalls += 1
        if (clearResult is AppResult.Success) {
            current = null
            mutableState.value = SessionState.Empty
        }
        return clearResult
    }

    override fun snapshot(): StoredSession? = current

    companion object {
        fun active(accessToken: String = "access-token") = FakeSessionCoordinator(
            StoredSession(
                userId = "user-remote",
                accessToken = accessToken,
                refreshToken = "refresh-token",
                issuedAt = Instant.parse("2026-09-09T10:00:00Z"),
                accessTokenExpiresAt = Instant.parse("2026-09-09T10:15:00Z"),
                refreshTokenExpiresAt = Instant.parse("2026-09-16T10:00:00Z")
            )
        )
    }
}
