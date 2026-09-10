# Tervyn Security Notes

## Phase 4 Threat Scope

Phase 4 adds a production-oriented Android HTTP client foundation and remote authentication client while preserving the Phase 3 secure local session store. It does not claim that a public Tervyn backend exists and does not implement a background sync engine.

The local storage protections do not claim resistance against a fully compromised or rooted device.

## Encrypted Session

The secure session payload contains:

- `userId`
- opaque access token
- opaque refresh token
- issue time
- access-token expiry
- refresh/session expiry
- session schema version

The entire payload is encrypted before it is written to an app-private AtomicFile envelope.

## Key Management

The AES key is generated and stored by Android Keystore under the stable alias `tervyn.session.aes.v2`. Key material is never exported to the repository or application files.

## Cipher

Session storage uses `AES/GCM/NoPadding` with a 256-bit key request and a fresh random IV generated for each write. GCM provides authenticated encryption, so tampering with the IV or ciphertext fails closed.

## Atomic Session Storage

The encrypted payload is stored in `files/secure/tervyn_secure_session_v2.bin` using `android.util.AtomicFile`. The storage envelope has a magic header, storage format version `2`, strict file/IV/ciphertext length bounds, and two record types: `SESSION` for encrypted session material and `EMPTY` for a durable logout tombstone.

`write()` reports success only after `AtomicFile.finishWrite()` succeeds. If a normal filesystem write fails before commit, `AtomicFile.failWrite()` restores the previous durable file. `clear()` writes an `EMPTY` tombstone atomically so a failed clear keeps the previous session file rather than pretending logout was durable.

## Session Coordinator

`SessionCoordinator` serializes restore, replace, and clear operations with a mutex and exposes a memory snapshot for the HTTP layer. OkHttp interceptors read only the memory snapshot; they do not read the encrypted session file per request.

An `Empty` coordinator state invalidates the authenticated app state so refresh failures can force logout consistently.

## Remote Authentication

Remote mode is opt-in through BuildConfig. Local demo auth remains the default.

Remote tokens are treated as opaque values. The client does not assume JWT claims, decode token bodies, or persist passwords. Remote login stores only the returned user and session material required for subsequent authenticated requests.

Remote logout is best-effort server revocation. Local session clearing is authoritative for user logout. A remote revoke failure cannot block local logout.

## 401 Refresh Behavior

Protected requests use `SessionRefreshAuthenticator` for 401 responses. It allows one retry, uses a public refresh client, and performs single-flight refresh so concurrent 401 responses share one refresh call.

If another request has already refreshed the token, the authenticator retries with the newer in-memory token without calling refresh again.

Expired or invalid refresh tokens clear the local session. Temporary refresh server/network failures preserve the previous session. If refreshed session persistence fails, the app preserves the old session and does not retry with an unpersisted token.

## HTTP Logging And Redaction

HTTP logging is limited to `BASIC` in debug and `NONE` in release. The app does not use body logging. Authorization is redacted. The code must not log passwords, access tokens, refresh tokens, login bodies, session response bodies, or raw error bodies.

## TLS

When remote mode is enabled for production configuration, `TERVYN_API_BASE_URL` must use HTTPS and include a non-empty host. No trust-all `TrustManager`, permissive `HostnameVerifier`, SSL bypass, or certificate verification bypass is used.

Certificate pinning is intentionally deferred until a stable real backend and certificate strategy exist.

## Backup Policy

`android:allowBackup` remains `false`. Backup and data-extraction rules also exclude the Room database, the legacy secure session preferences, the v2 AtomicFile session envelope, and the Preferences DataStore file.

## Deferred Work

Future phases must add WorkManager sync, persistent outbox execution, automatic pull/merge, retry/backoff scheduling, network constraints, conflict resolution, sync operation processing, and remote attachment upload.
