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
) {
    val hasTwoCrewMembers: Boolean
        get() = cm1StaffNumber != null && cm2StaffNumber != null

    /** The slots that hold a Staff Number, CM1 first. */
    val crewMembers: List<CrewMember>
        get() = CrewMember.entries.filter { staffNumberOf(it) != null }

    fun staffNumberOf(crewMember: CrewMember): StaffNumber? =
        when (crewMember) {
            CrewMember.CM1 -> cm1StaffNumber
            CrewMember.CM2 -> cm2StaffNumber
        }
}
