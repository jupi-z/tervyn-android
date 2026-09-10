package dev.amenokizele.tervyn.data.remote.job

import dev.amenokizele.tervyn.core.result.AppError
import dev.amenokizele.tervyn.core.result.AppResult
import dev.amenokizele.tervyn.data.remote.config.RemoteApiConfig
import dev.amenokizele.tervyn.data.remote.error.RemoteErrorMapper
import dev.amenokizele.tervyn.domain.model.JobStatus
import java.io.IOException
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import retrofit2.Response

class RetrofitRemoteJobDataSource @Inject constructor(
    private val config: RemoteApiConfig,
    private val jobsApi: JobsApi,
    private val errorMapper: RemoteErrorMapper
) : RemoteJobDataSource {
    override suspend fun fetchJobs(
        cursor: String?,
        limit: Int,
        updatedAfter: Instant?
    ): AppResult<RemoteJobsPage> {
        if (!config.enabled) return remoteDisabled()
        if (limit !in 1..100) return AppResult.Failure(AppError.Validation("invalid_jobs_limit"))
        return execute { jobsApi.jobs(cursor, limit, updatedAfter?.toString()) }
            .mapBody { it.toRemoteJobsPage() }
    }

    override suspend fun fetchJob(jobId: String): AppResult<RemoteJobSnapshot> {
        if (!config.enabled) return remoteDisabled()
        return execute { jobsApi.job(jobId) }.mapBody { it.toRemoteJobSnapshot() }
    }

    override suspend fun updateJobStatus(
        jobId: String,
        clientMutationId: String,
        serverVersion: Long,
        status: JobStatus,
        changedAt: Instant
    ): AppResult<RemoteJobMutationResult> {
        if (!config.enabled) return remoteDisabled()
        val body = JobStatusMutationRequestDto(
            status = status.name,
            startedAt = if (status == JobStatus.IN_PROGRESS) changedAt.toString() else null,
            completedAt = if (status == JobStatus.COMPLETED) changedAt.toString() else null
        )
        return execute {
            jobsApi.updateStatus(jobId, clientMutationId, serverVersion.ifMatch(), body)
        }.mapBody { it.toRemoteJobMutationResult() }
    }

    override suspend fun updateChecklistItem(
        jobId: String,
        itemId: String,
        clientMutationId: String,
        serverVersion: Long,
        completed: Boolean,
        completedAt: Instant?
    ): AppResult<RemoteChecklistItemSnapshot> {
        if (!config.enabled) return remoteDisabled()
        return execute {
            jobsApi.updateChecklistItem(
                jobId = jobId,
                itemId = itemId,
                idempotencyKey = clientMutationId,
                ifMatch = serverVersion.ifMatch(),
                body = ChecklistMutationRequestDto(completed = completed, completedAt = completedAt?.toString())
            )
        }.mapBody { it.toRemoteChecklistItemSnapshot() }
    }

    override suspend fun createNote(
        jobId: String,
        noteId: String,
        clientMutationId: String,
        content: String,
        createdAt: Instant
    ): AppResult<RemoteNoteSnapshot> {
        if (!config.enabled) return remoteDisabled()
        return execute {
            jobsApi.createNote(
                jobId = jobId,
                idempotencyKey = clientMutationId,
                body = NoteCreateRequestDto(id = noteId, content = content, createdAt = createdAt.toString())
            )
        }.mapBody { it.toRemoteNoteSnapshot() }
    }

    override suspend fun deleteAttachmentMetadata(
        jobId: String,
        attachmentId: String,
        clientMutationId: String,
        serverVersion: Long
    ): AppResult<Unit> {
        if (!config.enabled) return remoteDisabled()
        return execute {
            jobsApi.deleteAttachmentMetadata(jobId, attachmentId, clientMutationId, serverVersion.ifMatch())
        }.mapUnitBody()
    }

    private suspend fun <T> execute(call: suspend () -> Response<T>): AppResult<Response<T>> {
        return try {
            AppResult.Success(call())
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: IOException) {
            AppResult.Failure(errorMapper.map(exception))
        } catch (exception: SerializationException) {
            AppResult.Failure(errorMapper.map(exception))
        } catch (_: Exception) {
            AppResult.Failure(AppError.Network("invalid_server_response"))
        }
    }

    private fun <T, R> AppResult<Response<T>>.mapBody(mapper: (T) -> R): AppResult<R> {
        return when (this) {
            is AppResult.Failure -> AppResult.Failure(error)
            is AppResult.Success -> {
                val response = data
                if (!response.isSuccessful) {
                    return AppResult.Failure(errorMapper.map(response))
                }
                val body = response.body() ?: return invalidServerResponse()
                try {
                    AppResult.Success(mapper(body))
                } catch (exception: CancellationException) {
                    throw exception
                } catch (_: Exception) {
                    invalidServerResponse()
                }
            }
        }
    }

    private fun AppResult<Response<Unit>>.mapUnitBody(): AppResult<Unit> {
        return when (this) {
            is AppResult.Failure -> AppResult.Failure(error)
            is AppResult.Success -> {
                if (data.isSuccessful) AppResult.Success(Unit) else AppResult.Failure(errorMapper.map(data))
            }
        }
    }

    private fun Long.ifMatch(): String = "\"$this\""

    private fun <T> remoteDisabled(): AppResult<T> = AppResult.Failure(AppError.InvalidState("remote_api_disabled"))

    private fun <T> invalidServerResponse(): AppResult<T> = AppResult.Failure(AppError.Network("invalid_server_response"))
}
