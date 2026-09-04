package dev.amenokizele.tervyn.core.time

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class TervynDateTimeFormatter(
    private val zoneId: ZoneId = ZoneId.systemDefault(),
    private val locale: Locale = Locale.FRENCH
) {
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", locale)
    private val dayFormatter = DateTimeFormatter.ofPattern("dd MMMM", locale)
    private val dayTimeFormatter = DateTimeFormatter.ofPattern("dd MMMM · HH:mm", locale)

    fun formatTime(value: Instant?): String = value?.atZone(zoneId)?.format(timeFormatter).orEmpty()

    fun formatDay(value: Instant?): String = value?.atZone(zoneId)?.format(dayFormatter).orEmpty()

    fun formatDayTime(value: Instant?): String = value?.atZone(zoneId)?.format(dayTimeFormatter).orEmpty()
}
