package dev.amenokizele.tervyn.data.remote.auth

import dev.amenokizele.tervyn.core.result.AppError
import dev.amenokizele.tervyn.core.result.AppResult
import dev.amenokizele.tervyn.data.auth.session.StoredSession
import dev.amenokizele.tervyn.data.remote.error.RemoteErrorMapper
import java.io.IOException
import kotlinx.serialization.SerializationException

class RemoteTokenRefresher(
    private val authApi: PublicAuthApi,
    private val errorMapper: RemoteErrorMapper
) {
    fun refresh(currentSession: StoredSession): AppResult<StoredSession> {
        return try {
            val response = authApi.refresh(RefreshRequestDto(currentSession.refreshToken)).execute()
            if (!response.isSuccessful) {
                return AppResult.Failure(errorMapper.map(response))
            }
            val body = response.body() ?: return invalidServerResponse()
            AppResult.Success(body.toStoredSession(currentSession.userId))
        } catch (exception: IOException) {
            AppResult.Failure(errorMapper.map(exception))
        } catch (exception: SerializationException) {
            AppResult.Failure(errorMapper.map(exception))
        } catch (_: Exception) {
            invalidServerResponse()
        }
    }

    private fun invalidServerResponse(): AppResult.Failure {
        return AppResult.Failure(AppError.Network("invalid_server_response"))
    }
}
