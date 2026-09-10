package dev.amenokizele.tervyn.data.remote.auth

import dev.amenokizele.tervyn.core.result.AppError
import dev.amenokizele.tervyn.core.result.AppResult
import dev.amenokizele.tervyn.data.auth.session.SecureSessionStore
import dev.amenokizele.tervyn.data.auth.session.StoredSession
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionCoordinatorTest {
    @Test
    fun restoreNullPublishesEmpty() = runTest {
        val coordinator = SecureSessionCoordinator(FakeSecureSessionStore(storedSession = null))

        assertEquals(AppResult.Success(null), coordinator.restore())

        assertEquals(SessionState.Empty, coordinator.state.value)
        assertEquals(null, coordinator.snapshot())
    }

    @Test
    fun restoreActivePublishesActiveSession() = runTest {
        val session = session("token-A")
        val coordinator = SecureSessionCoordinator(FakeSecureSessionStore(storedSession = session))

        assertEquals(AppResult.Success(session), coordinator.restore())

        assertEquals(SessionState.Active(session), coordinator.state.value)
        assertEquals(session, coordinator.snapshot())
    }

    @Test
    fun clearSuccessPublishesEmpty() = runTest {
        val session = session("token-A")
        val coordinator = SecureSessionCoordinator(FakeSecureSessionStore(storedSession = session))
        coordinator.restore()

        assertEquals(AppResult.Success(Unit), coordinator.clear())

        assertEquals(SessionState.Empty, coordinator.state.value)
        assertEquals(null, coordinator.snapshot())
    }

    @Test
    fun clearFailureKeepsPreviousActiveState() = runTest {
        val session = session("token-A")
        val coordinator = SecureSessionCoordinator(
            FakeSecureSessionStore(
                storedSession = session,
                clearResult = AppResult.Failure(AppError.Storage("secure_session_clear_failed"))
            )
        )
        coordinator.restore()

        assertEquals(AppResult.Failure(AppError.Storage("secure_session_clear_failed")), coordinator.clear())

        assertEquals(SessionState.Active(session), coordinator.state.value)
        assertEquals(session, coordinator.snapshot())
    }

    @Test
    fun replaceFailureKeepsPreviousActiveState() = runTest {
        val oldSession = session("token-A")
        val newSession = session("token-B")
        val coordinator = SecureSessionCoordinator(
            FakeSecureSessionStore(
                storedSession = oldSession,
                writeResult = AppResult.Failure(AppError.Storage("secure_session_write_failed"))
            )
        )
        coordinator.restore()

        assertEquals(AppResult.Failure(AppError.Storage("secure_session_write_failed")), coordinator.replace(newSession))

        assertEquals(SessionState.Active(oldSession), coordinator.state.value)
        assertEquals(oldSession, coordinator.snapshot())
    }

    private fun session(accessToken: String) = StoredSession(
        userId = "user-remote",
        accessToken = accessToken,
        refreshToken = "refresh-token",
        issuedAt = Instant.parse("2026-09-09T10:00:00Z"),
        accessTokenExpiresAt = Instant.parse("2026-09-09T10:15:00Z"),
        refreshTokenExpiresAt = Instant.parse("2026-09-16T10:00:00Z")
    )

    private class FakeSecureSessionStore(
        private var storedSession: StoredSession?,
        private val writeResult: AppResult<Unit> = AppResult.Success(Unit),
        private val clearResult: AppResult<Unit> = AppResult.Success(Unit)
    ) : SecureSessionStore {
        override suspend fun read(): AppResult<StoredSession?> = AppResult.Success(storedSession)

        override suspend fun write(session: StoredSession): AppResult<Unit> {
            if (writeResult is AppResult.Success) {
                storedSession = session
            }
            return writeResult
        }

        override suspend fun clear(): AppResult<Unit> {
            if (clearResult is AppResult.Success) {
                storedSession = null
            }
            return clearResult
        }
    }
}
