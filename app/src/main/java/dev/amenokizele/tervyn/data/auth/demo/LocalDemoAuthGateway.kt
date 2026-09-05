package dev.amenokizele.tervyn.data.auth.demo

import dev.amenokizele.tervyn.app.SimulationController
import dev.amenokizele.tervyn.core.result.AppError
import dev.amenokizele.tervyn.core.result.AppResult
import dev.amenokizele.tervyn.data.fixtures.TervynDemoFixtures
import javax.inject.Inject

class LocalDemoAuthGateway @Inject constructor(
    private val simulationController: SimulationController
) : DemoAuthGateway {
    override suspend fun authenticate(
        email: String,
        password: String
    ): AppResult<DemoAuthGateway.AuthenticatedDemoUser> {
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
            return AppResult.Failure(AppError.Validation("invalid_email"))
        }
        if (password.isBlank()) {
            return AppResult.Failure(AppError.Validation("empty_password"))
        }
        if (simulationController.isOffline.value) {
            return AppResult.Failure(AppError.InvalidState("offline_simulation"))
        }
        if (!cleanEmail.equals(DEMO_EMAIL, ignoreCase = true) || password != DEMO_PASSWORD) {
            return AppResult.Failure(AppError.Authentication("invalid_credentials"))
        }

        return AppResult.Success(
            DemoAuthGateway.AuthenticatedDemoUser(TervynDemoFixtures.CURRENT_USER_ID)
        )
    }

    companion object {
        const val DEMO_EMAIL = "amina@tervyn.demo"
        const val DEMO_PASSWORD = "tervyn2026"
    }
}
