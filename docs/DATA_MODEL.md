# Tervyn Data Model

## Status

Phase 1 defines Domain models with real `java.time.Instant` timestamps. These are not Room entities and are not network DTOs.

## User

Represents the authenticated field user in the app session.

- `id`
- `email`
- `firstName`
- `lastName`
- `jobTitle`
- `avatarUrl`
- `createdAt`
- `updatedAt`
- `lastSyncedAt`

## Job

Represents an intervention assigned to a field user.

- `id`
- `reference`
- `title`
- `description`
- `clientName`
- `siteName`
- `siteAddress`
- `priority`
- `status`
- `scheduledAt`
- `startedAt`
- `completedAt`
- `serverVersion`
- `syncState`
- `createdAt`
- `updatedAt`
- `lastSyncedAt`
- `checklist`
- `notes`
- `attachments`

Allowed status transitions:

```text
ASSIGNED -> IN_PROGRESS -> COMPLETED
```

## ChecklistItem

Represents one checklist task attached to a job.

- `id`
- `jobId`
- `label`
- `position`
- `required`
- `completed`
- `completedAt`
- `serverVersion`
- `syncState`
- `updatedAt`

## Note

Represents a note created during an in-progress job.

- `id`
- `jobId`
- `authorUserId`
- `content`
- `createdAt`
- `updatedAt`
- `syncState`
- `serverVersion`
- `deletedAt`

## Attachment

Represents a job attachment. Phase 1 supports only `AttachmentType.PHOTO`.

- `id`
- `jobId`
- `authorUserId`
- `type`
- `localUri`
- `remoteUrl`
- `mimeType`
- `fileName`
- `sizeBytes`
- `checksumSha256`
- `syncState`
- `createdAt`
- `uploadedAt`
- `deletedAt`

`localUri` is a string placeholder in Phase 1. No Android `Uri`, bitmap, drawable or file handle is stored in Domain.

## NOT IMPLEMENTED YET

- Room entity mapping.
- DAO layer.
- SQLite persistence.
- `SyncOperationEntity`.
- Persistent outbox.
- Remote DTO mapping.
- Conflict resolution.
- Real `serverVersion` reconciliation.
