package com.mebiad.checkpilot.domain.session

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class DeleteSessionUsecaseImplTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = SessionRepositoryFake()
    private val usecase = DeleteSessionUsecaseImpl(repository = repository, dispatcher = dispatcher)

    @Test
    fun theSessionIsDeletedAndTheOthersAreKept() =
        runTest(dispatcher) {
            // Given
            repository.insert(session("deleted"))
            repository.insert(session("kept"))

            // When
            usecase.delete("deleted")
            advanceUntilIdle()

            // Then
            assertEquals(listOf("kept"), repository.sessions.map { it.id })
        }

    @Test
    fun deletingEverySessionIsDoneWhenItReturns() =
        runTest(dispatcher) {
            // Given
            repository.insert(session("first"))
            repository.insert(session("second"))

            // When
            usecase.deleteAll()

            // Then
            assertEquals(emptyList<Session>(), repository.sessions)
        }

    private fun session(id: String): Session =
        Session(
            id = id,
            createdAt = Instant.parse("2026-11-01T08:00:00Z"),
            date = LocalDate.of(2026, 11, 1),
            cm1StaffNumber = StaffNumber.parse("79125"),
            cm2StaffNumber = null,
            departure = checkNotNull(AirportCode.parse("DOH")),
            destination = checkNotNull(AirportCode.parse("SLL"))
        )
}
