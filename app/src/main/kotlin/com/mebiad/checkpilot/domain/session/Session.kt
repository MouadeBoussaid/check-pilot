package com.mebiad.checkpilot.domain.session

import java.time.Instant
import java.time.LocalDate

/** A Session has at least one Crew Member; either slot may be the empty one. */
data class Session(
    val id: String,
    val createdAt: Instant,
    val date: LocalDate,
    val cm1StaffNumber: StaffNumber?,
    val cm2StaffNumber: StaffNumber?,
    val departure: AirportCode,
    val destination: AirportCode,
)
