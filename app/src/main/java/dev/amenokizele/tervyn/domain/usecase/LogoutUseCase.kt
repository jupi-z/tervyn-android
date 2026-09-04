package dev.amenokizele.tervyn.domain.usecase

import dev.amenokizele.tervyn.domain.repository.AuthRepository

class LogoutUseCase(
    private val repository: AuthRepository
) {
    suspend operator fun invoke() = repository.logout()
}
