package com.mebiad.checkpilot.core.clock

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class ClockImplTest {
    @Test
    fun nowAndTodayComeFromTheSystemSource() {
        // Given
        val instant = Instant.parse("2026-11-01T23:30:00Z")
        val source = java.time.Clock.fixed(instant, ZoneId.of("Europe/Amsterdam"))
        val clock = ClockImpl(source)

        // When
        val now = clock.now()
        val today = clock.today()

        // Then
        assertEquals(instant, now)
        assertEquals(LocalDate.of(2026, 11, 2), today)
    }
}
