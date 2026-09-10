package dev.amenokizele.tervyn.data.remote.auth

import dev.amenokizele.tervyn.core.result.AppError
import dev.amenokizele.tervyn.core.result.AppResult
import dev.amenokizele.tervyn.data.auth.session.StoredSession
import dev.amenokizele.tervyn.data.remote.config.RemoteApiConfig
import dev.amenokizele.tervyn.data.remote.error.RemoteErrorMapper
import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BearerAndRefreshTest {
    private lateinit var server: MockWebServer
    private lateinit var coordinator: FakeSessionCoordinator

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        coordinator = FakeSessionCoordinator(session("token-A", "refresh-A"))
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun bearerInterceptorAddsAuthorizationFromMemorySnapshotOnlyWhenSessionExists() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        clientWithoutAuthenticator(coordinator).newCall(Request.Builder().url(server.url("/v1/jobs")).build()).execute().close()
        assertEquals("Bearer token-A", server.takeRequest().getHeader("Authorization"))

        coordinator.current = null
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        clientWithoutAuthenticator(coordinator).newCall(Request.Builder().url(server.url("/v1/jobs")).build()).execute().close()
        assertNull(server.takeRequest().getHeader("Authorization"))
        assertEquals(2, coordinator.snapshotCalls)
        assertEquals(0, coordinator.restoreCalls)
    }

    @Test
    fun response401RefreshesPersistsRotatedSessionAndRetriesOnce() {
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"error":{"code":"unauthorized"}}"""))
        server.enqueue(MockResponse().setResponseCode(200).setBody(refreshResponse()))
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"items":[],"nextCursor":null,"serverTime":"2026-09-09T10:00:00Z"}"""))

        clientWithAuthenticator().newCall(Request.Builder().url(server.url("/v1/jobs")).build()).execute().use { response ->
            assertEquals(200, response.code)
        }

        assertEquals("Bearer token-A", server.takeRequest().getHeader("Authorization"))
        val refresh = server.takeRequest()
        assertEquals("/v1/auth/refresh", refresh.path)
        assertNull(refresh.getHeader("Authorization"))
        assertEquals("""{"refreshToken":"refresh-A"}""", refresh.body.readUtf8())
        assertEquals("Bearer token-B", server.takeRequest().getHeader("Authorization"))
        assertEquals("token-B", coordinator.current?.accessToken)
        assertEquals("refresh-B", coordinator.current?.refreshToken)
        assertEquals(1, coordinator.replaceCalls)
    }

    @Test
    fun concurrent401ResponsesShareSingleRefresh() {
        repeat(3) {
            server.enqueue(MockResponse().setResponseCode(401).setBody("""{"error":{"code":"unauthorized"}}"""))
        }
        server.enqueue(MockResponse().setResponseCode(200).setBody(refreshResponse()))
        repeat(3) {
            server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        }

        val client = clientWithAuthenticator()
        val executor = Executors.newFixedThreadPool(3)
        val latch = CountDownLatch(3)
        repeat(3) {
            executor.execute {
                client.newCall(Request.Builder().url(server.url("/v1/jobs")).build()).execute().close()
                latch.countDown()
            }
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS))
        executor.shutdownNow()
        val paths = (0 until server.requestCount).map { server.takeRequest().path.orEmpty() }
        assertEquals(1, paths.count { it == "/v1/auth/refresh" })
        assertEquals(1, coordinator.replaceCalls)
    }

    @Test
    fun invalidRefreshClearsSessionAndDoesNotLoop() {
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"error":{"code":"unauthorized"}}"""))
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"error":{"code":"invalid_refresh_token"}}"""))

        clientWithAuthenticator().newCall(Request.Builder().url(server.url("/v1/jobs")).build()).execute().use { response ->
            assertEquals(401, response.code)
        }

        assertNull(coordinator.current)
        assertEquals(1, coordinator.clearCalls)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun invalidRefreshClearFailurePreservesActiveSessionInMemory() {
        val oldSession = requireNotNull(coordinator.current)
        coordinator.clearResult = AppResult.Failure(AppError.Storage("secure_session_clear_failed"))
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"error":{"code":"unauthorized"}}"""))
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"error":{"code":"invalid_refresh_token"}}"""))

        clientWithAuthenticator().newCall(Request.Builder().url(server.url("/v1/jobs")).build()).execute().use { response ->
            assertEquals(401, response.code)
        }

        assertEquals(oldSession, coordinator.current)
        assertEquals(1, coordinator.clearCalls)
        assertEquals(0, coordinator.markEmptyCalls)
    }

    @Test
    fun expiredRefreshClearFailurePreservesActiveSessionInMemoryWithoutCallingRefresh() {
        val oldSession = session(
            accessToken = "token-A",
            refreshToken = "refresh-A",
            refreshTokenExpiresAt = Instant.parse("2026-09-09T09:59:59Z")
        )
        coordinator.current = oldSession
        coordinator.clearResult = AppResult.Failure(AppError.Storage("secure_session_clear_failed"))
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"error":{"code":"unauthorized"}}"""))

        clientWithAuthenticator().newCall(Request.Builder().url(server.url("/v1/jobs")).build()).execute().use { response ->
            assertEquals(401, response.code)
        }

        assertEquals(oldSession, coordinator.current)
        assertEquals(1, coordinator.clearCalls)
        assertEquals(0, coordinator.markEmptyCalls)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun refreshNetworkFailureAndPersistenceFailurePreserveOldSession() {
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(500))

        clientWithAuthenticator().newCall(Request.Builder().url(server.url("/v1/jobs")).build()).execute().close()
        assertEquals("token-A", coordinator.current?.accessToken)

        coordinator.replaceResult = AppResult.Failure(AppError.Storage("secure_session_write_failed"))
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(200).setBody(refreshResponse()))
        clientWithAuthenticator().newCall(Request.Builder().url(server.url("/v1/jobs")).build()).execute().close()
        assertEquals("token-A", coordinator.current?.accessToken)
    }

    @Test
    fun alreadyRefreshedTokenRetriesWithoutCallingRefreshEndpoint() {
        val request = Request.Builder()
            .url(server.url("/v1/jobs"))
            .header("Authorization", "Bearer token-A")
            .build()
        coordinator.current = session("token-B", "refresh-B")
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))

        clientWithAuthenticator().newCall(request).execute().use { response ->
            assertEquals(200, response.code)
        }

        assertEquals("Bearer token-A", server.takeRequest().getHeader("Authorization"))
        assertEquals("Bearer token-B", server.takeRequest().getHeader("Authorization"))
        assertEquals(0, coordinator.replaceCalls)
    }

    private fun clientWithoutAuthenticator(coordinator: SessionCoordinator): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(SafeHeadersInterceptor("1.0"))
            .addInterceptor(BearerTokenInterceptor(coordinator))
            .build()
    }

    private fun clientWithAuthenticator(): OkHttpClient {
        val config = RemoteApiConfig(enabled = true, baseUrl = server.url("/").toString(), allowHttpForTests = true)
        val authApi = RemoteApiFactory.createPublicRetrofit(config, RemoteJson.json).create(PublicAuthApi::class.java)
        val refresher = RemoteTokenRefresher(authApi, RemoteErrorMapper(RemoteJson.json))
        return clientWithoutAuthenticator(coordinator).newBuilder()
            .authenticator(SessionRefreshAuthenticator(coordinator, refresher) { Instant.parse("2026-09-09T10:00:00Z") })
            .build()
    }

    private fun refreshResponse() = """
        {
          "accessToken": "token-B",
          "refreshToken": "refresh-B",
          "issuedAt": "2026-09-09T10:01:00Z",
          "accessTokenExpiresAt": "2026-09-09T10:16:00Z",
          "refreshTokenExpiresAt": "2026-09-16T10:01:00Z"
        }
    """.trimIndent()

    private fun session(
        accessToken: String,
        refreshToken: String,
        refreshTokenExpiresAt: Instant = Instant.parse("2026-09-16T10:00:00Z")
    ) = StoredSession(
        userId = "user-remote",
        accessToken = accessToken,
        refreshToken = refreshToken,
        issuedAt = Instant.parse("2026-09-09T10:00:00Z"),
        accessTokenExpiresAt = Instant.parse("2026-09-09T10:15:00Z"),
        refreshTokenExpiresAt = refreshTokenExpiresAt
    )

    private class FakeSessionCoordinator(
        initialSession: StoredSession?
    ) : SessionCoordinator {
        override val state: StateFlow<SessionState> = MutableStateFlow(
            initialSession?.let(SessionState::Active) ?: SessionState.Empty
        )
        var current: StoredSession? = initialSession
        var snapshotCalls = 0
        var restoreCalls = 0
        var replaceCalls = 0
        var clearCalls = 0
        var markEmptyCalls = 0
        var replaceResult: AppResult<Unit> = AppResult.Success(Unit)
        var clearResult: AppResult<Unit> = AppResult.Success(Unit)

        override suspend fun restore(): AppResult<StoredSession?> {
            restoreCalls += 1
            return AppResult.Success(current)
        }

        override suspend fun replace(session: StoredSession): AppResult<Unit> {
            replaceCalls += 1
            if (replaceResult is AppResult.Success) current = session
            return replaceResult
        }

        override suspend fun clear(): AppResult<Unit> {
            clearCalls += 1
            if (clearResult is AppResult.Success) current = null
            return clearResult
        }

        override fun snapshot(): StoredSession? {
            snapshotCalls += 1
            return current
        }
    }
}
