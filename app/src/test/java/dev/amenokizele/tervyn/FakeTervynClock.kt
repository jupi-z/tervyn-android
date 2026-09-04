package dev.amenokizele.tervyn

import dev.amenokizele.tervyn.core.time.TervynClock
import java.time.Instant

class FakeTervynClock(
    private var current: Instant = Instant.parse("2026-09-03T12:00:00Z")
) : TervynClock {
    override fun now(): Instant = current

    fun advance(seconds: Long) {
        current = current.plusSeconds(seconds)
    }
}
