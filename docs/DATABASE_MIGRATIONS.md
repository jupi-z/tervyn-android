# Tervyn Database Migrations

## Current Version

- Database name: `tervyn.db`
- Current Room version: `2`
- Version `1` is the first official persistent local schema.
- Version `2` adds immutable outbox payload JSON, attachment server versions, and the due-operation index.

## Schema Export

Room schema export is enabled:

```text
app/schemas/dev.amenokizele.tervyn.data.local.db.TervynDatabase/1.json
app/schemas/dev.amenokizele.tervyn.data.local.db.TervynDatabase/2.json
```

This JSON file is versioned and must remain committed so future migration tests can validate upgrades such as `1 -> 2`.

## Migration Rule

Destructive migration fallback is not allowed.

Do not use:

```text
fallbackToDestructiveMigration()
fallbackToDestructiveMigrationOnDowngrade()
```

The `1 -> 2` migration is explicit, preserves existing rows, and is covered by `RoomMigrationSchemaTest`.

Future schema changes must provide explicit Room migrations and tests.

## Future Version Procedure

1. Increment `TervynDatabase` version.
2. Add an explicit `Migration(oldVersion, newVersion)` object.
3. Keep `exportSchema = true`.
4. Run schema export and commit the new JSON schema.
5. Add or update `MigrationTestHelper` tests for every supported upgrade path.
6. Verify `clean`, `assembleDebug`, `lint`, `testDebugUnitTest`, `assembleDebugAndroidTest`, and connected instrumented tests when a device is available.

## Backup And Restore Assumptions

Phase 2 disables Android backup with `android:allowBackup="false"` and also excludes `tervyn.db` in backup/data extraction rules. This is conservative because real authentication, session security, and operational data policy are not finalized yet.
