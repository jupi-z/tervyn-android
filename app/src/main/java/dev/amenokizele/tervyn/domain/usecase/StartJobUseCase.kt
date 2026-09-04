package dev.amenokizele.tervyn.domain.usecase

import dev.amenokizele.tervyn.domain.repository.JobRepository

class StartJobUseCase(
    private val repository: JobRepository
) {
    suspend operator fun invoke(jobId: String) = repository.startJob(jobId)
}
