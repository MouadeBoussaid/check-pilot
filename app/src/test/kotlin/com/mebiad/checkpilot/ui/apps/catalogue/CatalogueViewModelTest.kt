package com.mebiad.checkpilot.ui.apps.catalogue

import com.mebiad.checkpilot.domain.catalogue.CatalogueUsage
import com.mebiad.checkpilot.domain.catalogue.Competency
import com.mebiad.checkpilot.domain.catalogue.EditCatalogueUsecaseFake
import com.mebiad.checkpilot.domain.catalogue.EditCatalogueUsecaseFake.Edit
import com.mebiad.checkpilot.domain.catalogue.ObservableBehaviour
import com.mebiad.checkpilot.domain.catalogue.ObserveCatalogueUsecaseFake
import com.mebiad.checkpilot.testutils.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CatalogueViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val catalogue = ObserveCatalogueUsecaseFake()
    private val edit = EditCatalogueUsecaseFake()

    @Test
    fun eachItemShowsHowManySessionsUseItAndCompetenciesStartCollapsed() =
        runTest {
            // Given
            catalogue.competencies.value = placeholders()
            edit.usage.value = CatalogueUsage(competencies = mapOf(2L to 3), observableBehaviours = mapOf(22L to 1))

            // When
            val viewModel = shownViewModel()

            // Then
            val competencies = viewModel.state.value.competencies
            assertEquals(listOf(0, 3, 0), competencies.map { it.usedInSessions })
            assertEquals(listOf(0, 1, 0), competencies[1].observableBehaviours.map { it.usedInSessions })
            assertEquals(listOf("2a", "2b", "2c"), competencies[1].observableBehaviours.map { it.name })
            assertEquals(listOf(false, false, false), competencies.map { it.expanded })
        }

    @Test
    fun togglingACompetencyExpandsItThenCollapsesIt() =
        runTest {
            // Given
            catalogue.competencies.value = placeholders()
            val viewModel = shownViewModel()

            // When / Then: expanded
            viewModel.onToggle(2)
            advanceUntilIdle()
            assertEquals(
                listOf(false, true, false),
                viewModel.state.value.competencies
                    .map { it.expanded }
            )

            // When / Then: collapsed again
            viewModel.onToggle(2)
            advanceUntilIdle()
            assertEquals(
                listOf(false, false, false),
                viewModel.state.value.competencies
                    .map { it.expanded }
            )
        }

    @Test
    fun movingByPlacesMovesTheItemFromWhereItIsShown() =
        runTest {
            // Given
            catalogue.competencies.value = placeholders()
            val viewModel = shownViewModel()

            // When
            viewModel.onMoveCompetency(id = 3, by = -1)
            viewModel.onMoveCompetency(id = 1, by = 5)
            viewModel.onMoveObservableBehaviour(id = 23, by = -2)
            viewModel.onMoveObservableBehaviour(id = 31, by = -1)

            // Then
            assertEquals(
                listOf(
                    Edit.MoveCompetency(id = 3, index = 1),
                    Edit.MoveCompetency(id = 1, index = 5),
                    Edit.MoveObservableBehaviour(id = 23, index = 0),
                    Edit.MoveObservableBehaviour(id = 31, index = -1)
                ),
                edit.edits
            )
        }

    @Test
    fun theNameDialogHoldsTheCurrentNameAndSavingAppliesItToWhatItWasOpenedFor() =
        runTest {
            // Given
            catalogue.competencies.value = placeholders()
            val viewModel = shownViewModel()

            // When / Then: renaming a Competency starts from its name
            viewModel.onRenameCompetency(2)
            advanceUntilIdle()
            assertEquals(
                "Competency 2",
                viewModel.state.value.nameDialog
                    ?.name
            )
            viewModel.onNameSave("Communication")
            advanceUntilIdle()
            assertNull(viewModel.state.value.nameDialog)

            // When / Then: renaming an OBS BEH starts from its name
            viewModel.onRenameObservableBehaviour(32)
            advanceUntilIdle()
            assertEquals(
                "3b",
                viewModel.state.value.nameDialog
                    ?.name
            )
            viewModel.onNameSave("Monitors fuel state")

            // When / Then: adding starts empty
            viewModel.onAddCompetency()
            advanceUntilIdle()
            assertEquals(
                "",
                viewModel.state.value.nameDialog
                    ?.name
            )
            viewModel.onNameSave("Workload Management")
            viewModel.onAddObservableBehaviour(competencyId = 3)
            viewModel.onNameSave("Plans ahead")

            // When / Then: a dismissed dialog saves nothing
            viewModel.onRenameCompetency(1)
            viewModel.onNameDismiss()
            advanceUntilIdle()
            assertNull(viewModel.state.value.nameDialog)
            viewModel.onNameSave("Ignored")

            assertEquals(
                listOf(
                    Edit.RenameCompetency(id = 2, name = "Communication"),
                    Edit.RenameObservableBehaviour(id = 32, name = "Monitors fuel state"),
                    Edit.AddCompetency(name = "Workload Management"),
                    Edit.AddObservableBehaviour(competencyId = 3, name = "Plans ahead")
                ),
                edit.edits
            )
        }

    @Test
    fun deletingPassesTheItemOn() =
        runTest {
            // Given
            catalogue.competencies.value = placeholders()
            val viewModel = shownViewModel()

            // When
            viewModel.onDeleteCompetency(3)
            viewModel.onDeleteObservableBehaviour(21)

            // Then
            assertEquals(listOf(Edit.DeleteCompetency(id = 3), Edit.DeleteObservableBehaviour(id = 21)), edit.edits)
        }

    private fun TestScope.shownViewModel(): CatalogueViewModel {
        val viewModel = CatalogueViewModel(catalogue = catalogue, edit = edit)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
        advanceUntilIdle()
        return viewModel
    }

    /** Competency 1 to 3, each with OBS BEH a to c, whose ids are the Competency's number then 1 to 3, like 32 for 3b. */
    private fun placeholders(): List<Competency> =
        (1L..3L).map { number ->
            Competency(
                id = number,
                name = "Competency $number",
                observableBehaviours =
                    "abc".mapIndexed { index, letter ->
                        ObservableBehaviour(
                            id = number * 10 + index + 1,
                            competencyId = number,
                            name = "$number$letter"
                        )
                    }
            )
        }
}
