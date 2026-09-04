package dev.amenokizele.tervyn.data.local.converter

import dev.amenokizele.tervyn.data.local.entity.SyncEntityType
import dev.amenokizele.tervyn.data.local.entity.SyncOperationStatus
import dev.amenokizele.tervyn.data.local.entity.SyncOperationType
import dev.amenokizele.tervyn.domain.model.AttachmentType
import dev.amenokizele.tervyn.domain.model.JobPriority
import dev.amenokizele.tervyn.domain.model.JobStatus
import dev.amenokizele.tervyn.domain.model.SyncState
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RoomConvertersTest {
    private val converters = RoomConverters()

    @Test
    fun instantConverter_roundTripsEpochMillisWithoutLocaleOrTimezoneFormatting() {
        val instant = Instant.parse("2026-09-03T12:34:56.789Z")

        val stored = converters.instantToEpochMillis(instant)
        val restored = converters.epochMillisToInstant(stored)

        assertEquals(1_788_438_896_789L, stored)
        assertEquals(instant, restored)
    }

    @Test
    fun instantConverter_acceptsNullValues() {
        assertNull(converters.instantToEpochMillis(null))
        assertNull(converters.epochMillisToInstant(null))
    }

    @Test
    fun domainEnumConverters_storeEnumNamesAsText() {
        assertEquals("URGENT", converters.jobPriorityToString(JobPriority.URGENT))
        assertEquals(JobPriority.URGENT, converters.stringToJobPriority("URGENT"))
        assertEquals("IN_PROGRESS", converters.jobStatusToString(JobStatus.IN_PROGRESS))
        assertEquals(JobStatus.IN_PROGRESS, converters.stringToJobStatus("IN_PROGRESS"))
        assertEquals("PENDING", converters.syncStateToString(SyncState.PENDING))
        assertEquals(SyncState.PENDING, converters.stringToSyncState("PENDING"))
        assertEquals("PHOTO", converters.attachmentTypeToString(AttachmentType.PHOTO))
        assertEquals(AttachmentType.PHOTO, converters.stringToAttachmentType("PHOTO"))
    }

    @Test
    fun outboxEnumConverters_storeEnumNamesAsText() {
        assertEquals("JOB", converters.syncEntityTypeToString(SyncEntityType.JOB))
        assertEquals(SyncEntityType.JOB, converters.stringToSyncEntityType("JOB"))
        assertEquals("UPLOAD", converters.syncOperationTypeToString(SyncOperationType.UPLOAD))
        assertEquals(SyncOperationType.UPLOAD, converters.stringToSyncOperationType("UPLOAD"))
        assertEquals("PROCESSING", converters.syncOperationStatusToString(SyncOperationStatus.PROCESSING))
        assertEquals(SyncOperationStatus.PROCESSING, converters.stringToSyncOperationStatus("PROCESSING"))
    }
}
