package dev.amenokizele.tervyn.data.remote.error

import dev.amenokizele.tervyn.core.result.AppError
import dev.amenokizele.tervyn.data.remote.auth.RemoteJson
import java.io.IOException
import kotlinx.serialization.SerializationException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.Response

class RemoteErrorMapperTest {
    private val mapper = RemoteErrorMapper(RemoteJson.json)

    @Test
    fun httpStatusCodesMapToStableAppErrorsWithoutServerMessages() {
        assertEquals(AppError.Validation("bad_request"), mapper.map(response(400)))
        assertEquals(AppError.Authentication("unauthorized"), mapper.map(response(401)))
        assertEquals(AppError.Authentication("forbidden"), mapper.map(response(403)))
        assertEquals(AppError.NotFound("remote", "/v1/jobs/missing"), mapper.map(response(404, path = "/v1/jobs/missing")))
        assertEquals(AppError.Conflict("conflict"), mapper.map(response(409)))
        assertEquals(AppError.Conflict("version_conflict"), mapper.map(response(412)))
        assertEquals(AppError.Validation("validation_failed"), mapper.map(response(422)))
        assertEquals(AppError.Network("rate_limited"), mapper.map(response(429)))
        assertEquals(AppError.Network("server_error"), mapper.map(response(500)))
    }

    @Test
    fun serverErrorCodeOverridesStatusFallbackWhenSafe() {
        val errorBody = """{"error":{"code":"invalid_credentials","message":"Do not expose me"}}"""

        assertEquals(AppError.Authentication("invalid_credentials"), mapper.map(response(401, errorBody)))
    }

    @Test
    fun ioAndMalformedJsonMapToNetworkErrors() {
        assertEquals(AppError.Network("network_unavailable"), mapper.map(IOException("offline")))
        assertEquals(AppError.Network("invalid_server_response"), mapper.map(SerializationException("bad json")))
    }

    @Test
    fun malformedErrorBodyFallsBackToStatusCode() {
        assertEquals(AppError.Authentication("unauthorized"), mapper.map(response(401, "{not-json")))
    }

    private fun response(
        code: Int,
        body: String = """{"error":{"message":"server text"}}""",
        path: String = "/"
    ): Response<Unit> {
        val responseBody = body.toResponseBody("application/json".toMediaType())
        val rawResponse = okhttp3.Response.Builder()
            .request(Request.Builder().url("https://api.tervyn.invalid$path").build())
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("HTTP $code")
            .build()
        return Response.error(responseBody, rawResponse)
    }
}
