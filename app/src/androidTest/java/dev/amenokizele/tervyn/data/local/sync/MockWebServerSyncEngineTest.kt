package dev.amenokizele.tervyn.data.local.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.amenokizele.tervyn.core.result.AppResult
import dev.amenokizele.tervyn.core.time.TervynClock
import dev.amenokizele.tervyn.data.auth.session.StoredSession
import dev.amenokizele.tervyn.data.local.db.TervynDatabase
import dev.amenokizele.tervyn.data.local.repository.LocalIdGenerator
import dev.amenokizele.tervyn.data.local.repository.RoomJobRepository
import dev.amenokizele.tervyn.data.remote.auth.RemoteJson
import dev.amenokizele.tervyn.data.remote.auth.SessionCoordinator
import dev.amenokizele.tervyn.data.remote.auth.SessionState
import dev.amenokizele.tervyn.data.remote.config.RemoteApiConfig
import dev.amenokizele.tervyn.data.remote.error.RemoteErrorMapper
import dev.amenokizele.tervyn.data.remote.job.JobsApi
import dev.amenokizele.tervyn.data.remote.job.RetrofitRemoteJobDataSource
import dev.amenokizele.tervyn.domain.model.JobPriority
import dev.amenokizele.tervyn.domain.model.JobStatus
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class MockWebServerSyncEngineTest {
    private lateinit var database: TervynDatabase
    private lateinit var server: MockWebServer
    private lateinit var repository: RoomJobRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, TervynDatabase::class.java).build()
        server = MockWebServer()
        server.start()
        repository = RoomJobRepository(database, FixedClock(), TestIdGenerator())
    }

    @After
    fun tearDown() {
        server.shutdown()
        database.close()
    }

    @Test
    fun pushThenPullUsesRealRetrofitHttpAndClearsOutbox() = runBlocking {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {"id":"job-1","serverVersion":11,"updatedAt":"2026-09-03T12:00:01Z","status":"IN_PROGRESS","startedAt":"2026-09-03T12:00:00Z","completedAt":null}
                    """.trimIndent()
                )
        )
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {"items":[{"id":"job-1","reference":"REF-1","title":"Remote title","description":null,"clientName":"Client","siteName":"Site","siteAddress":null,"priority":"NORMAL","status":"IN_PROGRESS","scheduledAt":"2026-09-03T12:00:00Z","startedAt":"2026-09-03T12:00:00Z","completedAt":null,"serverVersion":11,"createdAt":"2026-09-03T12:00:00Z","updatedAt":"2026-09-03T12:00:01Z","checklist":[],"notes":[],"attachments":[]}],"nextCursor":null,"serverTime":"2026-09-03T12:00:01Z"}
                    """.trimIndent()
                )
        )

        database.jobDao().insertJob(job())
        assertTrue(repository.startJob("job-1") is AppResult.Success)

        val config = RemoteApiConfig(true, server.url("/").toString(), allowHttpForTests = true)
        val api = Retrofit.Builder()
            .baseUrl(config.clientBaseUrl())
            .client(OkHttpClient())
            .addConverterFactory(RemoteJson.json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(JobsApi::class.java)
        val remote = RetrofitRemoteJobDataSource(config, api, RemoteErrorMapper(RemoteJson.json))
        val orchestrator = SyncOrchestrator(
            database = database,
            config = config,
            sessionCoordinator = ActiveSessionCoordinator(),
            outboxExecutor = OutboxExecutor(database, remote, FixedClock()),
            pullSynchronizer = RemotePullSynchronizer(database, remote, RemoteJobMerger(database, FixedClock()), FixedClock()),
            runtimeState = SyncRuntimeStateStore()
        )

        assertTrue(orchestrator.run(SyncRunTrigger.MANUAL) is SyncRunResult.Success)

        val mutation = server.takeRequest()
        assertEquals("PATCH", mutation.method)
        assertEquals("/v1/jobs/job-1/status", mutation.path)
        assertEquals("mutation-1", mutation.getHeader("Idempotency-Key"))
        assertEquals("\"1\"", mutation.getHeader("If-Match"))
        assertEquals("GET", server.takeRequest().method)
        assertEquals(0, database.syncOperationDao().countOutstanding())
        assertEquals(11L, database.jobDao().getJobById("job-1")?.serverVersion)
        assertEquals("Remote title", database.jobDao().getJobById("job-1")?.title)
    }

    private fun job() = dev.amenokizele.tervyn.data.local.entity.JobEntity(
        id = "job-1", reference = "REF-1", title = "Local title", description = null,
        clientName = "Client", siteName = "Site", siteAddress = null, priority = JobPriority.NORMAL,
        status = JobStatus.ASSIGNED, scheduledAt = NOW, startedAt = null, completedAt = null,
        serverVersion = 1, syncState = dev.amenokizele.tervyn.domain.model.SyncState.SYNCED,
        createdAt = NOW, updatedAt = NOW, lastSyncedAt = NOW
    )

    private class FixedClock : TervynClock {
        override fun now(): Instant = NOW
    }

    private class TestIdGenerator : LocalIdGenerator {
        private val ids = ArrayDeque(listOf("operation-1", "mutation-1"))
        override fun newId(): String = ids.removeFirst()
    }

    private class ActiveSessionCoordinator : SessionCoordinator {
        private val session = StoredSession("user-1", "access", "refresh", NOW, NOW.plusSeconds(3600), NOW.plusSeconds(7200))
        private val stateFlow = MutableStateFlow<SessionState>(SessionState.Active(session))
        override val state: StateFlow<SessionState> = stateFlow
        override suspend fun restore() = AppResult.Success(session)
        override suspend fun replace(session: StoredSession) = AppResult.Success(Unit)
        override suspend fun clear() = AppResult.Success(Unit)
        override fun snapshot(): StoredSession = session
    }

    private companion object {
        val NOW: Instant = Instant.parse("2026-09-03T12:00:00Z")
    }
}
