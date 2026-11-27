package com.mebiad.checkpilot.ui.apps.capture

import com.mebiad.checkpilot.domain.catalogue.Competency
import com.mebiad.checkpilot.domain.catalogue.ObserveCatalogueUsecaseFake
import com.mebiad.checkpilot.domain.session.AirportCode
import com.mebiad.checkpilot.domain.session.ChangeStaffNumbersUsecaseFake
import com.mebiad.checkpilot.domain.session.CrewMember
import com.mebiad.checkpilot.domain.session.ObserveSessionUsecaseFake
import com.mebiad.checkpilot.domain.session.Session
import com.mebiad.checkpilot.domain.session.StaffNumber
import com.mebiad.checkpilot.domain.session.StaffNumberRemoval
import com.mebiad.checkpilot.testutils.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class StaffNumbersViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val observeSession = ObserveSessionUsecaseFake()
    private val observeCatalogue = ObserveCatalogueUsecaseFake()
    private val changeStaffNumbers = ChangeStaffNumbersUsecaseFake()

    @Test
    fun editingAStaffNumberStartsFromItsDigitsAndEditingAnEmptySlotFromNothing() =
        runTest {
            // Given
            observeSession.sessions.value = listOf(session(cm1 = "79152", cm2 = null))
            val viewModel = viewModel()

            // When / Then: the CM1 chip
            viewModel.onEdit(CrewMember.CM1)
            assertEquals(StaffNumberEdit(CrewMember.CM1, text = "79152", isNew = false), viewModel.state.value.editing)

            // When / Then: Add CM2
            viewModel.onDismiss()
            viewModel.onEdit(CrewMember.CM2)
            assertEquals(StaffNumberEdit(CrewMember.CM2, text = "", isNew = true), viewModel.state.value.editing)
        }

    @Test
    fun savingAValidStaffNumberSetsItAndClosesThePopUp() =
        runTest {
            // Given
            observeSession.sessions.value = listOf(session(cm1 = "79152", cm2 = null))
            val viewModel = viewModel()
            viewModel.onEdit(CrewMember.CM1)

            // When
            viewModel.onTextChange("79125")
            viewModel.onSave()
            advanceUntilIdle()

            // Then
            assertEquals(listOf(Triple(SESSION_ID, CrewMember.CM1, staffNumber("79125"))), changeStaffNumbers.sets)
            assertEquals(StaffNumbersUiState(), viewModel.state.value)
        }

    @Test
    fun savingAStaffNumberWithoutFourToSixDigitsIsRefusedInThePopUp() =
        runTest {
            // Given
            observeSession.sessions.value = listOf(session(cm1 = "79125", cm2 = null))
            val viewModel = viewModel()
            viewModel.onEdit(CrewMember.CM2)

            // When
            viewModel.onTextChange("045")
            viewModel.onSave()
            advanceUntilIdle()

            // Then
            assertEquals(emptyList<Any>(), changeStaffNumbers.sets)
            assertEquals(
                StaffNumberEdit(
                    CrewMember.CM2,
                    text = "045",
                    isNew = true,
                    refusal = StaffNumberRefusal.NotFourToSixDigits
                ),
                viewModel.state.value.editing
            )
        }

    @Test
    fun aRefusedRemovalSaysWhyInThePopUp() =
        runTest {
            // Given
            observeSession.sessions.value = listOf(session(cm1 = "79125", cm2 = "04512"))
            val viewModel = viewModel()
            viewModel.onEdit(CrewMember.CM2)

            // When / Then: Notes are tagged CM2
            changeStaffNumbers.removalKeepingGrades = StaffNumberRemoval.RefusedNotesTagged(count = 2)
            viewModel.onRemove()
            advanceUntilIdle()
            assertEquals(
                StaffNumberRefusal.NotesTagged(count = 2),
                viewModel.state.value.editing
                    ?.refusal
            )

            // When / Then: CM2 is the only Crew Member
            changeStaffNumbers.removalKeepingGrades = StaffNumberRemoval.RefusedOnlyCrewMember
            viewModel.onRemove()
            advanceUntilIdle()
            assertEquals(
                StaffNumberRefusal.OnlyCrewMember,
                viewModel.state.value.editing
                    ?.refusal
            )
        }

    @Test
    fun anAllowedRemovalClosesThePopUp() =
        runTest {
            // Given
            observeSession.sessions.value = listOf(session(cm1 = "79125", cm2 = "04512"))
            val viewModel = viewModel()
            viewModel.onEdit(CrewMember.CM2)

            // When
            viewModel.onRemove()
            advanceUntilIdle()

            // Then
            assertEquals(listOf(Triple(SESSION_ID, CrewMember.CM2, false)), changeStaffNumbers.removals)
            assertEquals(StaffNumbersUiState(), viewModel.state.value)
        }

    @Test
    fun removingACrewMemberWithGradesAsksToConfirmWithTheirGradeCountOutOfEveryCompetency() =
        runTest {
            // Given
            observeSession.sessions.value = listOf(session(cm1 = "79125", cm2 = "04512"))
            observeCatalogue.competencies.value = (1L..9L).map { Competency(id = it, name = "Competency $it") }
            changeStaffNumbers.removalKeepingGrades = StaffNumberRemoval.ConfirmGradesDeletion(count = 4)
            val viewModel = viewModel()
            viewModel.onEdit(CrewMember.CM2)

            // When
            viewModel.onRemove()
            advanceUntilIdle()

            // Then
            assertEquals(
                StaffNumbersUiState(
                    confirming =
                        CrewMemberRemoval(
                            crewMember = CrewMember.CM2,
                            staffNumber = staffNumber("04512"),
                            gradeCount = 4,
                            competencyCount = 9
                        )
                ),
                viewModel.state.value
            )
        }

    @Test
    fun confirmingTheRemovalDeletesTheGradesAndCancellingKeepsThem() =
        runTest {
            // Given
            observeSession.sessions.value = listOf(session(cm1 = "79125", cm2 = "04512"))
            changeStaffNumbers.removalKeepingGrades = StaffNumberRemoval.ConfirmGradesDeletion(count = 4)
            val viewModel = viewModel()

            // When / Then: cancelling closes the confirmation and removes nothing
            viewModel.onEdit(CrewMember.CM2)
            viewModel.onRemove()
            advanceUntilIdle()
            viewModel.onDismiss()
            advanceUntilIdle()
            assertEquals(StaffNumbersUiState(), viewModel.state.value)
            assertEquals(listOf(Triple(SESSION_ID, CrewMember.CM2, false)), changeStaffNumbers.removals)

            // When / Then: confirming removes CM2 with their Grades
            viewModel.onEdit(CrewMember.CM2)
            viewModel.onRemove()
            advanceUntilIdle()
            viewModel.onConfirmRemoval()
            advanceUntilIdle()
            assertEquals(StaffNumbersUiState(), viewModel.state.value)
            assertEquals(Triple(SESSION_ID, CrewMember.CM2, true), changeStaffNumbers.removals.last())
        }

    private fun TestScope.viewModel(): StaffNumbersViewModel {
        val viewModel =
            StaffNumbersViewModel(
                sessionId = SESSION_ID,
                observeSession = observeSession,
                observeCatalogue = observeCatalogue,
                changeStaffNumbers = changeStaffNumbers
            )
        advanceUntilIdle()
        return viewModel
    }

    private fun staffNumber(digits: String): StaffNumber = checkNotNull(StaffNumber.parse(digits))

    private fun session(
        cm1: String?,
        cm2: String?,
    ): Session =
        Session(
            id = SESSION_ID,
            createdAt = Instant.parse("2026-11-22T06:00:00Z"),
            date = LocalDate.of(2026, 11, 22),
            cm1StaffNumber = cm1?.let(::staffNumber),
            cm2StaffNumber = cm2?.let(::staffNumber),
            departure = checkNotNull(AirportCode.parse("DOH")),
            destination = checkNotNull(AirportCode.parse("SLL"))
        )

    private companion object {
        const val SESSION_ID = "session-1"
    }
}
