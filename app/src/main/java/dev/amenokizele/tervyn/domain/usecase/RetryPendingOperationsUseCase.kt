package dev.amenokizele.tervyn.domain.usecase

import dev.amenokizele.tervyn.domain.repository.SyncRepository

class RetryPendingOperationsUseCase(
    private val repository: SyncRepository
) {
    suspend operator fun invoke() = repository.retryPendingOperations()
}
