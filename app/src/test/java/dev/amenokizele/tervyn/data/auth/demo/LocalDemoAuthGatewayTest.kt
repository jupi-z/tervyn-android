package dev.amenokizele.tervyn.data.auth.demo

import dev.amenokizele.tervyn.app.SimulationController
import dev.amenokizele.tervyn.core.result.AppError
import dev.amenokizele.tervyn.core.result.AppResult
import dev.amenokizele.tervyn.data.fixtures.TervynDemoFixtures
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class LocalDemoAuthGatewayTest {
    @Test
    fun validCredentialsReturnDemoUserWithTrimmedCaseInsensitiveEmail() = runTest {
        val gateway = LocalDemoAuthGateway(FakeSimulationController())

        val result = gateway.authenticate(" AMINA@TERVYN.DEMO ", "tervyn2026")

        assertEquals(
            AppResult.Success(DemoAuthGateway.AuthenticatedDemoUser(TervynDemoFixtures.CURRENT_USER_ID)),
            result
        )
    }

    @Test
    fun invalidEmailAndPasswordReturnAuthenticationFailure() = runTest {
        val gateway = LocalDemoAuthGateway(FakeSimulationController())

        val badEmail = gateway.authenticate("other@tervyn.demo", "tervyn2026")
        val badPassword = gateway.authenticate("amina@tervyn.demo", "wrong")

        assertEquals(AppResult.Failure(AppError.Authentication("invalid_credentials")), badEmail)
        assertEquals(AppResult.Failure(AppError.Authentication("invalid_credentials")), badPassword)
    }

    @Test
    fun malformedEmailAndEmptyPasswordKeepExistingValidationCodes() = runTest {
        val gateway = LocalDemoAuthGateway(FakeSimulationController())

        val badEmail = gateway.authenticate("bad-email", "tervyn2026")
        val emptyPassword = gateway.authenticate("amina@tervyn.demo", "")

        assertEquals(AppResult.Failure(AppError.Validation("invalid_email")), badEmail)
        assertEquals(AppResult.Failure(AppError.Validation("empty_password")), emptyPassword)
    }

    @Test
    fun offlineSimulationRefusesValidCredentials() = runTest {
        val gateway = LocalDemoAuthGateway(FakeSimulationController(isOffline = true))

        val result = gateway.authenticate("amina@tervyn.demo", "tervyn2026")

        assertEquals(AppResult.Failure(AppError.InvalidState("offline_simulation")), result)
    }

    private class FakeSimulationController(isOffline: Boolean = false) : SimulationController {
        private val offline = MutableStateFlow(isOffline)
        override val isOffline: StateFlow<Boolean> = offline

        override fun toggleOffline() {
            offline.value = !offline.value
        }
    }
}
