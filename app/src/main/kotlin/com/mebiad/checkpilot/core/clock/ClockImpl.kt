package com.mebiad.checkpilot.core.clock

import java.time.Instant
import java.time.LocalDate

class ClockImpl(
    private val source: java.time.Clock = java.time.Clock.systemDefaultZone(),
) : Clock {
    override fun now(): Instant = source.instant()

    override fun today(): LocalDate = LocalDate.now(source)
}
