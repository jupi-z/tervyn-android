package dev.amenokizele.tervyn.data.inmemory

import dev.amenokizele.tervyn.core.result.AppError
import dev.amenokizele.tervyn.core.result.AppResult
import dev.amenokizele.tervyn.data.fixtures.TervynDemoFixtures
import dev.amenokizele.tervyn.di.DefaultDispatcher
import dev.amenokizele.tervyn.domain.model.AuthState
import dev.amenokizele.tervyn.domain.model.User
import dev.amenokizele.tervyn.domain.repository.AuthRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InMemoryAuthRepository @Inject constructor(
    private val store: InMemoryStore,
    @DefaultDispatcher dispatcher: CoroutineDispatcher
) : AuthRepository {
    override val authState: Flow<AuthState> = store.authState

    init {
        CoroutineScope(SupervisorJob() + dispatcher).launch {
            delay(450)
            if (store.authStateValue == AuthState.Checking) {
                store.authStateValue = AuthState.Unauthenticated
            }
        }
    }

    override suspend fun login(email: String, password: String): AppResult<User> {
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
            return AppResult.Failure(AppError.Validation("invalid_email"))
        }
        if (password.isBlank()) {
            return AppResult.Failure(AppError.Validation("empty_password"))
        }
        if (store.isOfflineValue) {
            return AppResult.Failure(AppError.InvalidState("offline_simulation"))
        }
        val user = TervynDemoFixtures.currentUser.copy(email = cleanEmail)
        store.authStateValue = AuthState.Authenticated(user)
        return AppResult.Success(user)
    }

    override suspend fun logout(): AppResult<Unit> {
        store.authStateValue = AuthState.Unauthenticated
        return AppResult.Success(Unit)
    }
}
