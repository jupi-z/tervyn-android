package dev.amenokizele.tervyn.domain.usecase

import dev.amenokizele.tervyn.domain.repository.AuthRepository

class ObserveAuthStateUseCase(
    private val repository: AuthRepository
) {
    operator fun invoke() = repository.authState
}
