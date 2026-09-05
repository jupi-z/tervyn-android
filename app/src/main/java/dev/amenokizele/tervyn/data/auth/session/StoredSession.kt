package dev.amenokizele.tervyn.data.auth.session

import java.time.Instant

data class StoredSession(
    val userId: String,
    val accessToken: String,
    val refreshToken: String,
    val issuedAt: Instant,
    val accessTokenExpiresAt: Instant,
    val refreshTokenExpiresAt: Instant,
    val schemaVersion: Int = SCHEMA_VERSION
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}
