# Tervyn

Offline-first field operations for Android.

## Status

Secure session foundation / Room-backed Android app.

The current app keeps the validated Compose front-end, Room local data layer, and navigation guard while adding secure local session persistence and persistent non-sensitive preferences. Remote authentication, APIs, background sync, upload, and conflict resolution are intentionally not implemented yet.

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
- Android Keystore
- Preferences DataStore
- KSP
- java.time domain timestamps with core library desugaring

## Implemented

- Room local persistence.
- Room as the source of truth for jobs, checklist items, notes, attachments, and local sync operations.
- Persistent jobs, checklist, notes, and attachment metadata.
- Persistent local outbox via `SyncOperationEntity`.
- Transactional local mutations that write business data and outbox rows atomically.
- Domain to Entity mapping and Entity to Domain mapping.
- One-time local demo seed guarded by a persistent metadata marker.
- Startup initialization error handling with an explicit retry path.
- Secure local session persistence with Android Keystore AES/GCM encryption and app-private AtomicFile storage.
- Session restore, expiry validation, and destructive logout.
- Persistent theme preference via Preferences DataStore.
- Local demo credential verification for the existing prototype flow.
- Simulated offline toggle for the existing prototype flow.
- Existing UI flow: Bootstrap, Login, Interventions, Detail, Execution, Notes, Photos, Completion, Sync, Settings, Logout.

## Simulated Or Not Implemented

- Credential verification is local demo only.
- Access and refresh tokens are synthetic local demo tokens.
- The offline toggle is a UI simulation, not a system network detector.
- Remote authentication is not implemented.
- Server-issued tokens are not implemented.
- Token refresh endpoint is not implemented.
- Remote API is not implemented.
- Network synchronization is not implemented.
- WorkManager queue processing is not implemented.
- Remote photo upload is not implemented.
- Server conflict resolution is not implemented.
- CameraX capture is not implemented.

## Local Data Layer

- Database file: `tervyn.db`.
- Room schema version: `1`.
- Schema export: `app/schemas/`.
- Destructive migrations are not enabled.
- The operational database, secure session storage, and preferences DataStore are excluded from backup rules. `allowBackup` is disabled.

## Session Security

- Secure session storage uses an Android Keystore AES/GCM key with alias `tervyn.session.aes.v2`.
- The encrypted app-private AtomicFile envelope contains the local demo session record: `userId`, synthetic opaque tokens, issue time, access expiry, refresh/session expiry, and schema version.
- Session writes and clears are committed atomically. Logout writes a durable `EMPTY` tombstone instead of deleting the file as the primary clear mechanism.
- Upgrading from v0.4.1 invalidates the previous local demo session once; the user reconnects through the local demo login flow.
- Passwords are never persisted.
- Session persistence is real, but the credential source remains local demo-only until the remote API phase.
- See `docs/SECURITY.md` for the Phase 3 threat scope and limitations.

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

## License

MIT License.

Copyright (c) 2026 Ameno Kizele Mwine
