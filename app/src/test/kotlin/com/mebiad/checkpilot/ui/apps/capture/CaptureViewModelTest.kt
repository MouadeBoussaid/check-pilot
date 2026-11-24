package com.mebiad.checkpilot.ui.apps.capture

import com.mebiad.checkpilot.domain.catalogue.Competency
import com.mebiad.checkpilot.domain.catalogue.ObservableBehaviour
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
    fun theCmTagsShowOnlyInASessionWithTwoCrewMembers() =
        runTest {
            // Given
            observeSession.sessions.value = listOf(session)
            val viewModel = subscribedViewModel()

            // When / Then: one Crew Member
            assertEquals(false, viewModel.state.value.showCmTags)

            // When / Then: two Crew Members
            observeSession.sessions.value = listOf(session.copy(cm2StaffNumber = StaffNumber.parse("04512")))
            advanceUntilIdle()
            assertEquals(true, viewModel.state.value.showCmTags)
        }

    @Test
    fun eachNoteShowsItsCmTagAndObsBehAndTheCompetencyItsLinkedObsBehOnceInCatalogueOrder() =
        runTest {
            // Given
            observeSession.sessions.value = listOf(session)
            observeCatalogue.competencies.value = listOf(competency3)
            observeNotes.notes.value =
                listOf(
                    note(id = "a", sortIndex = 0, competencyId = 3, observableBehaviourId = OBS_3C.id),
                    note(id = "b", sortIndex = 1, competencyId = 3, observableBehaviourId = OBS_3A.id),
                    note(id = "c", sortIndex = 2, competencyId = 3, observableBehaviourId = OBS_3C.id),
                    note(id = "d", sortIndex = 3, competencyId = 3, observableBehaviourId = null, cmTag = CmTag.BOTH)
                )

            // When
            val viewModel = subscribedViewModel()

            // Then
            val fields = viewModel.state.value.fieldsOf(3)
            assertEquals(listOf(CmTag.CM2, CmTag.CM2, CmTag.CM2, CmTag.BOTH, CmTag.BOTH), fields.map { it.cmTag })
            assertEquals(listOf(OBS_3C, OBS_3A, OBS_3C, null, null), fields.map { it.observableBehaviour })
            assertEquals(
                listOf(OBS_3A, OBS_3C),
                viewModel.state.value
                    .competencyOf(3)
                    .linkedObservableBehaviours
            )
        }

    @Test
    fun selectingACmTagSavesItAndShowsItBeforeTheDatabaseHasIt() =
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
            viewModel.onCmTagSelect(key = key, cmTag = CmTag.CM1)
            advanceUntilIdle()

            // Then
            assertEquals(EditNotesUsecaseFake.Call.SetCmTag(id = "added-1", cmTag = CmTag.CM1), editNotes.calls.last())
            assertEquals(
                CmTag.CM1,
                viewModel.state.value
                    .fieldsOf(1)[0]
                    .cmTag
            )
        }

    @Test
    fun selectingAnObsBehSavesItAndShowsItBeforeTheDatabaseHasIt() =
        runTest {
            // Given
            observeSession.sessions.value = listOf(session)
            observeCatalogue.competencies.value = listOf(competency3)
            observeNotes.notes.value =
                listOf(note(id = "a", sortIndex = 0, competencyId = 3, observableBehaviourId = OBS_3C.id))
            val viewModel = subscribedViewModel()

            // When / Then: none
            viewModel.onObservableBehaviourSelect(key = "a", observableBehaviourId = null)
            advanceUntilIdle()
            assertEquals(
                EditNotesUsecaseFake.Call.SetObservableBehaviour(id = "a", observableBehaviourId = null),
                editNotes.calls.last()
            )
            assertEquals(
                null,
                viewModel.state.value
                    .fieldsOf(3)[0]
                    .observableBehaviour
            )
            assertEquals(
                emptyList<ObservableBehaviour>(),
                viewModel.state.value
                    .competencyOf(3)
                    .linkedObservableBehaviours
            )

            // When / Then: 3b
            viewModel.onObservableBehaviourSelect(key = "a", observableBehaviourId = OBS_3B.id)
            advanceUntilIdle()
            assertEquals(
                EditNotesUsecaseFake.Call.SetObservableBehaviour(id = "a", observableBehaviourId = OBS_3B.id),
                editNotes.calls.last()
            )
            assertEquals(
                OBS_3B,
                viewModel.state.value
                    .fieldsOf(3)[0]
                    .observableBehaviour
            )
            assertEquals(
                listOf(OBS_3B),
                viewModel.state.value
                    .competencyOf(3)
                    .linkedObservableBehaviours
            )
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
    fun undoAfterADeleteRestoresTheNoteWithTheCmTagAndObsBehItShowed() =
        runTest {
            // Given
            observeSession.sessions.value = listOf(session)
            observeCatalogue.competencies.value = listOf(competency3)
            val saved = note(id = "a", sortIndex = 0, competencyId = 3, observableBehaviourId = OBS_3C.id)
            observeNotes.notes.value = listOf(saved)
            val viewModel = subscribedViewModel()
            val events = eventsOf(viewModel)
            viewModel.onCmTagSelect(key = "a", cmTag = CmTag.CM1)
            viewModel.onObservableBehaviourSelect(key = "a", observableBehaviourId = OBS_3B.id)
            viewModel.onDeleteField("a")
            advanceUntilIdle()
            val erased = events.filterIsInstance<CaptureEvent.NoteErased>().single()

            // When
            viewModel.undo(erased.token)
            advanceUntilIdle()

            // Then
            assertEquals(
                EditNotesUsecaseFake.Call.Restore(saved.copy(cmTag = CmTag.CM1, observableBehaviourId = OBS_3B.id)),
                editNotes.calls.last()
            )
            val field = viewModel.state.value.fieldsOf(3)[0]
            assertEquals(CmTag.CM1, field.cmTag)
            assertEquals(OBS_3B, field.observableBehaviour)
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

    @Test
    fun movingANoteSavesTheMoveAndShowsItLastInTheOtherCompetencyWithoutObsBehBeforeTheDatabaseHasIt() =
        runTest {
            // Given
            observeSession.sessions.value = listOf(session)
            observeCatalogue.competencies.value = listOf(competency3, Competency(id = 5, name = "Leadership"))
            observeNotes.notes.value =
                listOf(
                    note(
                        id = "a",
                        sortIndex = 0,
                        competencyId = 3,
                        text = "Readback",
                        observableBehaviourId = OBS_3C.id
                    ),
                    note(id = "b", sortIndex = 1, competencyId = 3, text = "Late flaps"),
                    note(id = "c", sortIndex = 4, competencyId = 5, text = "Briefed")
                )
            val viewModel = subscribedViewModel()
            val events = eventsOf(viewModel)

            // When
            viewModel.onMoveField(key = "a", competencyId = 5)
            advanceUntilIdle()

            // Then
            assertEquals(EditNotesUsecaseFake.Call.Move(id = "a", competencyId = 5), editNotes.calls.last())
            val state = viewModel.state.value
            assertEquals(listOf("Late flaps", ""), state.textsOf(3))
            assertEquals(listOf("Briefed", "Readback", ""), state.textsOf(5))
            val moved = state.fieldsOf(5)[1]
            assertEquals(CmTag.CM2 to null, moved.cmTag to moved.observableBehaviour)
            assertEquals(emptyList<ObservableBehaviour>(), state.competencyOf(3).linkedObservableBehaviours)
            assertEquals(1, events.filterIsInstance<CaptureEvent.NoteMoved>().size)
        }

    @Test
    fun undoAfterAMoveRestoresTheNoteInItsOldPlaceWithItsObsBehBeforeTheDatabaseHasIt() =
        runTest {
            // Given
            observeSession.sessions.value = listOf(session)
            observeCatalogue.competencies.value = listOf(competency3, Competency(id = 5, name = "Leadership"))
            val moved =
                note(id = "b", sortIndex = 1, competencyId = 3, text = "Readback", observableBehaviourId = OBS_3C.id)
            val before =
                listOf(
                    note(id = "a", sortIndex = 0, competencyId = 3, text = "Late flaps"),
                    moved,
                    note(id = "c", sortIndex = 2, competencyId = 3, text = "Late gear")
                )
            observeNotes.notes.value = before
            val viewModel = subscribedViewModel()
            val events = eventsOf(viewModel)
            viewModel.onMoveField(key = "b", competencyId = 5)
            advanceUntilIdle()
            observeNotes.notes.value =
                before.map {
                    if (it.id ==
                        "b"
                    ) {
                        it.copy(competencyId = 5, observableBehaviourId = null, sortIndex = 0)
                    } else {
                        it
                    }
                }
            advanceUntilIdle()
            val event = events.filterIsInstance<CaptureEvent.NoteMoved>().single()

            // When
            viewModel.undo(event.token)
            advanceUntilIdle()

            // Then
            assertEquals(EditNotesUsecaseFake.Call.Restore(moved), editNotes.calls.last())
            val state = viewModel.state.value
            assertEquals(listOf("Late flaps", "Readback", "Late gear", ""), state.textsOf(3))
            assertEquals(listOf(""), state.textsOf(5))
            assertEquals(OBS_3C, state.fieldsOf(3)[1].observableBehaviour)
            assertEquals(listOf(OBS_3C), state.competencyOf(3).linkedObservableBehaviours)
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

    private fun CaptureUiState.competencyOf(competencyId: Long): CompetencyNotes =
        competencies.single { it.competency.id == competencyId }

    private fun CaptureUiState.fieldsOf(competencyId: Long): List<NoteField> = competencyOf(competencyId).fields

    private fun CaptureUiState.textsOf(competencyId: Long): List<String> = fieldsOf(competencyId).map { it.text }

    private fun note(
        id: String,
        sortIndex: Long,
        text: String = "Checklist read silently",
        competencyId: Long = 1,
        cmTag: CmTag = CmTag.CM2,
        observableBehaviourId: Long? = 7,
    ) = Note(
        id = id,
        sessionId = SESSION_ID,
        competencyId = competencyId,
        text = text,
        cmTag = cmTag,
        observableBehaviourId = observableBehaviourId,
        sortIndex = sortIndex
    )

    private companion object {
        const val SESSION_ID = "session-1"
        val OBS_3A = ObservableBehaviour(id = 31, competencyId = 3, name = "3a")
        val OBS_3B = ObservableBehaviour(id = 32, competencyId = 3, name = "3b")
        val OBS_3C = ObservableBehaviour(id = 33, competencyId = 3, name = "3c")
        val competency3 =
            Competency(id = 3, name = "Situation awareness", observableBehaviours = listOf(OBS_3A, OBS_3B, OBS_3C))
    }
}
