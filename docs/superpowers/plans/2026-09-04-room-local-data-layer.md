# Room Local Data Layer Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the Phase 1 in-memory field-data implementation with a Room-backed local source of truth and persistent outbox.

**Architecture:** Keep the current single `:app` module, Domain models, use cases, ViewModels, Hilt, and navigation. Add a `data/local` Room layer behind existing repository contracts, leaving simulated auth/theme/offline state in memory only where still intentional.

**Tech Stack:** Kotlin, Jetpack Compose, Hilt, StateFlow, Coroutines, Room runtime/ktx/compiler/testing, KSP.

**Spec:** `C:\Users\Ameno MonarQue\.codex\attachments\25f565f2-e824-451d-b7b6-67f05ef83805\goal-objective.md`

## Global Constraints

- Repository starts from `main` commit `60b34e59e214e10a8c0350ef49dea36ea03d5420`.
- Work on `feat/room-local-data-layer`, then merge to `main` only after gates are green.
- Keep a single Gradle module: `:app`.
- Room is the local source of truth for Jobs, checklist items, notes, attachments, and outbox.
- Do not implement Retrofit, OkHttp, WorkManager, DataStore, JWT, backend, Firebase, Gemini, CameraX full integration, network monitor, or real upload.
- Domain must contain no Room, Android, or `data.local` imports.
- UI and app layers must not import Room entities, DAOs, or `TervynDatabase`.
- No `fallbackToDestructiveMigration`, no `allowMainThreadQueries`, no disabled tests, no lint ignore-failures.
- Fixtures start as `SYNCED`; every local mutation writes `syncState = PENDING` and a persistent outbox row.
- Backup must be disabled or explicitly exclude the database.
- Final gates: `./gradlew --version`, `clean`, `assembleDebug`, `lint`, `testDebugUnitTest`, `assembleDebugAndroidTest`, and `connectedDebugAndroidTest` if a device exists.

---

