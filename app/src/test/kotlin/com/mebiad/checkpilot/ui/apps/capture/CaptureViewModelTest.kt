package com.mebiad.checkpilot.ui.apps.capture

import app.cash.turbine.test
import com.mebiad.checkpilot.domain.catalogue.Competency
import com.mebiad.checkpilot.domain.catalogue.ObserveCatalogueUsecaseFake
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

class CaptureViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val observeSession = ObserveSessionUsecaseFake()
    private val observeCatalogue = ObserveCatalogueUsecaseFake()

    private val session =
        Session(
            id = "session-1",
            createdAt = Instant.parse("2026-11-22T06:00:00Z"),
            date = LocalDate.of(2026, 11, 22),
            cm1StaffNumber = StaffNumber.parse("79125"),
            cm2StaffNumber = null,
            departure = checkNotNull(AirportCode.parse("DOH")),
            destination = checkNotNull(AirportCode.parse("SLL"))
        )

    @Test
    fun theStateHoldsTheSessionAndTheCompetenciesInCatalogueOrder() =
        runTest {
            // Given
            observeSession.sessions.value = listOf(session)
            observeCatalogue.competencies.value =
                listOf(Competency(id = 2, name = "Communication"), Competency(id = 1, name = "Leadership"))
            val viewModel =
                CaptureViewModel(
                    sessionId = "session-1",
                    observeSession = observeSession,
                    observeCatalogue = observeCatalogue
                )

            viewModel.state.test {
                // When
                skipItems(1)
                val state = awaitItem()

                // Then
                assertEquals(
                    CaptureUiState(
                        session = session,
                        competencies =
                            listOf(Competency(id = 2, name = "Communication"), Competency(id = 1, name = "Leadership"))
                    ),
                    state
                )
            }
        }
}
