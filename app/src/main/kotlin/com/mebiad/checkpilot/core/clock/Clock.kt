package com.mebiad.checkpilot.core.clock

import java.time.Instant
import java.time.LocalDate

interface Clock {
    fun now(): Instant

    fun today(): LocalDate
}
