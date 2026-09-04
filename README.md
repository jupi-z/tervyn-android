# Tervyn

Offline-first field operations for Android.

## Status

Local offline data foundation / Room-backed Android app.

The current app keeps the validated Compose front-end and architecture boundaries while replacing field-data storage with Room local persistence. Remote authentication, APIs, background sync, upload, and conflict resolution are intentionally not implemented yet.

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
- Simulated login, theme preference, and offline toggle for the existing prototype flow.
- Existing UI flow: Bootstrap, Login, Interventions, Detail, Execution, Notes, Photos, Completion, Sync, Settings, Logout.

## Simulated Or Not Implemented

- Authentication is still simulated and in-memory.
- Remote API is not implemented.
- Network synchronization is not implemented.
- Token storage is not implemented.
- WorkManager queue processing is not implemented.
- Remote photo upload is not implemented.
- Server conflict resolution is not implemented.
- CameraX capture is not implemented.

## Local Data Layer

- Database file: `tervyn.db`.
- Room schema version: `1`.
- Schema export: `app/schemas/`.
- Destructive migrations are not enabled.
- The operational database is excluded from backup and `allowBackup` is disabled until the security/session policy is finalized.

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
