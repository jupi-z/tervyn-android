package dev.amenokizele.tervyn.data.auth.session

import dev.amenokizele.tervyn.core.result.AppResult

interface SecureSessionStore {
    suspend fun read(): AppResult<StoredSession?>

    suspend fun write(session: StoredSession): AppResult<Unit>

    suspend fun clear(): AppResult<Unit>
}
