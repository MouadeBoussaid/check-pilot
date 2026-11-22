package com.mebiad.checkpilot.domain.session

import app.cash.turbine.test
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class ObserveSessionUsecaseImplTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = SessionRepositoryFake()
    private val usecase = ObserveSessionUsecaseImpl(repository = repository, dispatcher = dispatcher)

    @Test
    fun emitsNullUntilTheSessionIsSavedThenTheSession() =
        runTest(dispatcher) {
            // Given
            val session =
                Session(
                    id = "session-1",
                    createdAt = Instant.parse("2026-11-01T09:00:00Z"),
                    date = LocalDate.of(2026, 11, 1),
                    cm1StaffNumber = StaffNumber.parse("79125"),
                    cm2StaffNumber = null,
                    departure = checkNotNull(AirportCode.parse("DOH")),
                    destination = checkNotNull(AirportCode.parse("SLL"))
                )

            usecase.session("session-1").test {
                // When / Then: before the Session is saved, then after
                assertNull(awaitItem())
                repository.insert(session)
                assertEquals(session, awaitItem())
            }
        }
}
