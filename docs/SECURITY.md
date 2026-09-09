# Tervyn Security Notes

## Phase 3 Threat Scope

Phase 3 protects locally persisted session material against accidental plaintext exposure in normal application-private storage. It does not implement remote authentication and does not claim protection against a fully compromised or rooted device.

## Encrypted Session

The secure session payload contains:

- `userId`
- synthetic local demo access token
- synthetic local demo refresh token
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

## Passwords

The demo password is non-secret test data used only by the local demo gateway and login UI state. The application never persists a password, password hash, or encrypted password.

## Expiration

The refresh-token expiry is the Phase 3 persistent session boundary. An expired access token does not log the user out while the refresh/session expiry remains valid; remote token refresh is intentionally deferred.

## Corruption And Key Invalidation

If encrypted storage is corrupted, tampered with, unreadable, oversized, structurally invalid, or the Keystore key cannot be trusted, the application writes an `EMPTY` tombstone on a best-effort basis and treats the user as unauthenticated.

Recovery attempts payload cleanup and, for invalidated or unrecoverable keys, key deletion independently. Cleanup failures cannot return a session or escape as ordinary recovery exceptions. Coroutine cancellation is still propagated. A write with an unusable key fails; the next explicit login can create a new key.

## Legacy v0.4.1 Storage

v0.4.2 intentionally does not migrate the previous SharedPreferences-backed local demo session. On first secure-store access, the app attempts to clear legacy `tervyn_secure_session` preferences and delete the legacy `tervyn.session.aes.v1` key alias on a best-effort basis. The expected one-time impact is that a user may need to sign in again with the local demo account.

## Session Consistency

A failed re-login (invalid credentials, missing local user, or failed session write) preserves the existing authenticated session. A new session is published in memory only after secure storage reports a successful write. Restore, login, logout, and current-user expiry checks are serialized with a coroutine mutex. Persistence and memory publication complete together before honoring caller cancellation; authentication and lock waiting remain cancellable.

## Backup Policy

`android:allowBackup` remains `false`. Backup and data-extraction rules also exclude the Room database, the legacy secure session preferences, the v2 AtomicFile session envelope, and the Preferences DataStore file.

## Demo Authentication Limitation

Credential verification is local demo-only:

- email: `amina@tervyn.demo`
- password: `tervyn2026`

Synthetic tokens are generated locally, are opaque, are not JWTs, and are never sent to a server.

## Deferred Work

Future phases must add remote authentication, server-issued tokens, token refresh, 401 handling, Retrofit/OkHttp integration, WorkManager processing, remote sync, upload, and conflict resolution.
