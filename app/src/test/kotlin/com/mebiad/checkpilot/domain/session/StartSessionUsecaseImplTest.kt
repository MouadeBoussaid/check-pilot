package com.mebiad.checkpilot.domain.session

import com.mebiad.checkpilot.core.clock.ClockFake
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class StartSessionUsecaseImplTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = SessionRepositoryFake()
    private val clock = ClockFake()
    private val usecase =
        StartSessionUsecaseImpl(
            repository = repository,
            clock = clock,
            newId = { "session-1" },
            dispatcher = dispatcher
        )

    @Test
    fun noStaffNumberIsRefusedAndNothingIsSaved() =
        runTest(dispatcher) {
            // Given
            val setup = validSetup.copy(cm1StaffNumber = "", cm2StaffNumber = "")

            // When
            val result = usecase.start(setup)

            // Then
            assertEquals(StartSessionResult.Refused(SetupRefusal.NO_STAFF_NUMBER), result)
            assertEquals(emptyList<Session>(), repository.sessions)
        }

    @Test
    fun aValidSetupSavesTheSessionCreatedNow() =
        runTest(dispatcher) {
            // Given
            clock.setToday(LocalDate.of(2026, 11, 22))

            // When
            val result = usecase.start(validSetup)

            // Then
            assertEquals(StartSessionResult.Started("session-1"), result)
            val session = repository.sessions.single()
            assertEquals("session-1", session.id)
            assertEquals(clock.now(), session.createdAt)
            assertEquals(LocalDate.of(2026, 11, 1), session.date)
            assertEquals("79125", session.cm1StaffNumber?.digits)
            assertEquals(null, session.cm2StaffNumber)
            assertEquals("DOH", session.departure.letters)
            assertEquals("SLL", session.destination.letters)
        }

    @Test
    fun aStaffNumberOutside4To6DigitsIsRefusedInEitherSlot() =
        runTest(dispatcher) {
            val setups =
                listOf(
                    validSetup.copy(cm1StaffNumber = "791"),
                    validSetup.copy(cm1StaffNumber = "7912534"),
                    validSetup.copy(cm2StaffNumber = "045"),
                    validSetup.copy(cm2StaffNumber = "0451234")
                )
            for (setup in setups) {
                // When / Then: one setup after the other
                assertEquals(
                    StartSessionResult.Refused(SetupRefusal.STAFF_NUMBER_NOT_4_TO_6_DIGITS),
                    usecase.start(setup)
                )
                assertEquals(emptyList<Session>(), repository.sessions)
            }
        }

    @Test
    fun theShortestAndLongestStaffNumbersAreAccepted() =
        runTest(dispatcher) {
            // Given
            val setup = validSetup.copy(cm1StaffNumber = "0451", cm2StaffNumber = "791250")

            // When
            usecase.start(setup)

            // Then
            val session = repository.sessions.single()
            assertEquals("0451", session.cm1StaffNumber?.digits)
            assertEquals("791250", session.cm2StaffNumber?.digits)
        }

    @Test
    fun anEmptyDepartureOrDestinationIsRefused() =
        runTest(dispatcher) {
            for (setup in listOf(validSetup.copy(departure = ""), validSetup.copy(destination = ""))) {
                // When / Then: the departure left empty, then the destination
                assertEquals(StartSessionResult.Refused(SetupRefusal.AIRPORT_CODE_MISSING), usecase.start(setup))
                assertEquals(emptyList<Session>(), repository.sessions)
            }
        }

    @Test
    fun anAirportCodeThatIsNot3LettersIsRefused() =
        runTest(dispatcher) {
            val setups =
                listOf(
                    validSetup.copy(destination = "AM"),
                    validSetup.copy(destination = "AM1"),
                    validSetup.copy(departure = "DOHA")
                )
            for (setup in setups) {
                // When / Then: one setup after the other
                assertEquals(
                    StartSessionResult.Refused(SetupRefusal.AIRPORT_CODE_NOT_3_LETTERS),
                    usecase.start(setup)
                )
                assertEquals(emptyList<Session>(), repository.sessions)
            }
        }

    @Test
    fun airportCodesTypedInLowercaseAreSavedInCapitals() =
        runTest(dispatcher) {
            // Given
            val setup = validSetup.copy(departure = "doh", destination = "sLl")

            // When
            usecase.start(setup)

            // Then
            val session = repository.sessions.single()
            assertEquals("DOH", session.departure.letters)
            assertEquals("SLL", session.destination.letters)
        }

    private val validSetup =
        SessionSetup(
            cm1StaffNumber = "79125",
            cm2StaffNumber = "",
            departure = "DOH",
            destination = "SLL",
            date = LocalDate.of(2026, 11, 1)
        )
}
