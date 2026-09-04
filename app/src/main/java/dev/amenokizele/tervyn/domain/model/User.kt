package dev.amenokizele.tervyn.domain.model

import java.time.Instant

data class User(
    val id: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val jobTitle: String?,
    val avatarUrl: String?,
    val createdAt: Instant?,
    val updatedAt: Instant?,
    val lastSyncedAt: Instant?
) {
    val displayName: String
        get() = "$firstName $lastName".trim()
}
