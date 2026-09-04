package dev.amenokizele.tervyn.domain.usecase

import dev.amenokizele.tervyn.domain.repository.JobRepository

class ObserveJobUseCase(
    private val repository: JobRepository
) {
    operator fun invoke(jobId: String) = repository.observeJob(jobId)
}
