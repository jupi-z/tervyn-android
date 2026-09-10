package dev.amenokizele.tervyn.data.remote.auth

import dev.amenokizele.tervyn.core.result.AppResult
import dev.amenokizele.tervyn.data.auth.session.SecureSessionStore
import dev.amenokizele.tervyn.data.auth.session.StoredSession
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed interface SessionState {
    data object Uninitialized : SessionState
    data object Empty : SessionState
    data class Active(val session: StoredSession) : SessionState
}

interface SessionCoordinator {
    val state: StateFlow<SessionState>

    suspend fun restore(): AppResult<StoredSession?>

    suspend fun replace(session: StoredSession): AppResult<Unit>

    suspend fun clear(): AppResult<Unit>

    suspend fun markEmptyAfterRemoteInvalidation()

    fun snapshot(): StoredSession?
}

@Singleton
class SecureSessionCoordinator @Inject constructor(
    private val secureSessionStore: SecureSessionStore
) : SessionCoordinator {
    private val mutex = Mutex()
    private val mutableState = MutableStateFlow<SessionState>(SessionState.Uninitialized)
    override val state: StateFlow<SessionState> = mutableState.asStateFlow()

    override suspend fun restore(): AppResult<StoredSession?> = mutex.withLock {
        when (val result = secureSessionStore.read()) {
            is AppResult.Success -> {
                mutableState.value = result.data?.let(SessionState::Active) ?: SessionState.Empty
                result
            }

            is AppResult.Failure -> {
                mutableState.value = SessionState.Empty
                result
            }
        }
    }

    override suspend fun replace(session: StoredSession): AppResult<Unit> = mutex.withLock {
        when (val result = secureSessionStore.write(session)) {
            is AppResult.Success -> {
                mutableState.value = SessionState.Active(session)
                result
            }

            is AppResult.Failure -> result
        }
    }

    override suspend fun clear(): AppResult<Unit> = mutex.withLock {
        when (val result = secureSessionStore.clear()) {
            is AppResult.Success -> {
                mutableState.value = SessionState.Empty
                result
            }

            is AppResult.Failure -> result
        }
    }

    override suspend fun markEmptyAfterRemoteInvalidation() = mutex.withLock {
        mutableState.value = SessionState.Empty
    }

    override fun snapshot(): StoredSession? {
        return (mutableState.value as? SessionState.Active)?.session
    }
}
