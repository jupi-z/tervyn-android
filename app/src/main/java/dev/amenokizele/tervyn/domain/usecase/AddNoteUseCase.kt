package dev.amenokizele.tervyn.domain.usecase

import dev.amenokizele.tervyn.core.result.AppError
import dev.amenokizele.tervyn.core.result.AppResult
import dev.amenokizele.tervyn.domain.model.Note
import dev.amenokizele.tervyn.domain.repository.AuthRepository
import dev.amenokizele.tervyn.domain.repository.JobRepository

class AddNoteUseCase(
    private val repository: JobRepository,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(jobId: String, content: String): AppResult<Note> {
        val user = authRepository.currentAuthenticatedUser()
            ?: return AppResult.Failure(AppError.Authentication("not_authenticated"))

        return repository.addNote(
            jobId = jobId,
            authorUserId = user.id,
            content = content
        )
    }
}
