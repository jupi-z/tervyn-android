package dev.amenokizele.tervyn.data.local.seed

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.amenokizele.tervyn.core.time.TervynClock
import dev.amenokizele.tervyn.data.fixtures.TervynDemoFixtures
import dev.amenokizele.tervyn.data.local.db.TervynDatabase
import dev.amenokizele.tervyn.domain.model.SyncState
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RoomDatabaseSeederTest {
    private lateinit var database: TervynDatabase
    private lateinit var seeder: RoomDatabaseSeeder

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, TervynDatabase::class.java).build()
        seeder = RoomDatabaseSeeder(database, FixedClock())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun firstInitializationSeedsUserJobsChildrenAndMarkerTransactionally() = runBlocking {
        seeder.seedIfNeeded()

        assertEquals(10, database.jobDao().countJobs())
        assertNotNull(database.userDao().getById(TervynDemoFixtures.CURRENT_USER_ID))
        assertEquals("1", database.localMetadataDao().get(RoomDatabaseSeeder.SEED_VERSION_KEY)?.value)
        assertTrue(database.jobDao().getJobWithDetails("job-003")?.job?.syncState == SyncState.SYNCED)
        assertTrue(database.jobDao().getJobWithDetails("job-007")?.job?.syncState == SyncState.SYNCED)
    }

    @Test
    fun secondInitializationDoesNotDuplicateFixturesOrRewriteSeedMarker() = runBlocking {
        seeder.seedIfNeeded()
        val firstMarker = database.localMetadataDao().get(RoomDatabaseSeeder.SEED_VERSION_KEY)

        seeder.seedIfNeeded()

        assertEquals(10, database.jobDao().countJobs())
        assertEquals(firstMarker, database.localMetadataDao().get(RoomDatabaseSeeder.SEED_VERSION_KEY))
    }

    private class FixedClock : TervynClock {
        override fun now(): Instant = Instant.parse("2026-09-04T12:00:00Z")
    }
}
