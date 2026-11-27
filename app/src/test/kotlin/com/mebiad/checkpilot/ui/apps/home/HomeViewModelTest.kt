package com.mebiad.checkpilot.ui.apps.home

import app.cash.turbine.test
import com.mebiad.checkpilot.domain.session.AirportCode
import com.mebiad.checkpilot.domain.session.ObserveSessionUsecaseFake
import com.mebiad.checkpilot.domain.session.Session
import com.mebiad.checkpilot.domain.session.StaffNumber
import com.mebiad.checkpilot.testutils.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val observeSession = ObserveSessionUsecaseFake()

    private val session =
        Session(
            id = "session-1",
            createdAt = Instant.parse("2026-11-22T06:00:00Z"),
            date = LocalDate.of(2026, 11, 1),
            cm1StaffNumber = StaffNumber.parse("79125"),
            cm2StaffNumber = StaffNumber.parse("04512"),
            departure = checkNotNull(AirportCode.parse("DOH")),
            destination = checkNotNull(AirportCode.parse("SLL"))
        )

    @Test
    fun theStateFollowsTheLastSession() =
        runTest {
            // Given
            val viewModel = HomeViewModel(observeSession = observeSession)

            viewModel.state.test {
                // When / Then: no Session, then a Session created, then the Session deleted
                assertEquals(HomeUiState(lastSession = null), awaitItem())
                observeSession.last.value = session
                assertEquals(HomeUiState(lastSession = session), awaitItem())
                observeSession.last.value = null
                assertEquals(HomeUiState(lastSession = null), awaitItem())
            }
        }
}
