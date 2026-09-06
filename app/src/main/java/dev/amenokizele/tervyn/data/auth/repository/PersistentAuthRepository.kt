package dev.amenokizele.tervyn.data.auth.repository

import dev.amenokizele.tervyn.core.result.AppError
import dev.amenokizele.tervyn.core.result.AppResult
import dev.amenokizele.tervyn.core.time.TervynClock
import dev.amenokizele.tervyn.data.auth.demo.DemoAuthGateway
import dev.amenokizele.tervyn.data.auth.local.LocalUserDataSource
import dev.amenokizele.tervyn.data.auth.session.SecureSessionStore
import dev.amenokizele.tervyn.data.auth.session.StoredSession
import dev.amenokizele.tervyn.di.ApplicationScope
import dev.amenokizele.tervyn.di.IoDispatcher
import dev.amenokizele.tervyn.domain.model.AuthState
import dev.amenokizele.tervyn.domain.model.User
import dev.amenokizele.tervyn.domain.repository.AuthRepository
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
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
    private val secureSessionStore: SecureSessionStore,
    private val demoAuthGateway: DemoAuthGateway,
    private val userDataSource: LocalUserDataSource,
    private val clock: TervynClock,
    @ApplicationScope applicationScope: CoroutineScope,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : AuthRepository {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Checking)
    override val authState: Flow<AuthState> = _authState.asStateFlow()

    private var restoredSession: StoredSession? = null
    private val sessionMutex = Mutex()

    private val restoration = applicationScope.launch {
        sessionMutex.withLock {
            restoreSession()
        }
    }

    override suspend fun currentAuthenticatedUser(): User? {
        restoration.join()
        return sessionMutex.withLock { currentAuthenticatedUserLocked() }
    }

    private suspend fun currentAuthenticatedUserLocked(): User? {
        val session = restoredSession ?: return null
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
            when (val authResult = demoAuthGateway.authenticate(email, password)) {
                is AppResult.Failure -> AppResult.Failure(authResult.error)

                is AppResult.Success -> loginAuthenticatedDemoUser(authResult.data.userId)
            }
        }
    }

    override suspend fun logout(): AppResult<Unit> {
        restoration.join()
        return sessionMutex.withLock {
            currentCoroutineContext().ensureActive()
            val result = withContext(NonCancellable) {
                when (val clearResult = secureSessionStore.clear()) {
                    is AppResult.Success -> {
                        restoredSession = null
                        _authState.value = AuthState.Unauthenticated
                        AppResult.Success(Unit)
                    }

                    is AppResult.Failure -> AppResult.Failure(clearResult.error)
                }
            }
            currentCoroutineContext().ensureActive()
            result
        }
    }

    private suspend fun loginAuthenticatedDemoUser(userId: String): AppResult<User> {
        val user = userDataSource.getUser(userId)
            ?: return AppResult.Failure(AppError.Authentication("local_user_missing"))
        val session = createDemoSession(user.id, clock.now())

        currentCoroutineContext().ensureActive()
        // Once persistence starts, publish its result before honoring caller cancellation.
        val result = withContext(NonCancellable) {
            when (val writeResult = secureSessionStore.write(session)) {
                is AppResult.Success -> {
                    restoredSession = session
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
            when (val readResult = secureSessionStore.read()) {
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

        restoredSession = session
        _authState.value = AuthState.Authenticated(user)
    }

    private suspend fun invalidateSession() {
        currentCoroutineContext().ensureActive()
        withContext(NonCancellable) {
            secureSessionStore.clear()
            restoredSession = null
            _authState.value = AuthState.Unauthenticated
        }
        currentCoroutineContext().ensureActive()
    }

    private fun isSessionExpired(session: StoredSession, now: Instant): Boolean {
        return !session.refreshTokenExpiresAt.isAfter(now)
    }

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

    companion object {
        private const val ACCESS_TOKEN_TTL_SECONDS = 15 * 60L
        private const val REFRESH_TOKEN_TTL_SECONDS = 7 * 24 * 60 * 60L
        private val secureRandom = SecureRandom()
    }
}
