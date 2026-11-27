package com.mebiad.checkpilot.domain.note

import app.cash.turbine.test
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ObserveNotesUsecaseImplTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = NoteRepositoryFake()
    private val usecase = ObserveNotesUsecaseImpl(repository = repository, dispatcher = dispatcher)

    @Test
    fun emitsOnlyTheNotesOfTheSession() =
        runTest(dispatcher) {
            // Given
            val mine = note(id = "a", sessionId = "session-1")
            repository.insert(mine)
            repository.insert(note(id = "b", sessionId = "session-2"))

            usecase.notes("session-1").test {
                // When
                val notes = awaitItem()

                // Then
                assertEquals(listOf(mine), notes)
            }
        }

    private fun note(
        id: String,
        sessionId: String,
    ) = Note(
        id = id,
        sessionId = sessionId,
        competencyId = 1,
        text = "Late flaps",
        cmTag = CmTag.BOTH,
        observableBehaviourId = null,
        sortIndex = 0
    )
}
