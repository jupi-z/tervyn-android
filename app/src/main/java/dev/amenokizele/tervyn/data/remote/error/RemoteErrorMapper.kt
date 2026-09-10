package dev.amenokizele.tervyn.data.remote.error

import dev.amenokizele.tervyn.core.result.AppError
import java.io.IOException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.Response

class RemoteErrorMapper(
    private val json: Json
) {
    fun map(response: Response<*>): AppError {
        val serverCode = parseServerCode(response)
        return when (response.code()) {
            400 -> AppError.Validation(serverCode ?: "bad_request")
            401 -> AppError.Authentication(serverCode ?: "unauthorized")
            403 -> AppError.Authentication(serverCode ?: "forbidden")
            404 -> AppError.NotFound("remote", response.raw().request.url.encodedPath)
            409 -> AppError.Conflict(serverCode ?: "conflict")
            412 -> AppError.Conflict(serverCode ?: "version_conflict")
            422 -> AppError.Validation(serverCode ?: "validation_failed")
            429 -> AppError.Network("rate_limited")
            in 500..599 -> AppError.Network("server_error")
            else -> AppError.Unknown("remote_error")
        }
    }

    fun map(exception: IOException): AppError = AppError.Network("network_unavailable")

    fun map(exception: SerializationException): AppError = AppError.Network("invalid_server_response")

    fun mapUnexpected(): AppError = AppError.Unknown("remote_error")

    private fun parseServerCode(response: Response<*>): String? {
        val body = response.errorBody() ?: return null
        return try {
            val source = body.source()
            source.request(MAX_ERROR_BODY_BYTES + 1)
            val buffer = source.buffer.clone()
            if (buffer.size > MAX_ERROR_BODY_BYTES) return null
            json.decodeFromString<RemoteErrorEnvelopeDto>(buffer.readUtf8()).error?.code?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    private companion object {
        private const val MAX_ERROR_BODY_BYTES = 8_192L
    }
}
