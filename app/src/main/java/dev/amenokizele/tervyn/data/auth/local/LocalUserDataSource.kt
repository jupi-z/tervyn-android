package dev.amenokizele.tervyn.data.auth.local

import dev.amenokizele.tervyn.domain.model.User

interface LocalUserDataSource {
    suspend fun getUser(userId: String): User?
}
