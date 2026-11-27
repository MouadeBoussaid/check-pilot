package com.mebiad.checkpilot.ui.apps.setup

import app.cash.turbine.test
import com.mebiad.checkpilot.core.clock.ClockFake
import com.mebiad.checkpilot.domain.session.SessionSetup
import com.mebiad.checkpilot.domain.session.SetupRefusal
import com.mebiad.checkpilot.domain.session.StartSessionResult
import com.mebiad.checkpilot.domain.session.StartSessionUsecaseFake
import com.mebiad.checkpilot.testutils.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class SetupViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val startSession = StartSessionUsecaseFake()
    private val clock = ClockFake().apply { setToday(LocalDate.of(2026, 11, 21)) }

    @Test
    fun theDateStartsAsToday() {
        // Given
        val viewModel = SetupViewModel(startSession = startSession, clock = clock)

        // When
        val date = viewModel.state.value.date

        // Then
        assertEquals(LocalDate.of(2026, 11, 21), date)
    }

    @Test
    fun editingTheFieldsUpdatesTheState() {
        // Given
        val viewModel = SetupViewModel(startSession = startSession, clock = clock)

        // When
        viewModel.onCm1StaffNumberChange("79125")
        viewModel.onCm2StaffNumberChange("4410")
        viewModel.onDepartureChange("doh")
        viewModel.onDestinationChange("SLL")

        // Then
        val state = viewModel.state.value
        assertEquals("79125", state.cm1StaffNumber)
        assertEquals("4410", state.cm2StaffNumber)
        assertEquals("doh", state.departure)
        assertEquals("SLL", state.destination)
    }

    @Test
    fun startingSendsTheFieldsAndAnnouncesTheStartedSession() =
        runTest {
            // Given
            val viewModel = SetupViewModel(startSession = startSession, clock = clock)
            viewModel.onCm1StaffNumberChange("79125")
            viewModel.onDepartureChange("DOH")
            viewModel.onDestinationChange("SLL")
            startSession.result = StartSessionResult.Started("session-42")

            viewModel.events.test {
                // When
                viewModel.onStart()
                advanceUntilIdle()

                // Then
                assertEquals(SetupEvent.SessionStarted("session-42"), awaitItem())
                assertEquals(
                    listOf(
                        SessionSetup(
                            cm1StaffNumber = "79125",
                            cm2StaffNumber = "",
                            departure = "DOH",
                            destination = "SLL",
                            date = LocalDate.of(2026, 11, 21)
                        )
                    ),
                    startSession.setups
                )
            }
        }

    @Test
    fun aRefusedStartShowsTheRefusal() =
        runTest {
            // Given
            val viewModel = SetupViewModel(startSession = startSession, clock = clock)
            startSession.result = StartSessionResult.Refused(SetupRefusal.NO_STAFF_NUMBER)

            // When
            viewModel.onStart()
            advanceUntilIdle()

            // Then
            assertEquals(SetupRefusal.NO_STAFF_NUMBER, viewModel.state.value.refusal)
        }

    @Test
    fun editingAFieldClearsTheRefusal() =
        runTest {
            // Given
            val viewModel = SetupViewModel(startSession = startSession, clock = clock)
            startSession.result = StartSessionResult.Refused(SetupRefusal.NO_STAFF_NUMBER)
            viewModel.onStart()
            advanceUntilIdle()

            // When
            viewModel.onCm1StaffNumberChange("7")

            // Then
            assertEquals(null, viewModel.state.value.refusal)
        }

    @Test
    fun aSecondStartWhileTheFirstIsRunningStartsNoSecondSession() =
        runTest {
            // Given
            val viewModel = SetupViewModel(startSession = startSession, clock = clock)
            val hold = CompletableDeferred<Unit>()
            startSession.hold = hold
            viewModel.onStart()
            advanceUntilIdle()

            // When
            viewModel.onStart()
            hold.complete(Unit)
            advanceUntilIdle()

            // Then
            assertEquals(1, startSession.setups.size)
        }

    @Test
    fun pickingADateUpdatesTheDate() {
        // Given
        val viewModel = SetupViewModel(startSession = startSession, clock = clock)

        // When
        viewModel.onDateChange(LocalDate.of(2026, 11, 1))

        // Then
        assertEquals(LocalDate.of(2026, 11, 1), viewModel.state.value.date)
    }
}
