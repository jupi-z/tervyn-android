# Tervyn Android Architecture

## Status

Phase 5 adds the offline sync engine inside the existing single `:app` module. Room remains the source of truth for UI-observed field data. Remote mode is opt-in and no public Tervyn backend is bundled or claimed.

## Dependency Rule

```text
Compose UI
   -> ViewModel
      -> Use Case
         -> Repository Contract
            <- Room Repository / Persistent Auth Repository
               -> Room Database / Secure Session Coordinator / DataStore
            <- Remote data sources
               -> Retrofit API interfaces
               -> OkHttp
```

The Domain layer remains pure Kotlin and independent from Android UI, Compose, Hilt, Room, Retrofit, OkHttp, and serialization DTOs. Feature and navigation code do not import remote DTOs, `AuthApi`, `JobsApi`, `RemoteJobDataSource`, or OkHttp.

## Packages

- `app/`: root application state, startup initialization contract, and app-level contracts.
- `core/result/`: `AppResult` and `AppError`.
- `core/time/`: clock and presentation date/time utilities.
- `domain/model/`: business models and enums.
- `domain/repository/`: repository contracts.
- `domain/usecase/`: use cases for current application actions.
- `data/fixtures/`: local demo fixture source used by the one-time Room seed.
- `data/auth/`: local demo authentication, encrypted session storage, local users, and persistent auth repository.
- `data/remote/auth/`: auth DTOs, gateways, bearer interceptor, session refresh authenticator, token refresher, and session coordinator.
- `data/remote/config/`: remote API configuration.
- `data/remote/error/`: remote error envelope and `RemoteErrorMapper`.
- `data/remote/job/`: jobs API, DTOs, mappers, remote models, and `RemoteJobDataSource`.
- `data/inmemory/`: simulated offline UI state only.
- `data/local/`: Room converters, entities, DAOs, database, mappers, relations, repositories, and seeding.
- `data/preferences/`: Preferences DataStore-backed non-sensitive preferences.
- `di/`: Hilt modules, bindings, qualifiers, and network providers.
- `feature/`: screens and ViewModels.
- `navigation/`: root, auth, and app navigation graphs.

## Local Source Of Truth

`RoomJobRepository` implements `JobRepository` and remains bound as the UI-facing job repository. It reads and writes jobs, checklist items, notes, attachment metadata, and outbox rows through Room. `observeJobs()` and `observeJob(jobId)` are Room Flow queries, so UI state updates after local transactions without remote access.

`RoomSyncRepository` implements `SyncRepository` from persistent outbox counters and runtime sync state. `SyncOrchestrator` executes push first, then paginated pull/merge, while a process-local mutex prevents overlapping runs.

`SyncOperationEntity.payloadJson` stores an immutable versioned payload for each mutation. The payload is decoded before execution, and the original `clientMutationId` is reused for retries. Upload operations are durable but excluded from execution until the later attachment-upload phase.

## Remote API Foundation

```text
RemoteJobDataSource
   -> JobsApi
      -> authenticated Retrofit
         -> authenticated OkHttp
            -> SafeHeadersInterceptor
            -> BearerTokenInterceptor
            -> SessionRefreshAuthenticator
```

`RemoteJobDataSource` returns remote snapshot models and never writes Room. It supports job list/detail fetches and declares mutation contracts for future status, checklist, note, and attachment metadata operations. Mutation calls accept a caller-provided `clientMutationId` for `Idempotency-Key` and stable `If-Match: "<serverVersion>"` headers where required.

`RemotePullSynchronizer` fetches pages using the stored remote watermark, merges each page in one Room transaction, and commits the watermark only after the complete pull succeeds. `RemoteJobMerger` preserves local business fields for entities with pending, processing, or failed local operations while applying remote-owned fields and server versions.

`SyncScheduler` registers unique immediate and periodic WorkManager jobs with a connected-network constraint. The application schedules periodic work only when remote mode is enabled.

## Authentication And Session

```text
Login UI
   -> LoginUseCase
      -> AuthRepository
         <- PersistentAuthRepository
            -> ConfiguredAuthGateway
               -> LocalAuthGatewayAdapter
                  -> LocalDemoAuthGateway
               -> RemoteAuthGateway
                  -> AuthApi
            -> SessionCoordinator
               -> SecureSessionStore
                  -> Android Keystore AES/GCM
                  -> app-private AtomicFile encrypted envelope
            -> Room UserDao
```

`ConfiguredAuthGateway` selects local demo auth by default and remote auth only when `TERVYN_REMOTE_API_ENABLED=true`. Remote login upserts the returned user into Room before replacing the session. A session write failure does not publish an authenticated state.

`SessionCoordinator` is the single in-memory session snapshot source for network code. `BearerTokenInterceptor` reads this memory snapshot and does not hit disk per request.

## Network Clients

The public client is used for login and refresh. It has safe headers and no bearer interceptor or authenticator.

Remote logout revocation also uses the public/no-authenticator transport with an explicit `Authorization` header captured from the session being revoked. Logout never triggers token refresh.

The authenticated client is used for protected endpoints. It has safe headers, bearer injection, and `SessionRefreshAuthenticator`.

Timeouts:

- connect: 15 seconds
- read: 30 seconds
- write: 30 seconds
- call: 60 seconds

HTTP logging is `BASIC` in debug and `NONE` in release. Authorization is redacted. Bodies and headers containing credentials or tokens are not logged.

## 401 Refresh

`SessionRefreshAuthenticator` handles protected 401 responses with:

- one retry maximum;
- single-flight refresh under a process-local lock;
- reuse when another request has already refreshed the token;
- public refresh client to avoid auth loops;
- refresh-token expiry or invalid refresh attempting durable local clear;
- `SessionState.Empty` published only after durable clear success or restore reading no session;
- clear failure preserving the previous active session snapshot;
- temporary server/network refresh failure preserving the old session;
- persistence failure preserving the old session and refusing to retry with an unpersisted token.

## Explicit Non-Goals

- Remote attachment upload: deferred; the upload operation is persisted but not executed.
- Interactive conflict resolution: not implemented; permanent conflicts remain failed for a later product phase.
- CameraX capture: not implemented.
