package dev.amenokizele.tervyn.domain.usecase

import dev.amenokizele.tervyn.domain.repository.JobRepository

class CompleteJobUseCase(
    private val repository: JobRepository
) {
    suspend operator fun invoke(jobId: String) = repository.completeJob(jobId)
}
