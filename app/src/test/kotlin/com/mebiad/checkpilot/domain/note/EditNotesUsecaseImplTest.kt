package com.mebiad.checkpilot.domain.note

import com.mebiad.checkpilot.domain.catalogue.CatalogueRepositoryFake
import com.mebiad.checkpilot.domain.catalogue.Competency
import com.mebiad.checkpilot.domain.catalogue.ObservableBehaviour
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class EditNotesUsecaseImplTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = NoteRepositoryFake()
    private val catalogue =
        CatalogueRepositoryFake().apply {
            competencies.value =
                listOf(
                    Competency(id = 1, name = "Competency 1", observableBehaviours = listOf(OBS_1A)),
                    Competency(id = 3, name = "Competency 3", observableBehaviours = listOf(OBS_3C))
                )
        }
    private val ids = ArrayDeque(listOf("note-1", "note-2", "note-3"))
    private val usecase =
        EditNotesUsecaseImpl(
            repository = repository,
            catalogue = catalogue,
            newId = { ids.removeFirst() },
            dispatcher = dispatcher
        )

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
    fun setCmTagTagsOnlyThatNote() =
        runTest(dispatcher) {
            // Given
            repository.insert(note(id = "first", competencyId = 1, sortIndex = 0))
            repository.insert(note(id = "second", competencyId = 1, sortIndex = 1))

            // When
            usecase.setCmTag(id = "first", cmTag = CmTag.CM1)
            advanceUntilIdle()

            // Then
            assertEquals(listOf(CmTag.CM1, CmTag.CM2), repository.notes.map { it.cmTag })
        }

    @Test
    fun setObservableBehaviourLinksOneOfTheNotesCompetencyAndNullUnlinksIt() =
        runTest(dispatcher) {
            // Given
            repository.insert(note(id = "first", competencyId = 3, sortIndex = 0).copy(observableBehaviourId = null))
            repository.insert(note(id = "second", competencyId = 3, sortIndex = 1))

            // When / Then: linked
            usecase.setObservableBehaviour(id = "first", observableBehaviourId = OBS_3C.id)
            advanceUntilIdle()
            assertEquals(listOf(OBS_3C.id, 7L), repository.notes.map { it.observableBehaviourId })

            // When / Then: unlinked
            usecase.setObservableBehaviour(id = "first", observableBehaviourId = null)
            advanceUntilIdle()
            assertEquals(listOf(null, 7L), repository.notes.map { it.observableBehaviourId })
        }

    @Test
    fun setObservableBehaviourIgnoresAnObsBehOfAnotherCompetencyOrNotInTheCatalogue() =
        runTest(dispatcher) {
            // Given
            repository.insert(note(id = "first", competencyId = 3, sortIndex = 0))

            // When
            usecase.setObservableBehaviour(id = "first", observableBehaviourId = OBS_1A.id)
            usecase.setObservableBehaviour(id = "first", observableBehaviourId = 999L)
            advanceUntilIdle()

            // Then
            assertEquals(7L, repository.notes.single().observableBehaviourId)
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
        val OBS_1A = ObservableBehaviour(id = 11, competencyId = 1, name = "1a")
        val OBS_3C = ObservableBehaviour(id = 33, competencyId = 3, name = "3c")
    }
}