### Task 1: Room Setup And First Failing Mapper/Converter Tests

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`
- Create: `app/src/test/java/dev/amenokizele/tervyn/data/local/converter/RoomConvertersTest.kt`
- Create: `app/src/test/java/dev/amenokizele/tervyn/data/local/mapper/LocalMapperTest.kt`

**Interfaces:**
- Produces: `RoomConverters` with `Instant` and enum converters; mapper function names consumed by later tasks.

- [ ] Write failing JVM tests for `Instant` round-trip, null `Instant`, enum converters, job mapper, child mappers, and soft-deleted attachment filtering.
- [ ] Run `./gradlew testDebugUnitTest`; expected failure is missing converter/mapper production classes.
- [ ] Add Room dependencies and KSP schema args only; do not add networking/background libraries.
- [ ] Implement minimal converters and mappers.
- [ ] Re-run `./gradlew testDebugUnitTest`; expected pass.
- [ ] Commit as `feat: add room local persistence mapping`.

### Task 2: Entities, Relations, Database, And Schema Export

**Files:**
- Create: `app/src/main/java/dev/amenokizele/tervyn/data/local/entity/*.kt`
- Create: `app/src/main/java/dev/amenokizele/tervyn/data/local/relation/JobWithDetails.kt`
- Create: `app/src/main/java/dev/amenokizele/tervyn/data/local/db/TervynDatabase.kt`
- Create: `app/schemas/dev.amenokizele.tervyn.data.local.db.TervynDatabase/1.json`

**Interfaces:**
- Consumes: converters and mappers from Task 1.
- Produces: Room schema v1 with `tervyn.db`, `exportSchema = true`, and required tables/indexes/FKs.

- [ ] Write Android compile-facing tests that instantiate the Room database schema with in-memory Room.
- [ ] Run `./gradlew assembleDebugAndroidTest`; expected failure is missing entities/database.
- [ ] Implement `UserEntity`, `JobEntity`, `ChecklistItemEntity`, `NoteEntity`, `AttachmentEntity`, `SyncOperationEntity`, and `LocalMetadataEntity`.
- [ ] Implement `JobWithDetails`.
- [ ] Implement `TervynDatabase` version `1`, `exportSchema = true`, no destructive migration fallback, no main-thread queries.
- [ ] Run `./gradlew assembleDebugAndroidTest` to generate and validate schema export.
- [ ] Commit as `feat: add room database schema`.

### Task 3: DAOs And Relation Tests

**Files:**
- Create: `app/src/main/java/dev/amenokizele/tervyn/data/local/dao/*.kt`
- Create: `app/src/androidTest/java/dev/amenokizele/tervyn/data/local/dao/RoomDaoTest.kt`

**Interfaces:**
- Consumes: Room entities/database.
- Produces: DAOs for User, Job, ChecklistItem, Note, Attachment, SyncOperation, and LocalMetadata.

- [ ] Write instrumented DAO tests for insert/observe job, relations, deterministic child ordering, FK cascade, unique job reference, and unique client mutation id.
- [ ] Run `./gradlew assembleDebugAndroidTest`; expected failure is missing DAO APIs.
- [ ] Implement required DAO queries with `@Transaction` for relation reads and focused DAOs per table.
- [ ] Run `./gradlew assembleDebugAndroidTest`; expected compile pass.
- [ ] Run `./gradlew connectedDebugAndroidTest` if a device is available; expected DAO tests pass.
- [ ] Commit as `feat: add room daos and relations`.

### Task 4: Transactional Room Repositories And Outbox

**Files:**
- Create: `app/src/main/java/dev/amenokizele/tervyn/data/local/repository/RoomJobRepository.kt`
- Create: `app/src/main/java/dev/amenokizele/tervyn/data/local/repository/RoomSyncRepository.kt`
- Modify: `app/src/main/java/dev/amenokizele/tervyn/di/RepositoryModule.kt`
- Modify: `app/src/main/java/dev/amenokizele/tervyn/data/inmemory/InMemoryStore.kt`
- Delete or move from production: legacy in-memory job repository file
- Delete or move from production: legacy in-memory sync repository file
- Create: `app/src/androidTest/java/dev/amenokizele/tervyn/data/local/repository/RoomJobRepositoryTest.kt`
- Create: `app/src/androidTest/java/dev/amenokizele/tervyn/data/local/repository/RoomSyncRepositoryTest.kt`

**Interfaces:**
- Consumes: DAOs/database/mappers.
- Produces: `JobRepository -> RoomJobRepository`, `SyncRepository -> RoomSyncRepository`, persistent outbox counters.

- [ ] Write repository tests for start, checklist toggle, add note, add attachment, soft delete, complete, completed read-only, full UUID ids, outbox writes, and failed remote sync retry.
- [ ] Write rollback test proving business mutation rolls back when outbox insert fails.
- [ ] Run `./gradlew assembleDebugAndroidTest`; expected failure is missing Room repositories.
- [ ] Implement repository transactions with `database.withTransaction`.
- [ ] Bind production repositories to Room implementations.
- [ ] Remove job/outbox state from `InMemoryStore`; retain only auth/theme/offline simulation state.
- [ ] Update unit fakes so Phase 1 ViewModel/use-case tests stay fast without production in-memory job storage.
- [ ] Run `./gradlew testDebugUnitTest` and `./gradlew assembleDebugAndroidTest`.
- [ ] Commit as `feat: persist tervyn jobs and local outbox`.

### Task 5: One-Time Seed And App Initialization

**Files:**
- Create: `app/src/main/java/dev/amenokizele/tervyn/data/local/seed/RoomDatabaseSeeder.kt`
- Create: `app/src/main/java/dev/amenokizele/tervyn/app/LocalDataInitializer.kt`
- Modify: `app/src/main/java/dev/amenokizele/tervyn/app/TervynAppViewModel.kt`
- Modify: `app/src/main/java/dev/amenokizele/tervyn/di/AppModule.kt` or `RepositoryModule.kt`
- Create: `app/src/androidTest/java/dev/amenokizele/tervyn/data/local/seed/RoomDatabaseSeederTest.kt`

**Interfaces:**
- Consumes: fixtures, Room DAOs/database, mappers.
- Produces: transactional, idempotent seed marker `demo_seed_version = 1` and app bootstrap wait.

- [ ] Write seed tests for first initialization, second initialization with no duplicates, and preserved seed marker.
- [ ] Write app initialization behavior test if current unit architecture supports it with fakes.
- [ ] Run tests; expected failure is missing seeder/initializer.
- [ ] Implement transactional seeder inserting current user and fixture jobs/children/attachments as `SYNCED`.
- [ ] Implement initializer and make `TervynAppViewModel` keep Bootstrap state until initialization completes.
- [ ] Re-run affected tests.
- [ ] Commit as `feat: seed room local demo data`.

### Task 6: Backup Policy, Sync Microcopy, Docs, And Final Gates

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/res/xml/backup_rules.xml`
- Modify: `app/src/main/res/xml/data_extraction_rules.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `README.md`
- Modify: `docs/ARCHITECTURE.md`
- Modify: `docs/DATA_MODEL.md`
- Create: `docs/DATABASE_MIGRATIONS.md`

**Interfaces:**
- Consumes: final Room implementation and current UI strings.
- Produces: accurate documentation and no false remote-sync claims.

- [ ] Configure conservative backup behavior so `tervyn.db` is not exported unintentionally.
- [ ] Update Sync microcopy to report local pending operations and remote sync not configured.
- [ ] Update README, architecture, data model, and migration docs to describe exactly what Phase 2 implements.
- [ ] Run final sanity searches for prohibited APIs/claims.
- [ ] Run all final gates.
- [ ] Push `feat/room-local-data-layer`.
- [ ] Merge cleanly to `main`, push `main`, or create PR if branch protection blocks direct push.
- [ ] Commit as `docs: document room local data layer`.

## Self-Review

- Spec coverage: covered Room setup, schema export, entities, DAOs, relations, mappers, Room repositories, persistent outbox, seeding, backup, docs, tests, final gates, branch/tag/merge.
- Placeholder scan: no TBD or deferred implementation placeholders in executable tasks.
- Type consistency: task interfaces consistently reference current Domain contracts and planned `data/local` package boundaries.
