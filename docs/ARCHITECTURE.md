# Tervyn Android Architecture

## Status

Phase 2 implements the local offline data foundation inside the existing single `:app` module. Room is now the local source of truth for field data and the persistent local outbox.

Remote API, WorkManager queue processing, secure token storage, real authentication, remote photo upload, and server conflict resolution are not implemented.

## Dependency Rule

```text
Compose UI
   -> ViewModel
      -> Use Case
         -> Repository Contract
            <- Room Repository
               -> Room Database
                  -> Business tables
                  -> Persistent Outbox
```

The Domain layer remains pure Kotlin and independent from Android UI, Compose, Hilt, Room, and network libraries. Feature ViewModels continue to depend on use cases or small application contracts; they do not import Room entities, DAOs, or `TervynDatabase`.

## Packages

- `app/`: root application state, startup initialization contract, and app-level contracts.
- `core/result/`: `AppResult` and `AppError`.
- `core/time/`: clock and presentation date/time formatting utilities.
- `domain/model/`: business models and enums.
- `domain/repository/`: repository contracts.
- `domain/usecase/`: use cases for current application actions.
- `data/fixtures/`: local demo fixture source used only by the one-time Room seed.
- `data/inmemory/`: simulated auth, theme, and offline UI state only.
- `data/local/converter/`: Room type converters.
- `data/local/entity/`: Room entities and local outbox enums.
- `data/local/dao/`: focused DAOs for Room tables.
- `data/local/db/`: `TervynDatabase`.
- `data/local/mapper/`: Domain/Entity mapping.
- `data/local/relation/`: Room relation projections.
- `data/local/repository/`: Room-backed repository implementations.
- `data/local/seed/`: one-time local seed initialization.
- `di/`: Hilt bindings and providers.
- `feature/`: screens and ViewModels.
- `navigation/`: root, auth, and app navigation graphs.

## Local Source Of Truth

`RoomJobRepository` implements `JobRepository` and reads/writes jobs, checklist items, notes, and attachment metadata through Room. `observeJobs()` and `observeJob(jobId)` are backed by Room Flow queries, so UI state updates after local transactions without manual refresh.

`RoomSyncRepository` implements `SyncRepository` from the persistent outbox counters in `SyncOperationDao`. It reports local pending/failed operations and does not mark anything synchronized because no remote sync engine exists in Phase 2.

## Startup Initialization

`RoomDatabaseSeeder` implements `LocalDataInitializer`. `TervynAppViewModel` keeps the app in `AuthState.Checking` so Bootstrap remains visible until Room opens and the one-time seed completes.

## Explicit Non-Goals

- Remote API: NOT IMPLEMENTED.
- WorkManager queue processing: NOT IMPLEMENTED.
- Real authentication/session persistence: NOT IMPLEMENTED.
- Remote photo upload: NOT IMPLEMENTED.
- Server conflict resolution: NOT IMPLEMENTED.
