# Tervyn

Offline-first field operations for Android.

## Status

Architecture foundation / interactive in-memory Android demo.

This repository contains the validated Tervyn Android front-end baseline migrated to a cleaner single-module architecture. The UI and user journey remain a prototype, but ViewModels now depend on use cases and repository contracts instead of a Kotlin singleton data source.

## Current stack

- Kotlin
- Jetpack Compose
- Material 3
- Navigation Compose
- ViewModel
- StateFlow
- Coroutines
- Hilt
- java.time domain timestamps with core library desugaring

## Architecture foundation implemented

- Domain models for users, jobs, checklist items, notes, attachments, auth state, sync overview and preferences.
- Repository contracts for authentication, jobs, synchronization and user preferences.
- Use cases for the current business and application actions.
- Hilt dependency injection with singleton in-memory repository bindings.
- In-memory data implementations behind Domain interfaces.
- Lifecycle-aware Flow collection in production Composables.
- Root navigation with Bootstrap, AUTH graph and APP graph.
- APP graph start destination is Jobs.
- Logout is driven by `AuthState.Unauthenticated` and clears authenticated navigation state.
- `BuildConfig.VERSION_NAME` is used for the Settings version value.

## Implemented user flow

- Bootstrap.
- Simulated login with local validation.
- Jobs list with search and status filters: Toutes, A faire, En cours, Terminees.
- Job detail.
- Start / continue intervention.
- Execution checklist.
- Add note.
- Add simulated photo attachment.
- Photo viewer and delete action when the job is in progress.
- Complete intervention when required checklist items are done.
- Simulated sync screen.
- Settings with profile, appearance mode, last sync information, app version, MIT license and logout.
- Light, dark and system theme mode.

## Still simulated

- Authentication.
- Data persistence.
- Offline state.
- Network state.
- Sync engine.
- Photo capture.
- Photo upload.

## Not implemented yet

- Room.
- Retrofit.
- OkHttp networking.
- WorkManager.
- DataStore.
- Secure session or token storage.
- CameraX.
- Real API.
- Real synchronization.
- Conflict handling.
- Persistent outbox.

## Build

Use the Gradle wrapper from the repository:

```bash
./gradlew clean
./gradlew assembleDebug
./gradlew lint
./gradlew testDebugUnitTest
```

On Windows:

```powershell
.\gradlew.bat clean
.\gradlew.bat assembleDebug
.\gradlew.bat lint
.\gradlew.bat testDebugUnitTest
```

## License

MIT License.

Copyright (c) 2026 Ameno Kizele Mwine
