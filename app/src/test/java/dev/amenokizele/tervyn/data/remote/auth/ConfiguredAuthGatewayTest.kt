package dev.amenokizele.tervyn.data.remote.auth

import dev.amenokizele.tervyn.FakeTervynClock
import dev.amenokizele.tervyn.core.result.AppResult
import dev.amenokizele.tervyn.data.auth.demo.DemoAuthGateway
import dev.amenokizele.tervyn.data.auth.local.LocalUserDataSource
import dev.amenokizele.tervyn.data.remote.config.RemoteApiConfig
import dev.amenokizele.tervyn.data.remote.error.RemoteErrorMapper
import dev.amenokizele.tervyn.domain.model.User
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConfiguredAuthGatewayTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun disabledRemoteApiUsesLocalDemoGatewayWithoutHttpRequest() = runTest {
        val gateway = configuredGateway(enabled = false)

        val result = gateway.login("amina@tervyn.demo", "tervyn2026")

        val authenticated = (result as AppResult.Success).data
        assertEquals("user-amina", authenticated.user.id)
        assertEquals("user-amina", authenticated.session.userId)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun enabledRemoteApiUsesRemoteGatewayInsteadOfDemoCredentials() = runTest {
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"error":{"code":"invalid_credentials"}}"""))
        val gateway = configuredGateway(enabled = true)

        assertEquals(
            AppResult.Failure(dev.amenokizele.tervyn.core.result.AppError.Authentication("invalid_credentials")),
            gateway.login("amina@tervyn.demo", "tervyn2026")
        )

        assertEquals("/v1/auth/login", server.takeRequest().path)
    }

    private fun configuredGateway(enabled: Boolean): ConfiguredAuthGateway {
        val config = RemoteApiConfig(enabled = enabled, baseUrl = server.url("/").toString(), allowHttpForTests = true)
        val publicAuthApi = RemoteApiFactory.createPublicRetrofit(config, RemoteJson.json).create(PublicAuthApi::class.java)
        val authenticatedAuthApi = RemoteApiFactory.createPublicRetrofit(config, RemoteJson.json).create(AuthenticatedAuthApi::class.java)
        val users = FakeLocalUserDataSource(mapOf("user-amina" to user()))
        return ConfiguredAuthGateway(
            config = config,
            localGateway = LocalAuthGatewayAdapter(
                demoAuthGateway = FakeDemoAuthGateway(AppResult.Success(DemoAuthGateway.AuthenticatedDemoUser("user-amina"))),
                userDataSource = users,
                clock = FakeTervynClock(now),
                ioDispatcher = UnconfinedTestDispatcher()
            ),
            remoteGateway = RemoteAuthGateway(publicAuthApi, authenticatedAuthApi, RemoteErrorMapper(RemoteJson.json))
        )
    }

    private fun user() = User(
        id = "user-amina",
        email = "amina@tervyn.demo",
        firstName = "Amina",
        lastName = "Kabwe",
        jobTitle = "Technicienne terrain",
        avatarUrl = null,
        createdAt = now,
        updatedAt = now,
        lastSyncedAt = now
    )

    private class FakeDemoAuthGateway(
        private val result: AppResult<DemoAuthGateway.AuthenticatedDemoUser>
    ) : DemoAuthGateway {
        override suspend fun authenticate(email: String, password: String): AppResult<DemoAuthGateway.AuthenticatedDemoUser> {
            return result
        }
    }

    private class FakeLocalUserDataSource(
        private val users: Map<String, User>
    ) : LocalUserDataSource {
        override suspend fun getUser(userId: String): User? = users[userId]

        override suspend fun upsertUser(user: User): AppResult<Unit> = AppResult.Success(Unit)
    }

    private companion object {
        val now: Instant = Instant.parse("2026-09-09T10:00:00Z")
    }
}
