package dev.amenokizele.tervyn.domain.usecase

import dev.amenokizele.tervyn.domain.repository.SyncRepository

class ObserveSyncOverviewUseCase(
    private val repository: SyncRepository
) {
    operator fun invoke() = repository.overview
}
