package dev.amenokizele.tervyn.domain.usecase

import dev.amenokizele.tervyn.domain.model.AddAttachmentRequest
import dev.amenokizele.tervyn.domain.repository.JobRepository

class AddAttachmentUseCase(
    private val repository: JobRepository
) {
    suspend operator fun invoke(jobId: String, request: AddAttachmentRequest) =
        repository.addAttachment(jobId, request)
}
