package dev.amenokizele.tervyn.domain.usecase

import dev.amenokizele.tervyn.domain.repository.JobRepository

class DeleteAttachmentUseCase(
    private val repository: JobRepository
) {
    suspend operator fun invoke(jobId: String, attachmentId: String) =
        repository.deleteAttachment(jobId, attachmentId)
}
