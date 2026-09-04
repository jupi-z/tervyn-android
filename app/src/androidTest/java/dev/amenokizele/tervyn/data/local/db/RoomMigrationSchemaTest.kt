package dev.amenokizele.tervyn.data.local.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test

class RoomMigrationSchemaTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        TervynDatabase::class.java
    )

    @Test
    fun exportedVersionOneSchemaCanCreateDatabaseForFutureMigrationTests() {
        helper.createDatabase(TervynDatabase.DATABASE_NAME, 1).close()
    }
}
