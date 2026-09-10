package dev.amenokizele.tervyn.data.remote.auth

import dev.amenokizele.tervyn.data.auth.session.StoredSession
import dev.amenokizele.tervyn.domain.model.User
import java.time.Instant

fun UserDto.toDomainUser(lastSyncedAt: Instant? = null): User {
    require(id.isNotBlank())
    require(email.isNotBlank())
    require(firstName.isNotBlank())
    require(lastName.isNotBlank())
    return User(
        id = id,
        email = email,
        firstName = firstName,
        lastName = lastName,
        jobTitle = jobTitle,
        avatarUrl = avatarUrl,
        createdAt = createdAt?.let(Instant::parse),
        updatedAt = updatedAt?.let(Instant::parse),
        lastSyncedAt = lastSyncedAt ?: updatedAt?.let(Instant::parse)
    )
}

fun SessionDto.toStoredSession(userId: String): StoredSession = StoredSession(
    userId = userId,
    accessToken = accessToken.also { require(it.isNotBlank()) },
    refreshToken = refreshToken.also { require(it.isNotBlank()) },
    issuedAt = Instant.parse(issuedAt),
    accessTokenExpiresAt = Instant.parse(accessTokenExpiresAt),
    refreshTokenExpiresAt = Instant.parse(refreshTokenExpiresAt)
)

fun RefreshResponseDto.toStoredSession(userId: String): StoredSession = StoredSession(
    userId = userId,
    accessToken = accessToken.also { require(it.isNotBlank()) },
    refreshToken = refreshToken.also { require(it.isNotBlank()) },
    issuedAt = Instant.parse(issuedAt),
    accessTokenExpiresAt = Instant.parse(accessTokenExpiresAt),
    refreshTokenExpiresAt = Instant.parse(refreshTokenExpiresAt)
)
