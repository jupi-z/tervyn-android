package dev.amenokizele.tervyn.domain.usecase

import dev.amenokizele.tervyn.core.result.AppError
import dev.amenokizele.tervyn.core.result.AppResult
import dev.amenokizele.tervyn.domain.model.AddAttachmentRequest
import dev.amenokizele.tervyn.domain.model.Attachment
import dev.amenokizele.tervyn.domain.repository.AuthRepository
import dev.amenokizele.tervyn.domain.repository.JobRepository

class AddAttachmentUseCase(
    private val repository: JobRepository,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(jobId: String, request: AddAttachmentRequest): AppResult<Attachment> {
        val user = authRepository.currentAuthenticatedUser()
            ?: return AppResult.Failure(AppError.Authentication("not_authenticated"))

        return repository.addAttachment(
            jobId = jobId,
            authorUserId = user.id,
            request = request
        )
    }
}
