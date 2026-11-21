package com.mebiad.checkpilot.core.clock

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class ClockFake(
    var instant: Instant = Instant.parse("2026-11-01T09:00:00Z"),
    var zone: ZoneId = ZoneOffset.UTC,
) : Clock {
    override fun now(): Instant = instant

    override fun today(): LocalDate = instant.atZone(zone).toLocalDate()

    fun advanceBy(duration: Duration) {
        instant = instant.plus(duration)
    }

    fun setToday(date: LocalDate) {
        instant = date.atTime(9, 0).atZone(zone).toInstant()
    }
}
