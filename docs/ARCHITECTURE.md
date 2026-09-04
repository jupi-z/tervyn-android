# Tervyn Android Architecture

## Status

Phase 1 implements an architecture foundation inside the existing `:app` module. It does not add real persistence, networking, background sync or secure authentication.

## Dependency Rule

```text
Compose UI
   -> ViewModel
      -> Use Case
         -> Repository contract
            <- In-memory data implementation
```

The Domain layer is independent from Android UI, Compose, Hilt, Room and network libraries. Feature ViewModels depend on use cases or small application contracts. Data implementations depend on Domain contracts and are bound with Hilt.

## Packages

- `app/`: root application state and app-level contracts.
- `core/result/`: `AppResult` and `AppError`.
- `core/time/`: clock and presentation date/time formatting utilities.
- `domain/model/`: business models and enums.
- `domain/repository/`: repository contracts.
- `domain/usecase/`: use cases for existing actions.
- `data/fixtures/`: in-memory fixture data.
- `data/inmemory/`: temporary repository implementations.
- `di/`: Hilt bindings and providers.
- `feature/`: screens and ViewModels.
- `navigation/`: root, auth and app navigation graphs.

## In-memory Implementation

The current data source is intentionally in-memory. `InMemoryJobRepository`, `InMemoryAuthRepository`, `InMemorySyncRepository` and `InMemoryUserPreferencesRepository` are singleton Hilt bindings used to preserve the prototype behavior while making the UI independent from the temporary storage mechanism.

`TervynDemoFixtures` contains only fixture data. It is not a repository and is not called by production screens.

## Future Room Replacement

Room will be introduced in a later phase by adding persistence implementations behind the existing repository contracts. The UI, ViewModels and use cases should not need to know whether jobs come from in-memory flows or Room-backed flows.

## Future Retrofit Replacement

Retrofit and DTO mapping will be introduced behind remote data sources in a later phase. Domain models remain separate from transport DTOs.

## Future WorkManager / Outbox

The sync screen is still a UI simulation. A real outbox and WorkManager-based synchronization engine are not implemented in Phase 1. They should be added behind sync/data abstractions in the offline-first phase.
