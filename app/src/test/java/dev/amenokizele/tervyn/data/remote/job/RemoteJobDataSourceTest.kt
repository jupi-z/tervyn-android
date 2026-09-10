package dev.amenokizele.tervyn.data.remote.job

import dev.amenokizele.tervyn.core.result.AppError
import dev.amenokizele.tervyn.core.result.AppResult
import dev.amenokizele.tervyn.data.remote.auth.BearerTokenInterceptor
import dev.amenokizele.tervyn.data.remote.auth.FakeSessionCoordinator
import dev.amenokizele.tervyn.data.remote.auth.RemoteApiFactory
import dev.amenokizele.tervyn.data.remote.auth.RemoteJson
import dev.amenokizele.tervyn.data.remote.config.RemoteApiConfig
import dev.amenokizele.tervyn.data.remote.error.RemoteErrorMapper
import dev.amenokizele.tervyn.domain.model.JobPriority
import dev.amenokizele.tervyn.domain.model.JobStatus
import java.time.Instant
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class RemoteJobDataSourceTest {
    private lateinit var server: MockWebServer
    private lateinit var dataSource: RemoteJobDataSource

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        dataSource = newDataSource(enabled = true)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun disabledRemoteDataSourceReturnsInvalidStateWithoutHttpRequest() = runTest {
        val disabled = newDataSource(enabled = false)

        assertEquals(AppResult.Failure(AppError.InvalidState("remote_api_disabled")), disabled.fetchJobs())
        assertEquals(0, server.requestCount)
    }

    @Test
    fun fetchJobsMapsPageAndNestedSnapshotsWithoutWritingRoom() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(jobsPage()))

        val result = dataSource.fetchJobs(cursor = "next-1", limit = 2, updatedAfter = Instant.parse("2026-09-09T09:00:00Z"))

        val page = (result as AppResult.Success).data
        assertEquals("cursor-2", page.nextCursor)
        assertEquals(Instant.parse("2026-09-09T10:00:00Z"), page.serverTime)
        assertEquals(2, page.items.size)
        assertEquals(JobPriority.URGENT, page.items.first().priority)
        assertEquals(JobStatus.IN_PROGRESS, page.items.first().status)
        assertEquals(1, page.items.first().checklist.size)
        assertEquals(1, page.items.first().notes.size)
        assertEquals(1, page.items.first().attachments.size)

        val request = server.takeRequest()
        assertEquals("/v1/jobs?cursor=next-1&limit=2&updatedAfter=2026-09-09T09%3A00%3A00Z", request.path)
    }

    @Test
    fun fetchJobMapsDetailSnapshot() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(jobJson("job-1")))

        val result = dataSource.fetchJob("job-1")

        val job = (result as AppResult.Success).data
        assertEquals("job-1", job.id)
        assertEquals("/v1/jobs/job-1", server.takeRequest().path)
    }

    @Test
    fun unknownEnumMapsToInvalidServerResponseWithoutCrash() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(jobsPage(priority = "MEGA_CRITICAL")))

        assertEquals(AppResult.Failure(AppError.Network("invalid_server_response")), dataSource.fetchJobs())
    }

    @Test
    fun paginationLimitIsValidatedLocallyWithoutHttpRequest() = runTest {
        assertEquals(AppResult.Failure(AppError.Validation("invalid_jobs_limit")), dataSource.fetchJobs(limit = 0))
        assertEquals(AppResult.Failure(AppError.Validation("invalid_jobs_limit")), dataSource.fetchJobs(limit = 101))
        assertEquals(0, server.requestCount)
    }

    @Test
    fun mutationContractsSendIdempotencyAndIfMatchHeaders() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(mutationResponse(status = "IN_PROGRESS")))
        server.enqueue(MockResponse().setResponseCode(200).setBody(checklistResponse()))
        server.enqueue(MockResponse().setResponseCode(200).setBody(noteResponse()))
        server.enqueue(MockResponse().setResponseCode(204))

        dataSource.updateJobStatus("job-1", "mutation-status", 42, JobStatus.IN_PROGRESS, Instant.parse("2026-09-09T10:01:00Z"))
        dataSource.updateChecklistItem("job-1", "check-1", "mutation-check", 43, true, Instant.parse("2026-09-09T10:02:00Z"))
        dataSource.createNote("job-1", "note-1", "mutation-note", "content", Instant.parse("2026-09-09T10:03:00Z"))
        dataSource.deleteAttachmentMetadata("job-1", "att-1", "mutation-delete", 44)

        val status = server.takeRequest()
        assertEquals("PATCH", status.method)
        assertEquals("/v1/jobs/job-1/status", status.path)
        assertEquals("mutation-status", status.getHeader("Idempotency-Key"))
        assertEquals("\"42\"", status.getHeader("If-Match"))
        assertEquals("""{"status":"IN_PROGRESS","startedAt":"2026-09-09T10:01:00Z"}""", status.body.readUtf8())

        val checklist = server.takeRequest()
        assertEquals("/v1/jobs/job-1/checklist/check-1", checklist.path)
        assertEquals("mutation-check", checklist.getHeader("Idempotency-Key"))
        assertEquals("\"43\"", checklist.getHeader("If-Match"))

        val note = server.takeRequest()
        assertEquals("/v1/jobs/job-1/notes", note.path)
        assertEquals("mutation-note", note.getHeader("Idempotency-Key"))

        val delete = server.takeRequest()
        assertEquals("DELETE", delete.method)
        assertEquals("/v1/jobs/job-1/attachments/att-1", delete.path)
        assertEquals("mutation-delete", delete.getHeader("Idempotency-Key"))
        assertEquals("\"44\"", delete.getHeader("If-Match"))
    }

    private fun newDataSource(enabled: Boolean): RemoteJobDataSource {
        val config = RemoteApiConfig(enabled = enabled, baseUrl = server.url("/").toString(), allowHttpForTests = true)
        val coordinator = FakeSessionCoordinator.active("remote-token")
        val client = OkHttpClient.Builder()
            .addInterceptor(BearerTokenInterceptor(coordinator))
            .build()
        val jobsApi = RemoteApiFactory.createRetrofit(config, RemoteJson.json, client).create(JobsApi::class.java)
        return RetrofitRemoteJobDataSource(config, jobsApi, RemoteErrorMapper(RemoteJson.json))
    }

    private fun jobsPage(priority: String = "URGENT") = """
        {
          "items": [${jobJson("job-1", priority)}, ${jobJson("job-2", "NORMAL")}],
          "nextCursor": "cursor-2",
          "serverTime": "2026-09-09T10:00:00Z"
        }
    """.trimIndent()

    private fun jobJson(id: String, priority: String = "URGENT") = """
        {
          "id": "$id",
          "reference": "TRV-001",
          "title": "Installation routeur",
          "description": "Install",
          "clientName": "Alpha Distribution",
          "siteName": "Kolwezi Centre",
          "siteAddress": "Avenue Industrielle",
          "priority": "$priority",
          "status": "IN_PROGRESS",
          "scheduledAt": "2026-09-09T10:00:00Z",
          "startedAt": "2026-09-09T10:05:00Z",
          "completedAt": null,
          "serverVersion": 42,
          "createdAt": "2026-09-09T09:00:00Z",
          "updatedAt": "2026-09-09T10:05:00Z",
          "checklist": [{
            "id": "check-1",
            "jobId": "$id",
            "label": "Power",
            "position": 1,
            "required": true,
            "completed": true,
            "completedAt": "2026-09-09T10:06:00Z",
            "serverVersion": 5,
            "updatedAt": "2026-09-09T10:06:00Z"
          }],
          "notes": [{
            "id": "note-1",
            "jobId": "$id",
            "authorUserId": "user-remote",
            "content": "Done",
            "createdAt": "2026-09-09T10:07:00Z",
            "updatedAt": "2026-09-09T10:07:00Z",
            "serverVersion": 6,
            "deletedAt": null
          }],
          "attachments": [{
            "id": "att-1",
            "jobId": "$id",
            "authorUserId": "user-remote",
            "type": "PHOTO",
            "remoteUrl": "https://files.example/photo.jpg",
            "mimeType": "image/jpeg",
            "fileName": "photo.jpg",
            "sizeBytes": 1024,
            "checksumSha256": null,
            "serverVersion": 7,
            "createdAt": "2026-09-09T10:08:00Z",
            "uploadedAt": "2026-09-09T10:09:00Z",
            "deletedAt": null
          }]
        }
    """.trimIndent()

    private fun mutationResponse(status: String) = """
        {"id":"job-1","serverVersion":45,"updatedAt":"2026-09-09T10:10:00Z","status":"$status","startedAt":"2026-09-09T10:01:00Z","completedAt":null}
    """.trimIndent()

    private fun checklistResponse() = """{"id":"check-1","jobId":"job-1","label":"Power","position":1,"required":true,"completed":true,"completedAt":"2026-09-09T10:02:00Z","serverVersion":45,"updatedAt":"2026-09-09T10:02:00Z"}"""

    private fun noteResponse() = """{"id":"note-1","jobId":"job-1","authorUserId":"user-remote","content":"content","createdAt":"2026-09-09T10:03:00Z","updatedAt":"2026-09-09T10:03:00Z","serverVersion":45,"deletedAt":null}"""
}
