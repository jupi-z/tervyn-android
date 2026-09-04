package dev.amenokizele.tervyn.domain.usecase

import dev.amenokizele.tervyn.domain.repository.JobRepository

class ObserveJobsUseCase(
    private val repository: JobRepository
) {
    operator fun invoke() = repository.observeJobs()
}
