package com.mebiad.checkpilot.domain.session

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class ChangeSessionDateUsecaseImplTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = SessionRepositoryFake()
    private val usecase = ChangeSessionDateUsecaseImpl(repository = repository, dispatcher = dispatcher)

    @Test
    fun theNewDateIsSavedOnTheSession() =
        runTest(dispatcher) {
            // Given
            val session =
                Session(
                    id = "session-1",
                    createdAt = Instant.parse("2026-11-22T09:30:00Z"),
                    date = LocalDate.of(2026, 11, 1),
                    cm1StaffNumber = StaffNumber.parse("79125"),
                    cm2StaffNumber = null,
                    departure = checkNotNull(AirportCode.parse("DOH")),
                    destination = checkNotNull(AirportCode.parse("SLL"))
                )
            repository.insert(session)

            // When
            usecase.change(sessionId = "session-1", date = LocalDate.of(2026, 11, 5))
            advanceUntilIdle()

            // Then
            assertEquals(listOf(session.copy(date = LocalDate.of(2026, 11, 5))), repository.sessions)
        }
}
