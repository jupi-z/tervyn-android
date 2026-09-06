package dev.amenokizele.tervyn.data.auth

import dev.amenokizele.tervyn.FakeTervynClock
import dev.amenokizele.tervyn.core.result.AppError
import dev.amenokizele.tervyn.core.result.AppResult
import dev.amenokizele.tervyn.data.auth.demo.DemoAuthGateway
import dev.amenokizele.tervyn.data.auth.local.LocalUserDataSource
import dev.amenokizele.tervyn.data.auth.repository.PersistentAuthRepository
import dev.amenokizele.tervyn.data.auth.session.SecureSessionStore
import dev.amenokizele.tervyn.data.auth.session.StoredSession
import dev.amenokizele.tervyn.domain.model.AuthState
import dev.amenokizele.tervyn.domain.model.User
import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PersistentAuthRepositoryTest {
    private val now = Instant.parse("2026-09-05T10:00:00Z")

    @Test
    fun initialAuthStateIsCheckingBeforeRestoreCompletes() = runTest {
        val repository = repository(
            sessionStore = FakeSecureSessionStore(readResult = AppResult.Success(null))
        )

        assertEquals(AuthState.Checking, repository.authState.first())
    }

    @Test
    fun noStoredSessionRestoresUnauthenticated() = runTest {
        val repository = repository(
            sessionStore = FakeSecureSessionStore(readResult = AppResult.Success(null))
        )

        advanceUntilIdle()

        assertEquals(AuthState.Unauthenticated, repository.authState.first())
    }

    @Test
    fun validStoredSessionWithRoomUserRestoresAuthenticated() = runTest {
        val user = user()
        val repository = repository(
            sessionStore = FakeSecureSessionStore(readResult = AppResult.Success(validSession())),
            userDataSource = FakeLocalUserDataSource(mapOf(user.id to user))
        )

        advanceUntilIdle()

        assertEquals(AuthState.Authenticated(user), repository.authState.first())
    }

    @Test
    fun expiredSessionClearsStorageAndRestoresUnauthenticated() = runTest {
        val sessionStore = FakeSecureSessionStore(
            readResult = AppResult.Success(
                validSession(refreshTokenExpiresAt = now.minusSeconds(1))
            )
        )
        val repository = repository(sessionStore = sessionStore)

        advanceUntilIdle()

        assertEquals(AuthState.Unauthenticated, repository.authState.first())
        assertEquals(1, sessionStore.clearCalls)
    }

    @Test
    fun missingRoomUserClearsStorageAndRestoresUnauthenticated() = runTest {
        val sessionStore = FakeSecureSessionStore(readResult = AppResult.Success(validSession()))
        val repository = repository(
            sessionStore = sessionStore,
            userDataSource = FakeLocalUserDataSource(emptyMap())
        )

        advanceUntilIdle()

        assertEquals(AuthState.Unauthenticated, repository.authState.first())
        assertEquals(1, sessionStore.clearCalls)
    }

    @Test
    fun corruptStoreReadFailsClosedToUnauthenticated() = runTest {
        val sessionStore = FakeSecureSessionStore(
            readResult = AppResult.Failure(AppError.Storage("secure_session_read_failed"))
        )
        val repository = repository(sessionStore = sessionStore)

        advanceUntilIdle()

        assertEquals(AuthState.Unauthenticated, repository.authState.first())
        assertEquals(1, sessionStore.clearCalls)
    }

    @Test
    fun loginWithValidDemoCredentialsWritesSessionBeforeAuthenticating() = runTest {
        val user = user()
        val sessionStore = FakeSecureSessionStore(readResult = AppResult.Success(null))
        val repository = repository(
            sessionStore = sessionStore,
            userDataSource = FakeLocalUserDataSource(mapOf(user.id to user))
        )
        advanceUntilIdle()

        val result = repository.login(" AMINA@TERVYN.DEMO ", "tervyn2026")

        assertEquals(AppResult.Success(user), result)
        assertEquals(AuthState.Authenticated(user), repository.authState.first())
        val stored = sessionStore.writtenSession
        assertNotNull(stored)
        requireNotNull(stored)
        assertEquals(user.id, stored.userId)
        assertTrue(stored.accessToken.isNotBlank())
        assertTrue(stored.refreshToken.isNotBlank())
        assertTrue(stored.accessTokenExpiresAt.isAfter(stored.issuedAt))
        assertTrue(stored.refreshTokenExpiresAt.isAfter(stored.issuedAt))
    }

    @Test
    fun loginStoreFailureDoesNotAuthenticate() = runTest {
        val user = user()
        val repository = repository(
            sessionStore = FakeSecureSessionStore(
                readResult = AppResult.Success(null),
                writeResult = AppResult.Failure(AppError.Storage("secure_session_write_failed"))
            ),
            userDataSource = FakeLocalUserDataSource(mapOf(user.id to user))
        )
        advanceUntilIdle()

        val result = repository.login("amina@tervyn.demo", "tervyn2026")

        assertTrue(result is AppResult.Failure)
        assertEquals(AuthState.Unauthenticated, repository.authState.first())
    }

    @Test
    fun failedDemoAuthenticationDoesNotWriteSession() = runTest {
        val sessionStore = FakeSecureSessionStore(readResult = AppResult.Success(null))
        val repository = repository(
            sessionStore = sessionStore,
            demoAuthGateway = FakeDemoAuthGateway(
                AppResult.Failure(AppError.Authentication("invalid_credentials"))
            )
        )
        advanceUntilIdle()

        val result = repository.login("other@tervyn.demo", "wrong")

        assertTrue(result is AppResult.Failure)
        assertEquals(0, sessionStore.writeCalls)
        assertEquals(AuthState.Unauthenticated, repository.authState.first())
    }

    @Test
    fun offlineDemoLoginReturnsInvalidStateAndDoesNotWriteSession() = runTest {
        val sessionStore = FakeSecureSessionStore(readResult = AppResult.Success(null))
        val repository = repository(
            sessionStore = sessionStore,
            demoAuthGateway = FakeDemoAuthGateway(AppResult.Failure(AppError.InvalidState("offline_simulation")))
        )
        advanceUntilIdle()

        val result = repository.login("amina@tervyn.demo", "tervyn2026")

        assertEquals(AppResult.Failure(AppError.InvalidState("offline_simulation")), result)
        assertEquals(0, sessionStore.writeCalls)
    }

    @Test
    fun logoutClearsSecureSessionAndUnauthenticates() = runTest {
        val user = user()
        val sessionStore = FakeSecureSessionStore(readResult = AppResult.Success(validSession()))
        val repository = repository(
            sessionStore = sessionStore,
            userDataSource = FakeLocalUserDataSource(mapOf(user.id to user))
        )
        advanceUntilIdle()

        val result = repository.logout()

        assertEquals(AppResult.Success(Unit), result)
        assertEquals(1, sessionStore.clearCalls)
        assertEquals(AuthState.Unauthenticated, repository.authState.first())
    }

    @Test
    fun logoutFailureDoesNotDeclareSuccessOrClearAuthenticatedState() = runTest {
        val user = user()
        val repository = repository(
            sessionStore = FakeSecureSessionStore(
                readResult = AppResult.Success(validSession()),
                clearResult = AppResult.Failure(AppError.Storage("secure_session_clear_failed"))
            ),
            userDataSource = FakeLocalUserDataSource(mapOf(user.id to user))
        )
        advanceUntilIdle()

        val result = repository.logout()

        assertTrue(result is AppResult.Failure)
        assertEquals(AuthState.Authenticated(user), repository.authState.first())
    }

    @Test
    fun currentAuthenticatedUserExpiresSessionWhenRefreshExpiryPasses() = runTest {
        val clock = FakeTervynClock(now)
        val user = user()
        val sessionStore = FakeSecureSessionStore(readResult = AppResult.Success(validSession()))
        val repository = repository(
            sessionStore = sessionStore,
            userDataSource = FakeLocalUserDataSource(mapOf(user.id to user)),
            clock = clock
        )
        advanceUntilIdle()
        clock.advance(604_801)

        val currentUser = repository.currentAuthenticatedUser()

        assertEquals(null, currentUser)
        assertEquals(1, sessionStore.clearCalls)
        assertEquals(AuthState.Unauthenticated, repository.authState.first())
    }

    @Test
    fun repositoryRecreationRestoresPersistedSession() = runTest {
        val user = user()
        val sessionStore = FakeSecureSessionStore(readResult = AppResult.Success(null))
        val userDataSource = FakeLocalUserDataSource(mapOf(user.id to user))
        val firstRepository = repository(
            sessionStore = sessionStore,
            userDataSource = userDataSource
        )
        advanceUntilIdle()
        firstRepository.login("amina@tervyn.demo", "tervyn2026")

        val secondRepository = repository(
            sessionStore = FakeSecureSessionStore(readResult = AppResult.Success(sessionStore.writtenSession)),
            userDataSource = userDataSource
        )
        advanceUntilIdle()

        assertEquals(AuthState.Authenticated(user), secondRepository.authState.first())
    }

    @Test
    fun expiredAccessTokenWithValidRefreshSessionRemainsAuthenticated() = runTest {
        val session = validSession().copy(
            issuedAt = now.minusSeconds(901),
            accessTokenExpiresAt = now.minusSeconds(1),
            refreshTokenExpiresAt = now.plusSeconds(86_400)
        )
        val store = FakeSecureSessionStore(AppResult.Success(session))
        val repository = repository(sessionStore = store)
        advanceUntilIdle()

        assertEquals(AuthState.Authenticated(user()), repository.authState.first())
        assertEquals(user(), repository.currentAuthenticatedUser())
        assertEquals(0, store.clearCalls)
        assertEquals(session, store.storedSession)
    }

    @Test
    fun failedReloginPreservesExistingSessionAndCurrentUser() = runTest {
        val session = validSession()
        val store = FakeSecureSessionStore(AppResult.Success(session))
        val repository = repository(
            sessionStore = store,
            demoAuthGateway = FakeDemoAuthGateway(
                AppResult.Failure(AppError.Authentication("invalid_credentials"))
            )
        )
        advanceUntilIdle()

        assertEquals(
            AppResult.Failure(AppError.Authentication("invalid_credentials")),
            repository.login("other@tervyn.demo", "wrong")
        )
        assertEquals(AuthState.Authenticated(user()), repository.authState.first())
        assertEquals(user(), repository.currentAuthenticatedUser())
        assertEquals(session, store.storedSession)
        assertEquals(0, store.writeCalls)
        assertEquals(0, store.clearCalls)
    }

    @Test
    fun missingNewLocalUserPreservesExistingSession() = runTest {
        val session = validSession()
        val store = FakeSecureSessionStore(AppResult.Success(session))
        val repository = repository(
            sessionStore = store,
            demoAuthGateway = FakeDemoAuthGateway(
                AppResult.Success(DemoAuthGateway.AuthenticatedDemoUser("missing-user"))
            )
        )
        advanceUntilIdle()

        assertEquals(
            AppResult.Failure(AppError.Authentication("local_user_missing")),
            repository.login("other@tervyn.demo", "demo-password")
        )
        assertEquals(AuthState.Authenticated(user()), repository.authState.first())
        assertEquals(user(), repository.currentAuthenticatedUser())
        assertEquals(session, store.storedSession)
        assertEquals(0, store.writeCalls)
        assertEquals(0, store.clearCalls)
    }

    @Test
    fun failedNewSessionWritePreservesExistingSessionAndCurrentUser() = runTest {
        val session = validSession()
        val newUser = user("other-user")
        val store = FakeSecureSessionStore(
            AppResult.Success(session),
            writeResult = AppResult.Failure(AppError.Storage("secure_session_write_failed"))
        )
        val repository = repository(
            sessionStore = store,
            demoAuthGateway = FakeDemoAuthGateway(
                AppResult.Success(DemoAuthGateway.AuthenticatedDemoUser(newUser.id))
            ),
            userDataSource = FakeLocalUserDataSource(mapOf(user().id to user(), newUser.id to newUser))
        )
        advanceUntilIdle()

        assertEquals(
            AppResult.Failure(AppError.Storage("secure_session_write_failed")),
            repository.login(newUser.email, "demo-password")
        )
        assertEquals(AuthState.Authenticated(user()), repository.authState.first())
        assertEquals(user(), repository.currentAuthenticatedUser())
        assertEquals(session, store.storedSession)
        assertEquals(1, store.writeCalls)
        assertEquals(0, store.clearCalls)
        val recreated = repository(sessionStore = store)
        advanceUntilIdle()
        assertEquals(AuthState.Authenticated(user()), recreated.authState.first())
    }

    @Test
    fun newSessionIsPublishedOnlyAfterWriteCompletes() = runTest {
        val oldSession = validSession()
        val newUser = user("other-user")
        val releaseWrite = CompletableDeferred<Unit>()
        val store = FakeSecureSessionStore(AppResult.Success(oldSession), beforeWrite = { releaseWrite.await() })
        val repository = repository(
            sessionStore = store,
            demoAuthGateway = FakeDemoAuthGateway(
                AppResult.Success(DemoAuthGateway.AuthenticatedDemoUser(newUser.id))
            ),
            userDataSource = FakeLocalUserDataSource(mapOf(user().id to user(), newUser.id to newUser))
        )
        advanceUntilIdle()

        val login = async { repository.login(newUser.email, "demo-password") }
        runCurrent()
        assertFalse(login.isCompleted)
        assertEquals(AuthState.Authenticated(user()), repository.authState.first())
        assertEquals(oldSession, store.storedSession)

        releaseWrite.complete(Unit)
        assertEquals(AppResult.Success(newUser), login.await())
        assertEquals(AuthState.Authenticated(newUser), repository.authState.first())
        assertEquals(newUser, repository.currentAuthenticatedUser())
        assertEquals(newUser.id, store.storedSession?.userId)
    }

    @Test
    fun logoutWaitsForInFlightLoginAndLeavesNoSession() = runTest {
        val releaseWrite = CompletableDeferred<Unit>()
        val store = FakeSecureSessionStore(AppResult.Success(validSession()), beforeWrite = { releaseWrite.await() })
        val repository = repository(sessionStore = store)
        advanceUntilIdle()
        val login = async { repository.login("amina@tervyn.demo", "tervyn2026") }
        runCurrent()

        val logout = async { repository.logout() }
        runCurrent()
        assertFalse(logout.isCompleted)
        assertEquals(0, store.clearCalls)
        releaseWrite.complete(Unit)

        assertTrue(login.await() is AppResult.Success)
        assertEquals(AppResult.Success(Unit), logout.await())
        assertEquals(AuthState.Unauthenticated, repository.authState.first())
        assertEquals(null, repository.currentAuthenticatedUser())
        assertEquals(null, store.storedSession)
    }

    @Test
    fun loginWaitsForRestoreInsteadOfBeingOverwrittenByIt() = runTest {
        val releaseRead = CompletableDeferred<Unit>()
        val store = FakeSecureSessionStore(AppResult.Success(null), beforeRead = { releaseRead.await() })
        val repository = repository(sessionStore = store)
        runCurrent()
        val login = async { repository.login("amina@tervyn.demo", "tervyn2026") }
        runCurrent()
        assertFalse(login.isCompleted)
        assertEquals(0, store.writeCalls)

        releaseRead.complete(Unit)
        assertTrue(login.await() is AppResult.Success)
        assertEquals(AuthState.Authenticated(user()), repository.authState.first())
        assertEquals(user(), repository.currentAuthenticatedUser())
        assertEquals(user().id, store.storedSession?.userId)
    }

    @Test
    fun cancelledLoginBeforeWritePreservesSessionAndReleasesLock() = runTest {
        val session = validSession()
        val store = FakeSecureSessionStore(AppResult.Success(session))
        val repository = repository(
            sessionStore = store,
            demoAuthGateway = object : DemoAuthGateway {
                override suspend fun authenticate(email: String, password: String): AppResult<DemoAuthGateway.AuthenticatedDemoUser> {
                    throw CancellationException("cancelled login")
                }
            }
        )
        advanceUntilIdle()
        val login = async { repository.login("amina@tervyn.demo", "tervyn2026") }
        runCurrent()
        assertTrue(login.isCancelled)
        assertEquals(AuthState.Authenticated(user()), repository.authState.first())
        assertEquals(user(), repository.currentAuthenticatedUser())
        assertEquals(session, store.storedSession)
        assertEquals(0, store.writeCalls)
        assertEquals(AppResult.Success(Unit), repository.logout())
    }

    @Test
    fun cancellationAtWriteCommitDoesNotLeaveMemoryBehindStorage() = runTest {
        val backingStore = FakeSecureSessionStore(AppResult.Success(null))
        val dispatcher = StandardTestDispatcher(testScheduler)
        lateinit var login: Job
        val store = object : SecureSessionStore by backingStore {
            override suspend fun write(session: StoredSession): AppResult<Unit> = withContext(dispatcher) {
                backingStore.write(session).also { login.cancel() }
            }
        }
        val repository = repository(sessionStore = store)
        advanceUntilIdle()

        login = async { repository.login("amina@tervyn.demo", "tervyn2026") }
        advanceUntilIdle()

        assertTrue(login.isCancelled)
        assertEquals(user().id, backingStore.storedSession?.userId)
        assertEquals(AuthState.Authenticated(user()), repository.authState.first())
        assertEquals(user(), repository.currentAuthenticatedUser())
    }

    @Test
    fun cancellationAtLogoutCommitDoesNotLeaveAuthenticatedMemory() = runTest {
        val backingStore = FakeSecureSessionStore(AppResult.Success(validSession()))
        val dispatcher = StandardTestDispatcher(testScheduler)
        lateinit var logout: Job
        val store = object : SecureSessionStore by backingStore {
            override suspend fun clear(): AppResult<Unit> = withContext(dispatcher) {
                backingStore.clear().also { logout.cancel() }
            }
        }
        val repository = repository(sessionStore = store)
        advanceUntilIdle()

        logout = async { repository.logout() }
        advanceUntilIdle()

        assertTrue(logout.isCancelled)
        assertEquals(null, backingStore.storedSession)
        assertEquals(AuthState.Unauthenticated, repository.authState.first())
        assertEquals(null, repository.currentAuthenticatedUser())
    }

    @Test
    fun cancellationAtExpiryCleanupDoesNotLeaveAuthenticatedMemory() = runTest {
        val clock = FakeTervynClock(now)
        val backingStore = FakeSecureSessionStore(AppResult.Success(validSession()))
        val dispatcher = StandardTestDispatcher(testScheduler)
        lateinit var currentUser: Job
        val store = object : SecureSessionStore by backingStore {
            override suspend fun clear(): AppResult<Unit> = withContext(dispatcher) {
                backingStore.clear().also { currentUser.cancel() }
            }
        }
        val repository = repository(
            sessionStore = store,
            clock = clock
        )
        advanceUntilIdle()
        clock.advance(604_801)

        currentUser = async { repository.currentAuthenticatedUser() }
        advanceUntilIdle()

        assertTrue(currentUser.isCancelled)
        assertEquals(null, backingStore.storedSession)
        assertEquals(AuthState.Unauthenticated, repository.authState.first())
        assertEquals(null, repository.currentAuthenticatedUser())
    }

    private fun TestScope.repository(
        sessionStore: SecureSessionStore = FakeSecureSessionStore(AppResult.Success(null)),
        demoAuthGateway: DemoAuthGateway = FakeDemoAuthGateway(
            AppResult.Success(DemoAuthGateway.AuthenticatedDemoUser("user-amina"))
        ),
        userDataSource: LocalUserDataSource = FakeLocalUserDataSource(mapOf("user-amina" to user())),
        clock: FakeTervynClock = FakeTervynClock(now)
    ) = PersistentAuthRepository(
        secureSessionStore = sessionStore,
        demoAuthGateway = demoAuthGateway,
        userDataSource = userDataSource,
        clock = clock,
        applicationScope = this,
        ioDispatcher = StandardTestDispatcher(testScheduler)
    )

    private fun validSession(
        refreshTokenExpiresAt: Instant = now.plusSeconds(604_800)
    ) = StoredSession(
        userId = "user-amina",
        accessToken = "local_demo_access_token",
        refreshToken = "local_demo_refresh_token",
        issuedAt = now,
        accessTokenExpiresAt = now.plusSeconds(900),
        refreshTokenExpiresAt = refreshTokenExpiresAt,
        schemaVersion = 1
    )

    private fun user(id: String = "user-amina") = User(
        id = id,
        email = "amina@tervyn.demo",
        firstName = "Amina",
        lastName = "Kabwe",
        jobTitle = "Technicienne terrain",
        avatarUrl = null,
        createdAt = now,
        updatedAt = now,
        lastSyncedAt = now
    )

    private class FakeSecureSessionStore(
        private val readResult: AppResult<StoredSession?>,
        private val writeResult: AppResult<Unit> = AppResult.Success(Unit),
        private val clearResult: AppResult<Unit> = AppResult.Success(Unit),
        private val beforeWrite: suspend () -> Unit = {},
        private val beforeRead: suspend () -> Unit = {}
    ) : SecureSessionStore {
        var storedSession: StoredSession? = (readResult as? AppResult.Success)?.data
            private set
        var writtenSession: StoredSession? = null
            private set
        var writeCalls = 0
            private set
        var clearCalls = 0
            private set

        override suspend fun read(): AppResult<StoredSession?> {
            val result = if (readResult is AppResult.Failure) readResult else AppResult.Success(storedSession)
            beforeRead()
            return result
        }

        override suspend fun write(session: StoredSession): AppResult<Unit> {
            writeCalls += 1
            beforeWrite()
            if (writeResult is AppResult.Success) {
                writtenSession = session
                storedSession = session
            }
            return writeResult
        }

        override suspend fun clear(): AppResult<Unit> {
            clearCalls += 1
            if (clearResult is AppResult.Success) storedSession = null
            return clearResult
        }
    }

    private class FakeDemoAuthGateway(
        private val result: AppResult<DemoAuthGateway.AuthenticatedDemoUser>
    ) : DemoAuthGateway {
        override suspend fun authenticate(
            email: String,
            password: String
        ): AppResult<DemoAuthGateway.AuthenticatedDemoUser> = result
    }

    private class FakeLocalUserDataSource(
        private val users: Map<String, User>
    ) : LocalUserDataSource {
        override suspend fun getUser(userId: String): User? = users[userId]
    }
}
