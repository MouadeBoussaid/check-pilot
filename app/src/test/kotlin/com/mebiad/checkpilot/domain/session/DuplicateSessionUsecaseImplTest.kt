package com.mebiad.checkpilot.domain.session

import com.mebiad.checkpilot.core.clock.ClockFake
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class DuplicateSessionUsecaseImplTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = SessionRepositoryFake()
    private val clock = ClockFake(instant = Instant.parse("2026-11-27T09:30:00Z"))
    private val usecase =
        DuplicateSessionUsecaseImpl(
            repository = repository,
            clock = clock,
            newId = { "duplicate" },
            dispatcher = dispatcher
        )

    @Test
    fun theDuplicateIsTheReturnSectorTodayWithTheSameStaffNumbers() =
        runTest(dispatcher) {
            // Given
            val original = session(id = "original", cm1 = "79125", cm2 = "04512")
            repository.insert(original)

            // When
            usecase.duplicate("original")
            advanceUntilIdle()

            // Then
            val expected =
                Session(
                    id = "duplicate",
                    createdAt = Instant.parse("2026-11-27T09:30:00Z"),
                    date = LocalDate.of(2026, 11, 27),
                    cm1StaffNumber = StaffNumber.parse("79125"),
                    cm2StaffNumber = StaffNumber.parse("04512"),
                    departure = checkNotNull(AirportCode.parse("SLL")),
                    destination = checkNotNull(AirportCode.parse("DOH"))
                )
            assertEquals(listOf(original, expected), repository.sessions)
        }

    @Test
    fun anEmptyCrewMemberSlotStaysEmpty() =
        runTest(dispatcher) {
            // Given
            repository.insert(session(id = "original", cm1 = null, cm2 = "04512"))

            // When
            usecase.duplicate("original")
            advanceUntilIdle()

            // Then
            val duplicate = repository.sessions.single { it.id == "duplicate" }
            assertEquals(null, duplicate.cm1StaffNumber)
            assertEquals(StaffNumber.parse("04512"), duplicate.cm2StaffNumber)
        }

    @Test
    fun aSessionThatIsGoneIsNotDuplicated() =
        runTest(dispatcher) {
            // Given
            repository.insert(session(id = "other", cm1 = "79125", cm2 = null))

            // When
            usecase.duplicate("gone")
            advanceUntilIdle()

            // Then
            assertEquals(listOf("other"), repository.sessions.map { it.id })
        }

    private fun session(
        id: String,
        cm1: String?,
        cm2: String?,
    ): Session =
        Session(
            id = id,
            createdAt = Instant.parse("2026-11-01T08:00:00Z"),
            date = LocalDate.of(2026, 11, 1),
            cm1StaffNumber = cm1?.let(StaffNumber::parse),
            cm2StaffNumber = cm2?.let(StaffNumber::parse),
            departure = checkNotNull(AirportCode.parse("DOH")),
            destination = checkNotNull(AirportCode.parse("SLL"))
        )
}
