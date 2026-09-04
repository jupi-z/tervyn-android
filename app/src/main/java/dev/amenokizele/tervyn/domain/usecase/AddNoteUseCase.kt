package dev.amenokizele.tervyn.domain.usecase

import dev.amenokizele.tervyn.domain.repository.JobRepository

class AddNoteUseCase(
    private val repository: JobRepository
) {
    suspend operator fun invoke(jobId: String, content: String) = repository.addNote(jobId, content)
}
