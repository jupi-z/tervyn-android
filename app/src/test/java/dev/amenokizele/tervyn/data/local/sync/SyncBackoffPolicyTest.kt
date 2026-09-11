package dev.amenokizele.tervyn.data.local.sync

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class SyncBackoffPolicyTest {
    @Test
    fun retryDelaysGrowExponentiallyAndAreCapped() {
        val now = Instant.parse("2026-09-10T12:00:00Z")
        val policy = SyncBackoffPolicy()

        assertEquals(now.plusSeconds(30), policy.nextAttemptAt(now, 1))
        assertEquals(now.plusSeconds(60), policy.nextAttemptAt(now, 2))
        assertEquals(now.plusSeconds(3600), policy.nextAttemptAt(now, 20))
    }
}
