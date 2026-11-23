package com.mebiad.checkpilot.domain.session

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class SessionTest {
    @Test
    fun crewMembersListsTheFilledSlotsInOrder() {
        // Given
        val both = session(cm1 = "79125", cm2 = "04512")
        val cm2Only = session(cm1 = null, cm2 = "04512")

        // When
        val bothCrewMembers = both.crewMembers
        val cm2OnlyCrewMembers = cm2Only.crewMembers

        // Then
        assertEquals(listOf(CrewMember.CM1, CrewMember.CM2), bothCrewMembers)
        assertEquals(listOf(CrewMember.CM2), cm2OnlyCrewMembers)
    }

    @Test
    fun staffNumberOfGivesTheSlotsStaffNumber() {
        // Given
        val session = session(cm1 = "79125", cm2 = null)

        // When
        val cm1 = session.staffNumberOf(CrewMember.CM1)
        val cm2 = session.staffNumberOf(CrewMember.CM2)

        // Then
        assertEquals(StaffNumber.parse("79125"), cm1)
        assertEquals(null, cm2)
    }

    private fun session(
        cm1: String?,
        cm2: String?,
    ): Session =
        Session(
            id = "session",
            createdAt = Instant.parse("2026-11-22T06:00:00Z"),
            date = LocalDate.of(2026, 11, 22),
            cm1StaffNumber = cm1?.let { StaffNumber.parse(it) },
            cm2StaffNumber = cm2?.let { StaffNumber.parse(it) },
            departure = checkNotNull(AirportCode.parse("DOH")),
            destination = checkNotNull(AirportCode.parse("SLL"))
        )
}
