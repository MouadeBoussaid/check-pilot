package com.mebiad.checkpilot.domain.catalogue

import app.cash.turbine.test
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class EditCatalogueUsecaseImplTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = CatalogueRepositoryFake()
    private val usecase = EditCatalogueUsecaseImpl(repository = repository, dispatcher = dispatcher)

    @Test
    fun namesAreTrimmedAndABlankNameChangesNothing() =
        runTest(dispatcher) {
            // Given
            repository.competencies.value = placeholders()

            // When
            usecase.renameCompetency(id = 1, name = "  Communication ")
            usecase.renameCompetency(id = 2, name = "   ")
            usecase.addCompetency(" Workload Management ")
            usecase.addCompetency("")
            usecase.renameObservableBehaviour(id = 32, name = " Monitors fuel state ")
            usecase.renameObservableBehaviour(id = 33, name = " ")
            usecase.addObservableBehaviour(competencyId = 3, name = " Plans ahead ")
            usecase.addObservableBehaviour(competencyId = 3, name = "\n")
            advanceUntilIdle()

            // Then
            val competencies = repository.competencies.value
            assertEquals(
                listOf("Communication", "Competency 2", "Competency 3", "Workload Management"),
                competencies.map { it.name }
            )
            assertEquals(
                listOf("3a", "Monitors fuel state", "3c", "Plans ahead"),
                competencies.single { it.id == 3L }.observableBehaviours.map { it.name }
            )
        }

    @Test
    fun movingPutsTheItemAtItsNewPlaceAndKeepsTheOthersInOrder() =
        runTest(dispatcher) {
            // Given
            repository.competencies.value = placeholders()

            // When
            usecase.moveCompetency(id = 3, index = 0)
            usecase.moveCompetency(id = 1, index = 1)
            usecase.moveObservableBehaviour(id = 33, index = 0)
            usecase.moveObservableBehaviour(id = 21, index = 99)
            advanceUntilIdle()

            // Then
            val competencies = repository.competencies.value
            assertEquals(listOf(3L, 1L, 2L), competencies.map { it.id })
            assertEquals(
                listOf("3c", "3a", "3b"),
                competencies.single { it.id == 3L }.observableBehaviours.map { it.name }
            )
            assertEquals(
                listOf("2b", "2c", "2a"),
                competencies.single { it.id == 2L }.observableBehaviours.map { it.name }
            )
        }

    @Test
    fun onlyItemsNoSessionUsesAreDeleted() =
        runTest(dispatcher) {
            // Given: Competency 1 and OBS BEH 2a are used
            repository.competencies.value = placeholders()
            repository.usage.value =
                CatalogueUsage(competencies = mapOf(1L to 2), observableBehaviours = mapOf(21L to 1))

            // When
            usecase.deleteCompetency(1)
            usecase.deleteCompetency(3)
            usecase.deleteObservableBehaviour(21)
            usecase.deleteObservableBehaviour(22)
            advanceUntilIdle()

            // Then
            val competencies = repository.competencies.value
            assertEquals(listOf(1L, 2L), competencies.map { it.id })
            assertEquals(listOf("2a", "2c"), competencies.single { it.id == 2L }.observableBehaviours.map { it.name })
        }

    @Test
    fun usageFollowsTheNotesAndGrades() =
        runTest(dispatcher) {
            // Given
            val used = CatalogueUsage(competencies = mapOf(2L to 4), observableBehaviours = mapOf(22L to 4))

            usecase.usage().test {
                // When / Then: nothing used, then Competency 2 and 2b used in 4 Sessions
                assertEquals(CatalogueUsage(), awaitItem())
                repository.usage.value = used
                assertEquals(used, awaitItem())
            }
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
