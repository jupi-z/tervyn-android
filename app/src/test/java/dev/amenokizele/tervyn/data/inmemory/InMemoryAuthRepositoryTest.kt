package dev.amenokizele.tervyn.data.inmemory

import dev.amenokizele.tervyn.core.result.AppResult
import dev.amenokizele.tervyn.domain.model.AuthState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InMemoryAuthRepositoryTest {
    @Test
    fun authState_emitsCheckingThenUnauthenticated() = runTest {
        val store = InMemoryStore()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val repository = InMemoryAuthRepository(store, dispatcher)

        assertEquals(AuthState.Checking, repository.authState.first())
        advanceTimeBy(450)
        runCurrent()
        assertEquals(AuthState.Unauthenticated, store.authStateValue)
    }

    @Test
    fun loginSuccess_invalidLogin_andLogoutUpdateAuthState() = runTest {
        val store = InMemoryStore()
        val repository = InMemoryAuthRepository(store, StandardTestDispatcher(testScheduler))

        assertTrue(repository.login("bad-email", "tervyn2026") is AppResult.Failure)
        assertTrue(repository.login("amina@tervyn.demo", "") is AppResult.Failure)

        val login = repository.login("amina@tervyn.demo", "tervyn2026")
        assertTrue(login is AppResult.Success)
        assertTrue(store.authStateValue is AuthState.Authenticated)

        assertTrue(repository.logout() is AppResult.Success)
        assertEquals(AuthState.Unauthenticated, store.authStateValue)
    }
}
