# Tervyn

Offline-first field operations for Android.

## Status

Remote API client foundation / Room-backed offline-first Android app.

The current app keeps Room as the UI source of truth and adds a real Retrofit/OkHttp client foundation for future remote integration. No public Tervyn backend is bundled or claimed. Remote mode is disabled by default, and the local demo login remains the default runnable mode.

## Current Stack

- Kotlin
- Jetpack Compose
- Material 3
- Navigation Compose
- ViewModel
- StateFlow
- Coroutines
- Hilt
- Room
- Retrofit
- OkHttp
- Kotlin Serialization
- Android Keystore
- Preferences DataStore
- KSP
- java.time domain timestamps with core library desugaring

## Implemented

- Room local persistence for jobs, checklist items, notes, attachments, and local sync operations.
- Room-backed `JobRepository` as the source of truth observed by the UI.
- Persistent local outbox rows via `SyncOperationEntity`.
- Transactional local mutations that write business data and outbox rows atomically.
- One-time local demo seed guarded by a persistent metadata marker.
- Startup initialization error handling with an explicit retry path.
- Secure session persistence with Android Keystore AES/GCM encryption and app-private AtomicFile storage.
- Session restore, expiry validation, logout, and a `SessionCoordinator` memory snapshot.
- Persistent theme preference via Preferences DataStore.
- Local demo credential verification for the default runnable flow.
- Remote API opt-in configuration through BuildConfig.
- Retrofit/OkHttp network stack with public and authenticated clients.
- Remote auth client for login, refresh, logout, and me endpoints.
- Bearer token injection from in-memory session state.
- Single-flight 401 refresh with one retry maximum.
- Remote job data source for remote snapshots and future mutation contracts.
- MockWebServer tests for remote auth, bearer/refresh, errors, and jobs contracts.
- Existing UI flow: Bootstrap, Login, Interventions, Detail, Execution, Notes, Photos, Completion, Sync, Settings, Logout.

## Simulated Or Deferred

- No public Tervyn backend is bundled or claimed.
- Remote mode is disabled by default.
- Default remote base URL is `https://tervyn.invalid/`.
- The local demo login remains the default runnable mode.
- Remote job snapshots are not yet merged into Room.
- Background synchronization is not implemented.
- WorkManager sync is not implemented.
- Persistent outbox execution is not implemented.
- Automatic pull/merge is not implemented.
- Remote photo upload is not implemented.
- Conflict resolution is not implemented.
- CameraX capture is not implemented.
- The offline toggle is a UI simulation, not a system network detector.

## Remote Configuration

Default values:

```text
TERVYN_REMOTE_API_ENABLED=false
TERVYN_API_BASE_URL=https://tervyn.invalid/
```

When remote mode is enabled, the production base URL must be valid, use HTTPS, include a host, and end with `/`.

## Local Data Layer

- Database file: `tervyn.db`.
- Room schema version: `1`.
- Schema export: `app/schemas/`.
- Destructive migrations are not enabled.
- The operational database, secure session storage, and preferences DataStore are excluded from backup rules. `allowBackup` is disabled.

## Build

Use the Gradle wrapper from the repository:

```bash
./gradlew clean
./gradlew assembleDebug
./gradlew lint
./gradlew testDebugUnitTest
./gradlew assembleDebugAndroidTest
```

On Windows:

```powershell
.\gradlew.bat clean
.\gradlew.bat assembleDebug
.\gradlew.bat lint
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebugAndroidTest
```

If a device or emulator is available:

```bash
./gradlew connectedDebugAndroidTest
```

## Documentation

- `docs/ARCHITECTURE.md`
- `docs/SECURITY.md`
- `docs/REMOTE_API.md`
- `docs/api/tervyn-api-v1.yaml`

## License

MIT License.

Copyright (c) 2026 Ameno Kizele Mwine
