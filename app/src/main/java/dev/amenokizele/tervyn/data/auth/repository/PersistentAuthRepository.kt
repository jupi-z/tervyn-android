package dev.amenokizele.tervyn.data.auth.repository

import dev.amenokizele.tervyn.core.result.AppError
import dev.amenokizele.tervyn.core.result.AppResult
import dev.amenokizele.tervyn.core.time.TervynClock
import dev.amenokizele.tervyn.data.auth.local.LocalUserDataSource
import dev.amenokizele.tervyn.data.auth.session.StoredSession
import dev.amenokizele.tervyn.data.remote.auth.AuthGateway
import dev.amenokizele.tervyn.data.remote.auth.SessionCoordinator
import dev.amenokizele.tervyn.data.remote.auth.SessionState
import dev.amenokizele.tervyn.di.ApplicationScope
import dev.amenokizele.tervyn.domain.model.AuthState
import dev.amenokizele.tervyn.domain.model.User
import dev.amenokizele.tervyn.domain.repository.AuthRepository
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Singleton
class PersistentAuthRepository @Inject constructor(
    private val sessionCoordinator: SessionCoordinator,
    private val authGateway: AuthGateway,
    private val userDataSource: LocalUserDataSource,
    private val clock: TervynClock,
    @ApplicationScope applicationScope: CoroutineScope
) : AuthRepository {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Checking)
    override val authState: Flow<AuthState> = _authState.asStateFlow()

    private val sessionMutex = Mutex()

    private val restoration = applicationScope.launch {
        sessionMutex.withLock {
            restoreSession()
        }
    }

    init {
        applicationScope.launch {
            restoration.join()
            observeSessionInvalidation()
        }
    }

    private suspend fun observeSessionInvalidation() {
        sessionCoordinator.state.collect { state ->
            if (state is SessionState.Empty && _authState.value is AuthState.Authenticated) {
                _authState.value = AuthState.Unauthenticated
            }
        }
    }

    override suspend fun currentAuthenticatedUser(): User? {
        restoration.join()
        return sessionMutex.withLock { currentAuthenticatedUserLocked() }
    }

    private suspend fun currentAuthenticatedUserLocked(): User? {
        val session = sessionCoordinator.snapshot() ?: return null
        if (isSessionExpired(session, clock.now())) {
            invalidateSession()
            return null
        }

        val user = userDataSource.getUser(session.userId)
        if (user == null) {
            invalidateSession()
        }
        return user
    }

    override suspend fun login(email: String, password: String): AppResult<User> {
        restoration.join()
        return sessionMutex.withLock {
            when (val authResult = authGateway.login(email, password)) {
                is AppResult.Failure -> AppResult.Failure(authResult.error)

                is AppResult.Success -> loginAuthenticatedUser(authResult.data.user, authResult.data.session)
            }
        }
    }

    override suspend fun logout(): AppResult<Unit> {
        restoration.join()
        return sessionMutex.withLock {
            currentCoroutineContext().ensureActive()
            val session = sessionCoordinator.snapshot()
            var remoteCancellation: CancellationException? = null
            if (session != null) {
                try {
                    authGateway.revoke(session)
                } catch (exception: CancellationException) {
                    remoteCancellation = exception
                } catch (_: Exception) {
                    // Remote revocation is best-effort; local token clearing is authoritative.
                }
            }
            val result = withContext(NonCancellable) {
                when (val clearResult = sessionCoordinator.clear()) {
                    is AppResult.Success -> {
                        _authState.value = AuthState.Unauthenticated
                        AppResult.Success(Unit)
                    }

                    is AppResult.Failure -> AppResult.Failure(clearResult.error)
                }
            }
            remoteCancellation?.let { throw it }
            currentCoroutineContext().ensureActive()
            result
        }
    }

    private suspend fun loginAuthenticatedUser(user: User, session: StoredSession): AppResult<User> {
        currentCoroutineContext().ensureActive()
        // Once persistence starts, publish its result before honoring caller cancellation.
        val result = withContext(NonCancellable) {
            when (val upsert = userDataSource.upsertUser(user)) {
                is AppResult.Failure -> return@withContext AppResult.Failure(upsert.error)
                is AppResult.Success -> Unit
            }
            when (val writeResult = sessionCoordinator.replace(session)) {
                is AppResult.Success -> {
                    _authState.value = AuthState.Authenticated(user)
                    AppResult.Success(user)
                }

                is AppResult.Failure -> AppResult.Failure(writeResult.error)
            }
        }
        currentCoroutineContext().ensureActive()
        return result
    }

    private suspend fun restoreSession() {
        try {
            when (val readResult = sessionCoordinator.restore()) {
                is AppResult.Success -> restoreSession(readResult.data)
                is AppResult.Failure -> {
                    invalidateSession()
                }
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            invalidateSession()
        }
    }

    private suspend fun restoreSession(session: StoredSession?) {
        if (session == null) {
            _authState.value = AuthState.Unauthenticated
            return
        }
        if (isSessionExpired(session, clock.now())) {
            invalidateSession()
            return
        }

        val user = userDataSource.getUser(session.userId)
        if (user == null) {
            invalidateSession()
            return
        }

        _authState.value = AuthState.Authenticated(user)
    }

    private suspend fun invalidateSession() {
        currentCoroutineContext().ensureActive()
        withContext(NonCancellable) {
            sessionCoordinator.clear()
        }
        // Deterministic local invalidation prevents UI use even if durable cleanup reports a rare failure.
        _authState.value = AuthState.Unauthenticated
        currentCoroutineContext().ensureActive()
    }

    private fun isSessionExpired(session: StoredSession, now: Instant): Boolean {
        return !session.refreshTokenExpiresAt.isAfter(now)
    }
}
