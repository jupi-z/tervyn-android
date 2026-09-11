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

    @Test
    fun versionOneDataSurvivesMigrationToVersionTwo() {
        val database = helper.createDatabase("migration-v1.db", 1)
        database.execSQL(
            """
            INSERT INTO jobs (
                id, reference, title, description, clientName, siteName, siteAddress,
                priority, status, scheduledAt, startedAt, completedAt, serverVersion,
                syncState, createdAt, updatedAt, lastSyncedAt
            ) VALUES ('job-1', 'REF-1', 'Job', NULL, 'Client', 'Site', NULL,
                'NORMAL', 'IN_PROGRESS', 1000, NULL, NULL, 1,
                'PENDING', 1000, 1000, NULL)
            """.trimIndent()
        )
        database.execSQL(
            """
            INSERT INTO attachments (
                id, jobId, authorUserId, type, localUri, remoteUrl, mimeType,
                fileName, sizeBytes, checksumSha256, syncState, createdAt,
                uploadedAt, deletedAt
            ) VALUES ('attachment-1', 'job-1', 'user-1', 'PHOTO', 'content://photo',
                NULL, 'image/jpeg', 'photo.jpg', 10, NULL, 'PENDING', 1000, NULL, NULL)
            """.trimIndent()
        )
        database.execSQL(
            """
            INSERT INTO sync_operations (
                id, entityType, entityId, operation, clientMutationId, status,
                attemptCount, lastErrorCode, lastErrorMessage, createdAt,
                lastAttemptAt, nextAttemptAt
            ) VALUES ('op-1', 'JOB', 'job-1', 'UPDATE', 'mutation-1', 'PENDING',
                0, NULL, NULL, 1000, NULL, NULL)
            """.trimIndent()
        )
        database.close()

        val migrated = helper.runMigrationsAndValidate(
            "migration-v1.db",
            2,
            true,
            MIGRATION_1_2
        )
        migrated.query("SELECT payloadJson FROM sync_operations WHERE id = 'op-1'").use { cursor ->
            check(cursor.moveToFirst())
            check(cursor.isNull(0))
        }
        migrated.query("SELECT serverVersion FROM attachments WHERE id = 'attachment-1'").use { cursor ->
            check(cursor.moveToFirst())
            check(cursor.isNull(0))
        }
        migrated.close()
    }
}
