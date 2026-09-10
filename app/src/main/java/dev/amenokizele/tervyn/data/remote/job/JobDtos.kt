package dev.amenokizele.tervyn.data.remote.job

import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface JobsApi {
    @GET("/v1/jobs")
    suspend fun jobs(
        @Query("cursor") cursor: String?,
        @Query("limit") limit: Int,
        @Query("updatedAfter") updatedAfter: String?
    ): Response<JobsPageDto>

    @GET("/v1/jobs/{jobId}")
    suspend fun job(@Path("jobId") jobId: String): Response<JobDto>

    @PATCH("/v1/jobs/{jobId}/status")
    suspend fun updateStatus(
        @Path("jobId") jobId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Header("If-Match") ifMatch: String,
        @Body body: JobStatusMutationRequestDto
    ): Response<JobMutationResponseDto>

    @PATCH("/v1/jobs/{jobId}/checklist/{itemId}")
    suspend fun updateChecklistItem(
        @Path("jobId") jobId: String,
        @Path("itemId") itemId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Header("If-Match") ifMatch: String,
        @Body body: ChecklistMutationRequestDto
    ): Response<ChecklistItemDto>

    @POST("/v1/jobs/{jobId}/notes")
    suspend fun createNote(
        @Path("jobId") jobId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: NoteCreateRequestDto
    ): Response<NoteDto>

    @DELETE("/v1/jobs/{jobId}/attachments/{attachmentId}")
    suspend fun deleteAttachmentMetadata(
        @Path("jobId") jobId: String,
        @Path("attachmentId") attachmentId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Header("If-Match") ifMatch: String
    ): Response<Unit>
}

@Serializable
data class JobsPageDto(
    val items: List<JobDto>,
    val nextCursor: String? = null,
    val serverTime: String
)

@Serializable
data class JobDto(
    val id: String,
    val reference: String,
    val title: String,
    val description: String? = null,
    val clientName: String,
    val siteName: String,
    val siteAddress: String? = null,
    val priority: String,
    val status: String,
    val scheduledAt: String,
    val startedAt: String? = null,
    val completedAt: String? = null,
    val serverVersion: Long,
    val createdAt: String,
    val updatedAt: String,
    val checklist: List<ChecklistItemDto> = emptyList(),
    val notes: List<NoteDto> = emptyList(),
    val attachments: List<AttachmentDto> = emptyList()
)

@Serializable
data class ChecklistItemDto(
    val id: String,
    val jobId: String,
    val label: String,
    val position: Int,
    val required: Boolean,
    val completed: Boolean,
    val completedAt: String? = null,
    val serverVersion: Long,
    val updatedAt: String
)

@Serializable
data class NoteDto(
    val id: String,
    val jobId: String,
    val authorUserId: String,
    val content: String,
    val createdAt: String,
    val updatedAt: String,
    val serverVersion: Long? = null,
    val deletedAt: String? = null
)

@Serializable
data class AttachmentDto(
    val id: String,
    val jobId: String,
    val authorUserId: String,
    val type: String,
    val remoteUrl: String? = null,
    val mimeType: String,
    val fileName: String? = null,
    val sizeBytes: Long,
    val checksumSha256: String? = null,
    val serverVersion: Long? = null,
    val createdAt: String,
    val uploadedAt: String? = null,
    val deletedAt: String? = null
)

@Serializable
data class JobStatusMutationRequestDto(
    val status: String,
    val startedAt: String? = null,
    val completedAt: String? = null
)

@Serializable
data class JobMutationResponseDto(
    val id: String,
    val serverVersion: Long,
    val updatedAt: String,
    val status: String,
    val startedAt: String? = null,
    val completedAt: String? = null
)

@Serializable
data class ChecklistMutationRequestDto(
    val completed: Boolean,
    val completedAt: String? = null
)

@Serializable
data class NoteCreateRequestDto(
    val id: String,
    val content: String,
    val createdAt: String
)
