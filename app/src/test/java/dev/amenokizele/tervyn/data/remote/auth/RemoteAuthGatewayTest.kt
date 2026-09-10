package dev.amenokizele.tervyn.data.remote.auth

import dev.amenokizele.tervyn.core.result.AppError
import dev.amenokizele.tervyn.core.result.AppResult
import dev.amenokizele.tervyn.data.auth.session.StoredSession
import dev.amenokizele.tervyn.data.remote.config.RemoteApiConfig
import dev.amenokizele.tervyn.data.remote.error.RemoteErrorMapper
import dev.amenokizele.tervyn.domain.model.User
import java.time.Instant
import okhttp3.OkHttpClient
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RemoteAuthGatewayTest {
    private lateinit var server: MockWebServer
    private lateinit var gateway: RemoteAuthGateway

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val config = RemoteApiConfig(enabled = true, baseUrl = server.url("/").toString(), allowHttpForTests = true)
        gateway = RemoteAuthGateway(
            publicAuthApi = RemoteApiFactory.createPublicRetrofit(config, RemoteJson.json).create(PublicAuthApi::class.java),
            authenticatedAuthApi = RemoteApiFactory.createPublicRetrofit(config, RemoteJson.json).create(AuthenticatedAuthApi::class.java),
            errorMapper = RemoteErrorMapper(RemoteJson.json)
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun loginSuccessMapsRemoteUserAndOpaqueSessionWithoutPersistingPassword() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(loginResponse()))

        val result = gateway.login("agent@example.com", "secret")

        val expectedUser = User(
            id = "user-remote",
            email = "agent@example.com",
            firstName = "Amina",
            lastName = "Kabwe",
            jobTitle = "Technicienne terrain",
            avatarUrl = null,
            createdAt = Instant.parse("2026-09-09T10:00:00Z"),
            updatedAt = Instant.parse("2026-09-09T10:00:00Z"),
            lastSyncedAt = Instant.parse("2026-09-09T10:00:00Z")
        )
        val expectedSession = StoredSession(
            userId = "user-remote",
            accessToken = "opaque-access-token",
            refreshToken = "opaque-refresh-token",
            issuedAt = Instant.parse("2026-09-09T10:00:00Z"),
            accessTokenExpiresAt = Instant.parse("2026-09-09T10:15:00Z"),
            refreshTokenExpiresAt = Instant.parse("2026-09-16T10:00:00Z")
        )
        assertEquals(AppResult.Success(AuthGatewayResult(expectedUser, expectedSession)), result)

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/v1/auth/login", request.path)
        assertEquals("""{"email":"agent@example.com","password":"secret"}""", request.body.readUtf8())
    }

    @Test
    fun invalidCredentialsMapToAuthenticationError() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(401)
                .setBody("""{"error":{"code":"invalid_credentials","message":"Bad credentials"}}""")
        )

        assertEquals(AppResult.Failure(AppError.Authentication("invalid_credentials")), gateway.login("agent@example.com", "bad"))
    }

    @Test
    fun malformedLoginResponseMapsToInvalidServerResponse() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(loginResponse(accessToken = "")))

        assertEquals(AppResult.Failure(AppError.Network("invalid_server_response")), gateway.login("agent@example.com", "secret"))
    }

    @Test
    fun revokeUsesAuthenticatedClientWithBearerTokenAndRefreshTokenBody() = runTest {
        val session = StoredSession(
            userId = "user-remote",
            accessToken = "logout-access-token",
            refreshToken = "logout-refresh-token",
            issuedAt = Instant.parse("2026-09-09T10:00:00Z"),
            accessTokenExpiresAt = Instant.parse("2026-09-09T10:15:00Z"),
            refreshTokenExpiresAt = Instant.parse("2026-09-16T10:00:00Z")
        )
        val config = RemoteApiConfig(enabled = true, baseUrl = server.url("/").toString(), allowHttpForTests = true)
        val publicApi = RemoteApiFactory.createPublicRetrofit(config, RemoteJson.json).create(PublicAuthApi::class.java)
        val authenticatedClient = OkHttpClient.Builder()
            .addInterceptor(BearerTokenInterceptor(FakeSessionCoordinator(session)))
            .build()
        val authenticatedApi = RemoteApiFactory.createRetrofit(config, RemoteJson.json, authenticatedClient)
            .create(AuthenticatedAuthApi::class.java)
        val gateway = RemoteAuthGateway(publicApi, authenticatedApi, RemoteErrorMapper(RemoteJson.json))
        server.enqueue(MockResponse().setResponseCode(204))

        assertEquals(AppResult.Success(Unit), gateway.revoke(session))

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/v1/auth/logout", request.path)
        assertEquals("Bearer logout-access-token", request.getHeader("Authorization"))
        assertEquals("""{"refreshToken":"logout-refresh-token"}""", request.body.readUtf8())
    }

    @Test
    fun publicAuthApiDoesNotExposeAuthenticatedEndpoints() {
        val publicMethods = PublicAuthApi::class.java.methods.map { it.name }.toSet()

        assertEquals(setOf("login", "refresh"), publicMethods)
    }

    private fun loginResponse(accessToken: String = "opaque-access-token"): String = """
        {
          "user": {
            "id": "user-remote",
            "email": "agent@example.com",
            "firstName": "Amina",
            "lastName": "Kabwe",
            "jobTitle": "Technicienne terrain",
            "avatarUrl": null,
            "createdAt": "2026-09-09T10:00:00Z",
            "updatedAt": "2026-09-09T10:00:00Z"
          },
          "session": {
            "accessToken": "$accessToken",
            "refreshToken": "opaque-refresh-token",
            "issuedAt": "2026-09-09T10:00:00Z",
            "accessTokenExpiresAt": "2026-09-09T10:15:00Z",
            "refreshTokenExpiresAt": "2026-09-16T10:00:00Z"
          }
        }
    """.trimIndent()
}
