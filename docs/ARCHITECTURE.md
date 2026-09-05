# Tervyn Android Architecture

## Status

Phase 3 implements the secure session and authentication foundation inside the existing single `:app` module. Room remains the local source of truth for field data and the persistent local outbox.

Remote API, WorkManager queue processing, remote authentication, remote photo upload, and server conflict resolution are not implemented.

## Dependency Rule

```text
Compose UI
   -> ViewModel
      -> Use Case
         -> Repository Contract
            <- Room Repository / Persistent Auth Repository
               -> Room Database / Android Keystore / DataStore
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
- `data/auth/`: local demo authentication, encrypted session storage, and persistent auth repository.
- `data/inmemory/`: simulated offline UI state only.
- `data/local/converter/`: Room type converters.
- `data/local/entity/`: Room entities and local outbox enums.
- `data/local/dao/`: focused DAOs for Room tables.
- `data/local/db/`: `TervynDatabase`.
- `data/local/mapper/`: Domain/Entity mapping.
- `data/local/relation/`: Room relation projections.
- `data/local/repository/`: Room-backed repository implementations.
- `data/local/seed/`: one-time local seed initialization.
- `data/preferences/`: Preferences DataStore-backed non-sensitive user preferences.
- `di/`: Hilt bindings and providers.
- `feature/`: screens and ViewModels.
- `navigation/`: root, auth, and app navigation graphs.

## Local Source Of Truth

`RoomJobRepository` implements `JobRepository` and reads/writes jobs, checklist items, notes, and attachment metadata through Room. `observeJobs()` and `observeJob(jobId)` are backed by Room Flow queries, so UI state updates after local transactions without manual refresh.

`RoomSyncRepository` implements `SyncRepository` from the persistent outbox counters in `SyncOperationDao`. It reports local pending/failed operations and does not mark anything synchronized because no remote sync engine exists in Phase 2.

## Authentication And Session

```text
Login UI
   -> LoginUseCase
      -> AuthRepository
         <- PersistentAuthRepository
            -> LocalDemoAuthGateway
            -> SecureSessionStore
               -> Android Keystore AES/GCM
               -> private encrypted SharedPreferences payload
            -> Room UserDao
```

`PersistentAuthRepository` owns the reactive `AuthState`, restores a valid encrypted session on startup, validates refresh/session expiry, resolves the authenticated user from Room, and clears the secure payload on logout. Credential verification is intentionally local demo-only in Phase 3.

Synthetic local demo tokens are opaque random values. They are not JWTs, are not sent over the network, and are stored only inside the encrypted session payload.

## Preferences

```text
Settings
   -> UserPreferencesRepository
      <- DataStoreUserPreferencesRepository
         -> Preferences DataStore
```

Only non-sensitive preferences are stored in DataStore. Phase 3 persists `theme_mode`; tokens and passwords are never written to DataStore.

## Startup Initialization

`RoomDatabaseSeeder` implements `LocalDataInitializer`. `TervynAppViewModel` exposes `LocalDataInitializationState` with `Initializing`, `Ready`, and `Error` states so Bootstrap remains the only visible surface until Room opens and the one-time seed completes.

Authentication and application navigation are activated only after local data reaches `Ready`. If initialization fails, Bootstrap shows a concise local-data error and a user-triggered retry action. Retry requests are ignored while an initialization attempt is already running. The root navigation guard continues to force Bootstrap whenever local data is not ready.

## Explicit Non-Goals

- Remote API: NOT IMPLEMENTED.
- WorkManager queue processing: NOT IMPLEMENTED.
- Remote authentication: NOT IMPLEMENTED.
- Server-issued tokens and token refresh endpoint: NOT IMPLEMENTED.
- Remote photo upload: NOT IMPLEMENTED.
- Server conflict resolution: NOT IMPLEMENTED.
