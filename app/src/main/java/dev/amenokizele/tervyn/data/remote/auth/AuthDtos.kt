package dev.amenokizele.tervyn.data.remote.auth

import kotlinx.serialization.Serializable
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface PublicAuthApi {
    @POST("/v1/auth/login")
    suspend fun login(@Body request: LoginRequestDto): Response<LoginResponseDto>

    @POST("/v1/auth/refresh")
    fun refresh(@Body request: RefreshRequestDto): Call<RefreshResponseDto>
}

interface AuthenticatedAuthApi {
    @POST("/v1/auth/logout")
    suspend fun logout(@Body request: LogoutRequestDto): Response<Unit>

    @GET("/v1/me")
    suspend fun me(): Response<UserDto>
}

@Serializable
data class LoginRequestDto(
    val email: String,
    val password: String
)

@Serializable
data class LoginResponseDto(
    val user: UserDto,
    val session: SessionDto
)

@Serializable
data class RefreshRequestDto(
    val refreshToken: String
)

@Serializable
data class RefreshResponseDto(
    val accessToken: String,
    val refreshToken: String,
    val issuedAt: String,
    val accessTokenExpiresAt: String,
    val refreshTokenExpiresAt: String
)

@Serializable
data class LogoutRequestDto(
    val refreshToken: String
)

@Serializable
data class UserDto(
    val id: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val jobTitle: String? = null,
    val avatarUrl: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Serializable
data class SessionDto(
    val accessToken: String,
    val refreshToken: String,
    val issuedAt: String,
    val accessTokenExpiresAt: String,
    val refreshTokenExpiresAt: String
)
