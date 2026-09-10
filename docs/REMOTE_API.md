# Tervyn Remote API Foundation

## Status

Phase 4 adds the Android client foundation for a future remote API. It does not include or claim a public Tervyn backend. All network behavior is validated with MockWebServer tests.

## Configuration

Default BuildConfig values:

```text
TERVYN_REMOTE_API_ENABLED=false
TERVYN_API_BASE_URL=https://tervyn.invalid/
```

Remote mode is opt-in. When disabled, the app uses the local demo auth flow and does not perform automatic remote startup requests, health checks, or job fetches.

When enabled for production, the base URL must:

- be parseable as an absolute URL;
- use HTTPS;
- have a non-empty host;
- end with `/`.

MockWebServer tests may use HTTP by constructing test config with `allowHttpForTests=true`.

## Clients

Public client:

- login;
- refresh;
- logout revocation with an explicit captured `Authorization` header;
- safe headers;
- no bearer token interceptor;
- no authenticator.

Authenticated client:

- protected endpoints;
- safe headers;
- bearer token injection;
- 401 refresh authenticator.

Timeouts are 15s connect, 30s read, 30s write, and 60s call.

## Auth Flow

`ConfiguredAuthGateway` selects:

- `LocalAuthGatewayAdapter` when remote mode is disabled;
- `RemoteAuthGateway` when remote mode is enabled.

Remote login calls `POST /v1/auth/login`, maps DTOs into data/domain models, upserts the returned user locally, then atomically replaces the session through `SessionCoordinator`.

## Refresh Flow

`SessionRefreshAuthenticator` handles a protected 401 by:

- avoiding loops with one retry maximum;
- reusing an already-refreshed in-memory token when available;
- refreshing once for concurrent 401 responses;
- persisting the full rotated session before retrying;
- attempting durable local clear for expired or invalid refresh tokens;
- publishing `SessionState.Empty` only when durable clear succeeds;
- preserving the previous active session snapshot if durable clear fails;
- preserving the previous session for temporary network/server refresh failures or persistence failures.

Remote logout never refreshes credentials. `/v1/auth/logout` uses the no-authenticator public client and sends the access token and refresh token from the same captured `StoredSession`. A logout 401 is mapped as a revoke failure and local secure clear remains authoritative for user logout.

## Error Mapping

Remote errors map into the existing `AppError` set:

- 400: `Validation(serverCode ?: "bad_request")`
- 401: `Authentication(serverCode ?: "unauthorized")`
- 403: `Authentication(serverCode ?: "forbidden")`
- 404: `NotFound("remote", path)`
- 409: `Conflict(serverCode ?: "conflict")`
- 412: `Conflict(serverCode ?: "version_conflict")`
- 422: `Validation(serverCode ?: "validation_failed")`
- 429: `Network("rate_limited")`
- 5xx: `Network("server_error")`
- IO: `Network("network_unavailable")`
- malformed JSON/body: `Network("invalid_server_response")`
- unknown: `Unknown("remote_error")`

Server human-readable messages are not copied into stable app error codes.

## Jobs Contract

`RemoteJobDataSource` supports:

- fetch job pages;
- fetch job detail;
- update job status;
- update checklist item;
- create note;
- delete attachment metadata.

It returns remote snapshot models and does not write Room. Phase 5 will decide how remote snapshots are merged into the Room source of truth.

## Idempotency And Versions

Mutation methods accept the existing caller-provided `clientMutationId` and send it as:

```http
Idempotency-Key: <clientMutationId>
```

Versioned updates send:

```http
If-Match: "<serverVersion>"
```

The data source does not generate a new idempotency key for outbox operations.

## Deferred

- Remote snapshot to Room merge.
- Persistent outbox execution.
- WorkManager.
- Retry/backoff scheduler.
- Network constraints.
- Conflict resolution.
- SyncOperation processing.
- Remote attachment upload.
