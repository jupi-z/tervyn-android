# Tervyn

Offline-first field operations for Android.

## Status

Front-end baseline / interactive demo.

This repository contains the Android front-end baseline for Tervyn. It is an in-memory Compose prototype intended to validate the current user flows before the real data, network, authentication and offline-first architecture is added.

## Current stack

- Kotlin
- Jetpack Compose
- Material 3
- Navigation Compose
- ViewModel
- StateFlow
- Coroutines

## Implemented in the baseline

- Bootstrap flow.
- Demo login screen with local input validation.
- Interventions list with search and status filters: Toutes, À faire, En cours, Terminées.
- Intervention detail screen.
- Start and continue intervention flow.
- Execution screen with checklist, notes, photos and completion action.
- Dedicated screens for adding a note, adding a photo, viewing a photo and completing an intervention.
- Sync screen showing simulated pending, syncing, synced and failed states.
- Settings screen with profile, appearance mode, last sync information, app version, MIT license and logout.
- Light, dark and system theme mode.
- In-memory demo data.

## Simulated only

- `DemoRepository` is the temporary in-memory source of truth.
- Login is a demo interaction, not real authentication.
- Offline mode is a UI simulation.
- Sync state is simulated locally.
- Photo capture and gallery selection are represented by placeholder demo photos.

## Not implemented yet

- Room
- Retrofit
- WorkManager
- Real authentication
- Secure token storage
- CameraX
- Real API
- Real synchronization
- Persistent local storage
- Conflict resolution

## Build

Use the Gradle wrapper from the repository:

```bash
./gradlew clean assembleDebug
```

On Windows:

```powershell
.\gradlew.bat clean assembleDebug
```

Run baseline checks:

```bash
./gradlew lint
./gradlew testDebugUnitTest
```

## License

MIT License.

Copyright (c) 2026 Ameno Kizele Mwine
