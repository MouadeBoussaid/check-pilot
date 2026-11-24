package com.mebiad.checkpilot.ui.apps.sessions

import com.mebiad.checkpilot.core.clock.ClockFake
import com.mebiad.checkpilot.domain.pin.PinUsecaseFake
import com.mebiad.checkpilot.domain.session.AirportCode
import com.mebiad.checkpilot.domain.session.DeleteSessionUsecaseFake
import com.mebiad.checkpilot.domain.session.SearchSessionsUsecaseFake
import com.mebiad.checkpilot.domain.session.Session
import com.mebiad.checkpilot.domain.session.SessionSummary
import com.mebiad.checkpilot.domain.session.StaffNumber
import com.mebiad.checkpilot.testutils.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class SessionsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val pin = PinUsecaseFake()
    private val search = SearchSessionsUsecaseFake()
    private val delete = DeleteSessionUsecaseFake()
    private val clock = ClockFake()

    @Test
    fun whileLockedItListsNoSession() =
        runTest {
            // Given
            search.setResults("", null, listOf(summary("a")))

            // When
            val viewModel = shownViewModel()

            // Then
            val state = viewModel.state.value
            assertFalse(state.unlocked)
            assertEquals(emptyList<SessionSummary>(), state.sessions)
        }

    @Test
    fun unlockingListsEverySession() =
        runTest {
            // Given
            search.setResults("", null, listOf(summary("b"), summary("a")))
            val viewModel = shownViewModel()

            // When
            pin.pin = "1234"
            pin.check("1234")
            advanceUntilIdle()

            // Then
            val state = viewModel.state.value
            assertTrue(state.unlocked)
            assertEquals(listOf("b", "a"), state.sessions.map { it.session.id })
        }

    @Test
    fun theSearchTextNarrowsTheList() =
        runTest {
            // Given
            search.setResults("", null, listOf(summary("b"), summary("a")))
            search.setResults("SLL", null, listOf(summary("a")))
            val viewModel = shownUnlockedViewModel()

            // When
            viewModel.onQueryChange("SLL")
            advanceUntilIdle()

            // Then
            val state = viewModel.state.value
            assertEquals("SLL", state.query)
            assertEquals(listOf("a"), state.sessions.map { it.session.id })
        }

    @Test
    fun theDatePickerStartsAtTodayAndNoDateIsPicked() =
        runTest {
            // Given
            clock.setToday(LocalDate.of(2026, 11, 21))

            // When
            val viewModel = shownUnlockedViewModel()

            // Then
            val state = viewModel.state.value
            assertEquals(LocalDate.of(2026, 11, 21), state.today)
            assertNull(state.date)
        }

    @Test
    fun aPickedDateNarrowsTheListAndClearingItListsAllAgain() =
        runTest {
            // Given
            val eighth = LocalDate.of(2026, 11, 8)
            search.setResults("", null, listOf(summary("b"), summary("a")))
            search.setResults("", eighth, listOf(summary("b")))
            val viewModel = shownUnlockedViewModel()

            // When / Then: a date is picked
            viewModel.onDatePick(eighth)
            advanceUntilIdle()
            assertEquals(eighth, viewModel.state.value.date)
            assertEquals(
                listOf("b"),
                viewModel.state.value.sessions
                    .map { it.session.id }
            )

            // When / Then: the date is cleared
            viewModel.onDateClear()
            advanceUntilIdle()
            assertNull(viewModel.state.value.date)
            assertEquals(
                listOf("b", "a"),
                viewModel.state.value.sessions
                    .map { it.session.id }
            )
        }

    @Test
    fun deletingAsksFirstAndCancellingKeepsTheSession() =
        runTest {
            // Given
            val a = summary("a")
            search.setResults("", null, listOf(a))
            val viewModel = shownUnlockedViewModel()

            // When / Then: Delete asks first
            viewModel.onDeleteAsk(a)
            advanceUntilIdle()
            assertEquals(a, viewModel.state.value.pendingDelete)
            assertEquals(emptyList<String>(), delete.deleted)

            // When / Then: Cancel keeps it
            viewModel.onDeleteCancel()
            advanceUntilIdle()
            assertNull(viewModel.state.value.pendingDelete)
            assertEquals(emptyList<String>(), delete.deleted)
        }

    @Test
    fun confirmingTheDeleteDeletesThatSession() =
        runTest {
            // Given
            val a = summary("a")
            search.setResults("", null, listOf(summary("b"), a))
            val viewModel = shownUnlockedViewModel()
            viewModel.onDeleteAsk(a)

            // When
            viewModel.onDeleteConfirm()
            advanceUntilIdle()

            // Then
            assertEquals(listOf("a"), delete.deleted)
            assertNull(viewModel.state.value.pendingDelete)
        }

    private suspend fun TestScope.shownUnlockedViewModel(): SessionsViewModel {
        pin.pin = "1234"
        pin.check("1234")
        return shownViewModel()
    }

    private fun TestScope.shownViewModel(): SessionsViewModel {
        val viewModel = SessionsViewModel(search = search, delete = delete, pin = pin, clock = clock)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
        advanceUntilIdle()
        return viewModel
    }

    private fun summary(
        id: String,
        cm1: String = "79125",
        destination: String = "SLL",
    ): SessionSummary =
        SessionSummary(
            session =
                Session(
                    id = id,
                    createdAt = Instant.parse("2026-11-01T08:00:00Z"),
                    date = LocalDate.of(2026, 11, 1),
                    cm1StaffNumber = checkNotNull(StaffNumber.parse(cm1)),
                    cm2StaffNumber = null,
                    departure = checkNotNull(AirportCode.parse("DOH")),
                    destination = checkNotNull(AirportCode.parse(destination))
                ),
            gradedCounts = emptyMap(),
            competencyCount = 9
        )
}
