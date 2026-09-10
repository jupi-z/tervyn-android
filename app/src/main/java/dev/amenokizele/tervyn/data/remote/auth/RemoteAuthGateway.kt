package dev.amenokizele.tervyn.data.remote.auth

import dev.amenokizele.tervyn.core.result.AppError
import dev.amenokizele.tervyn.core.result.AppResult
import dev.amenokizele.tervyn.data.auth.session.StoredSession
import dev.amenokizele.tervyn.data.remote.error.RemoteErrorMapper
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException

class RemoteAuthGateway @Inject constructor(
    private val publicAuthApi: PublicAuthApi,
    private val revocationAuthApi: RevocationAuthApi,
    private val authenticatedAuthApi: AuthenticatedAuthApi,
    private val errorMapper: RemoteErrorMapper
) : AuthGateway {
    override suspend fun login(email: String, password: String): AppResult<AuthGatewayResult> {
        return try {
            val response = publicAuthApi.login(LoginRequestDto(email = email.trim(), password = password))
            if (!response.isSuccessful) {
                return AppResult.Failure(errorMapper.map(response))
            }
            val body = response.body() ?: return invalidServerResponse()
            val user = body.user.toDomainUser()
            val session = body.session.toStoredSession(user.id)
            AppResult.Success(AuthGatewayResult(user, session))
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: IOException) {
            AppResult.Failure(errorMapper.map(exception))
        } catch (exception: SerializationException) {
            AppResult.Failure(errorMapper.map(exception))
        } catch (exception: Exception) {
            invalidServerResponse()
        }
    }

    override suspend fun revoke(session: StoredSession): AppResult<Unit> {
        return try {
            val response = revocationAuthApi.logout(
                authorization = "Bearer ${session.accessToken}",
                request = LogoutRequestDto(session.refreshToken)
            )
            if (response.isSuccessful) AppResult.Success(Unit) else AppResult.Failure(errorMapper.map(response))
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: IOException) {
            AppResult.Failure(errorMapper.map(exception))
        } catch (exception: SerializationException) {
            AppResult.Failure(errorMapper.map(exception))
        } catch (_: Exception) {
            AppResult.Failure(AppError.Unknown("remote_error"))
        }
    }

    private fun invalidServerResponse(): AppResult.Failure {
        return AppResult.Failure(AppError.Network("invalid_server_response"))
    }
}
