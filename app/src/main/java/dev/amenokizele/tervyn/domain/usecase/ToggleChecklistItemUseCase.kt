package dev.amenokizele.tervyn.domain.usecase

import dev.amenokizele.tervyn.domain.repository.JobRepository

class ToggleChecklistItemUseCase(
    private val repository: JobRepository
) {
    suspend operator fun invoke(jobId: String, checklistItemId: String) =
        repository.toggleChecklistItem(jobId, checklistItemId)
}
