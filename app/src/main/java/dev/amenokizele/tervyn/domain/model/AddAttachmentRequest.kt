package dev.amenokizele.tervyn.domain.model

data class AddAttachmentRequest(
    val type: AttachmentType,
    val localUri: String?,
    val mimeType: String,
    val fileName: String?,
    val sizeBytes: Long,
    val checksumSha256: String?
)
