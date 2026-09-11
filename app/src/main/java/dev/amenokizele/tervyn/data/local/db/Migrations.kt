package dev.amenokizele.tervyn.data.local.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE sync_operations ADD COLUMN payloadJson TEXT")
        db.execSQL("ALTER TABLE attachments ADD COLUMN serverVersion INTEGER")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_sync_operations_status_nextAttemptAt_createdAt " +
                "ON sync_operations(status, nextAttemptAt, createdAt)"
        )
    }
}
