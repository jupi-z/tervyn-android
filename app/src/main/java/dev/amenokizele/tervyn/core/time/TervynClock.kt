package dev.amenokizele.tervyn.core.time

import java.time.Instant

interface TervynClock {
    fun now(): Instant
}

class SystemTervynClock : TervynClock {
    override fun now(): Instant = Instant.now()
}
