package com.mebiad.checkpilot.ui.apps.capture

import com.mebiad.checkpilot.domain.catalogue.Competency
import com.mebiad.checkpilot.domain.catalogue.ObserveCatalogueUsecaseFake
import com.mebiad.checkpilot.domain.note.CmTag
import com.mebiad.checkpilot.domain.note.EditNotesUsecaseFake
import com.mebiad.checkpilot.domain.note.Note
import com.mebiad.checkpilot.domain.note.ObserveNotesUsecaseFake
import com.mebiad.checkpilot.domain.session.AirportCode
import com.mebiad.checkpilot.domain.session.ObserveSessionUsecaseFake
import com.mebiad.checkpilot.domain.session.Session
import com.mebiad.checkpilot.domain.session.StaffNumber
import com.mebiad.checkpilot.testutils.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class CaptureViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val observeSession = ObserveSessionUsecaseFake()
    private val observeCatalogue = ObserveCatalogueUsecaseFake()
    private val observeNotes = ObserveNotesUsecaseFake()
    private val editNotes = EditNotesUsecaseFake()

    private val session =
        Session(
            id = SESSION_ID,
            createdAt = Instant.parse("2026-11-22T06:00:00Z"),
            date = LocalDate.of(2026, 11, 22),
            cm1StaffNumber = StaffNumber.parse("79125"),
            cm2StaffNumber = null,
            departure = checkNotNull(AirportCode.parse("DOH")),
            destination = checkNotNull(AirportCode.parse("SLL"))
        )

    @Test
    fun eachCompetencyInCatalogueOrderShowsItsNotesThenOneEmptyField() =
        runTest {
            // Given
            observeSession.sessions.value = listOf(session)
            observeCatalogue.competencies.value =
                listOf(Competency(id = 2, name = "Communication"), Competency(id = 1, name = "Leadership"))
            observeNotes.notes.value =
                listOf(note(id = "b", sortIndex = 1, text = "Late flaps"), note(id = "a", sortIndex = 0))

            // When
            val viewModel = subscribedViewModel()

            // Then
            val state = viewModel.state.value
            assertEquals(session, state.session)
            assertEquals(listOf("Communication", "Leadership"), state.competencies.map { it.competency.name })
            assertEquals(listOf(""), state.textsOf(2))
            assertEquals(listOf("Checklist read silently", "Late flaps", ""), state.textsOf(1))
            assertEquals(listOf(true, true, false), state.fieldsOf(1).map { it.isNote })
        }

    @Test
    fun openingTheSessionRemovesItsEmptyNotes() =
        runTest {
            // Given
            observeSession.sessions.value = listOf(session)

            // When
            subscribedViewModel()

            // Then
            assertEquals(listOf(EditNotesUsecaseFake.Call.RemoveEmptyNotes(SESSION_ID)), editNotes.calls)
        }

    @Test
    fun typingIntoTheEmptyFieldMakesItANoteAndAddsAnEmptyFieldBelowBeforeTheNoteIsSaved() =
        runTest {
            // Given
            observeSession.sessions.value = listOf(session)
            observeCatalogue.competencies.value = listOf(Competency(id = 1, name = "Leadership"))
            val viewModel = subscribedViewModel()
            val emptyField =
                viewModel.state.value
                    .fieldsOf(1)
                    .single()

            // When
            viewModel.onFieldTextChange(competencyId = 1, key = emptyField.key, text = "L")
            advanceUntilIdle()

            // Then
            assertEquals(
                EditNotesUsecaseFake.Call.Add(sessionId = SESSION_ID, competencyId = 1, text = "L", id = "added-1"),
                editNotes.calls.last()
            )
            val fields = viewModel.state.value.fieldsOf(1)
            assertEquals(listOf("L", ""), fields.map { it.text })
            assertEquals(listOf(true, false), fields.map { it.isNote })
            assertEquals(emptyField.key, fields[0].key)
            assertEquals(2, fields.map { it.key }.toSet().size)
        }

    @Test
    fun typingIntoANoteSavesTheTextAndShowsItBeforeTheDatabaseHasIt() =
        runTest {
            // Given
            observeSession.sessions.value = listOf(session)
            observeCatalogue.competencies.value = listOf(Competency(id = 1, name = "Leadership"))
            observeNotes.notes.value = listOf(note(id = "a", sortIndex = 0, text = "Late"))
            val viewModel = subscribedViewModel()

            // When
            viewModel.onFieldTextChange(competencyId = 1, key = "a", text = "Late flaps")
            advanceUntilIdle()

            // Then
            assertEquals(EditNotesUsecaseFake.Call.ChangeText(id = "a", text = "Late flaps"), editNotes.calls.last())
            assertEquals(listOf("Late flaps", ""), viewModel.state.value.textsOf(1))
        }

    @Test
    fun theNextCharactersTypedIntoAFieldThatJustBecameANoteChangeThatNote() =
        runTest {
            // Given
            observeSession.sessions.value = listOf(session)
            observeCatalogue.competencies.value = listOf(Competency(id = 1, name = "Leadership"))
            val viewModel = subscribedViewModel()
            val key =
                viewModel.state.value
                    .fieldsOf(1)
                    .single()
                    .key
            viewModel.onFieldTextChange(competencyId = 1, key = key, text = "L")

            // When
            viewModel.onFieldTextChange(competencyId = 1, key = key, text = "La")
            advanceUntilIdle()

            // Then
            assertEquals(EditNotesUsecaseFake.Call.ChangeText(id = "added-1", text = "La"), editNotes.calls.last())
            assertEquals(listOf("La", ""), viewModel.state.value.textsOf(1))
        }

    @Test
    fun anEmptiedNoteThatLosesFocusIsDeletedAndHiddenWithAnUndoEvent() =
        runTest {
            // Given
            observeSession.sessions.value = listOf(session)
            observeCatalogue.competencies.value = listOf(Competency(id = 1, name = "Leadership"))
            observeNotes.notes.value = listOf(note(id = "a", sortIndex = 0), note(id = "b", sortIndex = 1))
            val viewModel = subscribedViewModel()
            val events = eventsOf(viewModel)
            viewModel.onFieldTextChange(competencyId = 1, key = "a", text = "")

            // When
            viewModel.onFieldFocusLost("a")
            advanceUntilIdle()

            // Then
            assertEquals(EditNotesUsecaseFake.Call.Delete("a"), editNotes.calls.last())
            assertEquals(
                listOf("b"),
                viewModel.state.value
                    .fieldsOf(1)
                    .filter { it.isNote }
                    .map { it.key }
            )
            assertEquals(1, events.filterIsInstance<CaptureEvent.NoteErased>().size)
        }

    @Test
    fun aNoteWithTextThatLosesFocusStays() =
        runTest {
            // Given
            observeSession.sessions.value = listOf(session)
            observeCatalogue.competencies.value = listOf(Competency(id = 1, name = "Leadership"))
            observeNotes.notes.value = listOf(note(id = "a", sortIndex = 0))
            val viewModel = subscribedViewModel()
            val events = eventsOf(viewModel)

            // When
            viewModel.onFieldFocusLost("a")
            advanceUntilIdle()

            // Then
            assertEquals(listOf(EditNotesUsecaseFake.Call.RemoveEmptyNotes(SESSION_ID)), editNotes.calls)
            assertEquals(listOf("Checklist read silently", ""), viewModel.state.value.textsOf(1))
            assertEquals(emptyList<CaptureEvent>(), events)
        }

    @Test
    fun undoAfterAnEmptiedNoteRestoresItWithTheTextItHadBeforeItWasEmptied() =
        runTest {
            // Given
            observeSession.sessions.value = listOf(session)
            observeCatalogue.competencies.value = listOf(Competency(id = 1, name = "Leadership"))
            val first = note(id = "a", sortIndex = 0, text = "Checklist read silently")
            val second = note(id = "b", sortIndex = 1, text = "Late flaps")
            observeNotes.notes.value = listOf(first, second)
            val viewModel = subscribedViewModel()
            val events = eventsOf(viewModel)
            viewModel.onFieldTextChange(competencyId = 1, key = "a", text = "Checklist read")
            viewModel.onFieldTextChange(competencyId = 1, key = "a", text = "")
            viewModel.onFieldFocusLost("a")
            observeNotes.notes.value = listOf(second)
            advanceUntilIdle()
            val erased = events.filterIsInstance<CaptureEvent.NoteErased>().single()

            // When
            viewModel.undo(erased.token)
            advanceUntilIdle()

            // Then
            assertEquals(EditNotesUsecaseFake.Call.Restore(first.copy(text = "Checklist read")), editNotes.calls.last())
            assertEquals(listOf("Checklist read", "Late flaps", ""), viewModel.state.value.textsOf(1))
            assertEquals(
                "a",
                viewModel.state.value
                    .fieldsOf(1)[0]
                    .key
            )
        }

    @Test
    fun deletingANoteErasesOnlyThatNoteWithAnUndoEvent() =
        runTest {
            // Given
            observeSession.sessions.value = listOf(session)
            observeCatalogue.competencies.value = listOf(Competency(id = 1, name = "Leadership"))
            observeNotes.notes.value =
                listOf(note(id = "a", sortIndex = 0), note(id = "b", sortIndex = 1), note(id = "c", sortIndex = 2))
            val viewModel = subscribedViewModel()
            val events = eventsOf(viewModel)

            // When
            viewModel.onDeleteField("b")
            advanceUntilIdle()

            // Then
            assertEquals(EditNotesUsecaseFake.Call.Delete("b"), editNotes.calls.last())
            assertEquals(
                listOf("a", "c"),
                viewModel.state.value
                    .fieldsOf(1)
                    .filter { it.isNote }
                    .map { it.key }
            )
            assertEquals(1, events.filterIsInstance<CaptureEvent.NoteErased>().size)
        }

    @Test
    fun undoAfterADeleteRestoresTheNoteWithTheTextItShowed() =
        runTest {
            // Given
            observeSession.sessions.value = listOf(session)
            observeCatalogue.competencies.value = listOf(Competency(id = 1, name = "Leadership"))
            val second = note(id = "b", sortIndex = 1, text = "Late")
            observeNotes.notes.value = listOf(note(id = "a", sortIndex = 0), second)
            val viewModel = subscribedViewModel()
            val events = eventsOf(viewModel)
            viewModel.onFieldTextChange(competencyId = 1, key = "b", text = "Late flaps")
            viewModel.onDeleteField("b")
            advanceUntilIdle()
            val erased = events.filterIsInstance<CaptureEvent.NoteErased>().single()

            // When
            viewModel.undo(erased.token)
            advanceUntilIdle()

            // Then
            assertEquals(EditNotesUsecaseFake.Call.Restore(second.copy(text = "Late flaps")), editNotes.calls.last())
            assertEquals(listOf("Checklist read silently", "Late flaps", ""), viewModel.state.value.textsOf(1))
        }

    @Test
    fun aNewerEraseMakesTheOlderOneFinal() =
        runTest {
            // Given
            observeSession.sessions.value = listOf(session)
            observeCatalogue.competencies.value = listOf(Competency(id = 1, name = "Leadership"))
            observeNotes.notes.value = listOf(note(id = "a", sortIndex = 0), note(id = "b", sortIndex = 1))
            val viewModel = subscribedViewModel()
            val events = eventsOf(viewModel)
            viewModel.onDeleteField("a")
            viewModel.onDeleteField("b")
            advanceUntilIdle()
            val (older, newer) = events.filterIsInstance<CaptureEvent.NoteErased>()

            // When / Then: the older snackbar's Undo does nothing, the newer one's restores its Note
            viewModel.undo(older.token)
            assertEquals(
                listOf<String>(),
                viewModel.state.value
                    .fieldsOf(1)
                    .filter { it.isNote }
                    .map { it.key }
            )
            viewModel.undo(newer.token)
            advanceUntilIdle()
            assertEquals(
                listOf("b"),
                viewModel.state.value
                    .fieldsOf(1)
                    .filter { it.isNote }
                    .map { it.key }
            )
            assertEquals(1, editNotes.calls.filterIsInstance<EditNotesUsecaseFake.Call.Restore>().size)
        }

    @Test
    fun aNoteErasedBeforeItWasSavedGoesWithoutAnUndoEvent() =
        runTest {
            // Given
            observeSession.sessions.value = listOf(session)
            observeCatalogue.competencies.value = listOf(Competency(id = 1, name = "Leadership"))
            val viewModel = subscribedViewModel()
            val events = eventsOf(viewModel)
            val key =
                viewModel.state.value
                    .fieldsOf(1)
                    .single()
                    .key
            viewModel.onFieldTextChange(competencyId = 1, key = key, text = "L")

            // When
            viewModel.onDeleteField(key)
            advanceUntilIdle()

            // Then
            assertEquals(EditNotesUsecaseFake.Call.Delete("added-1"), editNotes.calls.last())
            assertEquals(listOf(""), viewModel.state.value.textsOf(1))
            assertEquals(emptyList<CaptureEvent>(), events)
        }

    private fun TestScope.subscribedViewModel(): CaptureViewModel {
        val viewModel =
            CaptureViewModel(
                sessionId = SESSION_ID,
                observeSession = observeSession,
                observeCatalogue = observeCatalogue,
                observeNotes = observeNotes,
                editNotes = editNotes
            )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
        advanceUntilIdle()
        return viewModel
    }

    private fun TestScope.eventsOf(viewModel: CaptureViewModel): List<CaptureEvent> {
        val events = mutableListOf<CaptureEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.collect { events += it } }
        return events
    }

    private fun CaptureUiState.fieldsOf(competencyId: Long): List<NoteField> =
        competencies.single { it.competency.id == competencyId }.fields

    private fun CaptureUiState.textsOf(competencyId: Long): List<String> = fieldsOf(competencyId).map { it.text }

    private fun note(
        id: String,
        sortIndex: Long,
        text: String = "Checklist read silently",
        competencyId: Long = 1,
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
