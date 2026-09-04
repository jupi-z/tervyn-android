# Tervyn Data Model

## Status

Phase 2 defines the first persistent Room schema for Tervyn Android.

- Database file: `tervyn.db`
- Room schema version: `1`
- Schema export path: `app/schemas/dev.amenokizele.tervyn.data.local.db.TervynDatabase/1.json`
- Remote conflict resolution: NOT IMPLEMENTED YET

## Tables

### users

Primary key: `id`.

Columns:

- `id: String`
- `email: String`
- `firstName: String`
- `lastName: String`
- `jobTitle: String?`
- `avatarUrl: String?`
- `createdAt: Instant?`
- `updatedAt: Instant?`
- `lastSyncedAt: Instant?`

Indexes:

- `email`

### jobs

Primary key: `id`.

Columns:

- `id: String`
- `reference: String`
- `title: String`
- `description: String?`
- `clientName: String`
- `siteName: String`
- `siteAddress: String?`
- `priority: JobPriority`
- `status: JobStatus`
- `scheduledAt: Instant`
- `startedAt: Instant?`
- `completedAt: Instant?`
- `serverVersion: Long`
- `syncState: SyncState`
- `createdAt: Instant`
- `updatedAt: Instant`
- `lastSyncedAt: Instant?`

Indexes:

- `reference` unique
- `status`
- `scheduledAt`
- `syncState`

Allowed status transitions are enforced in `RoomJobRepository`:

```text
ASSIGNED -> IN_PROGRESS -> COMPLETED
```

### checklist_items

Primary key: `id`.

Foreign key:

- `jobId -> jobs.id ON DELETE CASCADE`

Columns:

- `id: String`
- `jobId: String`
- `label: String`
- `position: Int`
- `required: Boolean`
- `completed: Boolean`
- `completedAt: Instant?`
- `serverVersion: Long`
- `syncState: SyncState`
- `updatedAt: Instant`

Indexes:

- `jobId`
- `(jobId, position)` unique
- `syncState`

### notes

Primary key: `id`.

Foreign key:

- `jobId -> jobs.id ON DELETE CASCADE`

`authorUserId` is stored as a logical reference to preserve note history without destructive user cascades.

Columns:

- `id: String`
- `jobId: String`
- `authorUserId: String`
- `content: String`
- `createdAt: Instant`
- `updatedAt: Instant`
- `syncState: SyncState`
- `serverVersion: Long?`
- `deletedAt: Instant?`

Indexes:

- `jobId`
- `authorUserId`
- `syncState`
- `createdAt`

### attachments

Primary key: `id`.

Foreign key:

- `jobId -> jobs.id ON DELETE CASCADE`

Room stores attachment metadata and local URI strings only. It does not store bitmaps, byte arrays, or photo blobs.

Columns:

- `id: String`
- `jobId: String`
- `authorUserId: String`
- `type: AttachmentType`
- `localUri: String?`
- `remoteUrl: String?`
- `mimeType: String`
- `fileName: String?`
- `sizeBytes: Long`
- `checksumSha256: String?`
- `syncState: SyncState`
- `createdAt: Instant`
- `uploadedAt: Instant?`
- `deletedAt: Instant?`

Indexes:

- `jobId`
- `authorUserId`
- `syncState`
- `createdAt`

Soft delete:

- `deleteAttachment()` sets `deletedAt = now` and `syncState = PENDING`.
- Soft-deleted attachments remain in Room for future remote delete processing.
- Normal `Job.attachments` mapping hides rows where `deletedAt != null`.

### sync_operations

Primary key: `id`.

This is a persistent local outbox only. No worker processes it in Phase 2.

Columns:

- `id: String`
- `entityType: SyncEntityType`
- `entityId: String`
- `operation: SyncOperationType`
- `clientMutationId: String`
- `status: SyncOperationStatus`
- `attemptCount: Int`
- `lastErrorCode: String?`
- `lastErrorMessage: String?`
- `createdAt: Instant`
- `lastAttemptAt: Instant?`
- `nextAttemptAt: Instant?`

Indexes:

- `status`
- `createdAt`
- `(entityType, entityId)`
- `clientMutationId` unique

Outbox enums:

- `SyncEntityType`: `JOB`, `CHECKLIST_ITEM`, `NOTE`, `ATTACHMENT`
- `SyncOperationType`: `UPDATE`, `CREATE`, `DELETE`, `UPLOAD`
- `SyncOperationStatus`: `PENDING`, `PROCESSING`, `FAILED`

### local_metadata

Infrastructure-only table.

Primary key: `key`.

Columns:

- `key: String`
- `value: String`
- `updatedAt: Instant`

Current seed marker:

- `key = demo_seed_version`
- `value = 1`

## Sync State Rules

- One-time seeded fixture rows are stored as `SYNCED`.
- Every local business mutation writes the changed row as `PENDING`.
- Every local business mutation also writes one outbox row in the same Room transaction.
- No mutation is marked `SYNCED` without real remote processing.

## Transaction Rules

The repository validates current state, mutates business rows, and inserts the outbox row inside one Room transaction. If outbox insertion fails, the business mutation rolls back.

## Time And IDs

- Domain timestamps remain `java.time.Instant`.
- Room stores `Instant` values as epoch milliseconds through type converters.
- New note IDs, attachment IDs, sync operation IDs, and `clientMutationId` values are full UUID strings in production.
- `serverVersion` is persisted for future server reconciliation, but real reconciliation is not implemented yet.
