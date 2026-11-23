package com.mebiad.checkpilot.domain.note

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class EditNotesUsecaseImplTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = NoteRepositoryFake()
    private val ids = ArrayDeque(listOf("note-1", "note-2", "note-3"))
    private val usecase =
        EditNotesUsecaseImpl(repository = repository, newId = { ids.removeFirst() }, dispatcher = dispatcher)

    @Test
    fun addReturnsTheNewIdAndSavesTheNoteTaggedBothAtTheEndOfItsCompetency() =
        runTest(dispatcher) {
            // Given
            repository.insert(note(id = "old", competencyId = 1, sortIndex = 4))
            repository.insert(note(id = "other", competencyId = 2, sortIndex = 9))

            // When
            val id = usecase.add(sessionId = SESSION_ID, competencyId = 1, text = "Late flaps")
            advanceUntilIdle()

            // Then
            assertEquals("note-1", id)
            assertEquals(
                Note(
                    id = "note-1",
                    sessionId = SESSION_ID,
                    competencyId = 1,
                    text = "Late flaps",
                    cmTag = CmTag.BOTH,
                    observableBehaviourId = null,
                    sortIndex = 5
                ),
                repository.notes.single { it.id == "note-1" }
            )
        }

    @Test
    fun aTextChangeCalledWhileTheNoteIsStillBeingAddedLandsAfterIt() =
        runTest(dispatcher) {
            // Given
            repository.insertDelayMs = 100
            val id = usecase.add(sessionId = SESSION_ID, competencyId = 1, text = "L")

            // When
            usecase.changeText(id = id, text = "Late flaps")
            advanceUntilIdle()

            // Then
            assertEquals("Late flaps", repository.notes.single { it.id == id }.text)
        }

    @Test
    fun deleteRemovesOnlyThatNote() =
        runTest(dispatcher) {
            // Given
            repository.insert(note(id = "first", competencyId = 1, sortIndex = 0))
            repository.insert(note(id = "second", competencyId = 1, sortIndex = 1))

            // When
            usecase.delete("first")
            advanceUntilIdle()

            // Then
            assertEquals(listOf("second"), repository.notes.map { it.id })
        }

    @Test
    fun restoreWritesTheCopyBackWithItsIdCmTagObsBehAndPlace() =
        runTest(dispatcher) {
            // Given
            val copy = note(id = "second", competencyId = 1, sortIndex = 1, text = "Late flaps")
            repository.insert(copy)
            usecase.delete("second")

            // When
            usecase.restore(copy)
            advanceUntilIdle()

            // Then
            assertEquals(listOf(copy), repository.notes)
        }

    @Test
    fun removeEmptyNotesDeletesTheSessionsEmptyNotesOnly() =
        runTest(dispatcher) {
            // Given
            repository.insert(note(id = "emptied", competencyId = 1, sortIndex = 0, text = ""))
            repository.insert(note(id = "kept", competencyId = 1, sortIndex = 1))
            repository.insert(note(id = "elsewhere", competencyId = 1, sortIndex = 0, text = "").copy(sessionId = "s2"))

            // When
            usecase.removeEmptyNotes(SESSION_ID)
            advanceUntilIdle()

            // Then
            assertEquals(listOf("kept", "elsewhere"), repository.notes.map { it.id })
        }

    @Test
    fun aFailedWriteDoesNotStopTheWritesAfterIt() =
        runTest(dispatcher) {
            // Given
            repository.insert(note(id = "kept", competencyId = 1, sortIndex = 0))
            repository.failingIds = setOf("broken")

            // When
            usecase.restore(note(id = "broken", competencyId = 1, sortIndex = 1))
            usecase.changeText(id = "kept", text = "Late flaps")
            advanceUntilIdle()

            // Then
            assertEquals(listOf("Late flaps"), repository.notes.map { it.text })
        }

    private fun note(
        id: String,
        competencyId: Long,
        sortIndex: Long,
        text: String = "Checklist read silently",
    ) = Note(
        id = id,
        sessionId = SESSION_ID,
        competencyId = competencyId,
        text = text,
        cmTag = CmTag.CM2,
        observableBehaviourId = 7,
        sortIndex = sortIndex
    )

    private companion object {
        const val SESSION_ID = "session-1"
    }
}
