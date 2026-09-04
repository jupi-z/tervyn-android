package dev.amenokizele.tervyn.domain.usecase

import dev.amenokizele.tervyn.domain.repository.AuthRepository

class LoginUseCase(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String) = repository.login(email, password)
}
