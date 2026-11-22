package com.mebiad.checkpilot.domain.catalogue

import app.cash.turbine.test
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ObserveCatalogueUsecaseImplTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = CatalogueRepositoryFake()
    private val usecase = ObserveCatalogueUsecaseImpl(repository = repository, dispatcher = dispatcher)

    @Test
    fun emitsTheCompetenciesAgainWhenTheCatalogueChanges() =
        runTest(dispatcher) {
            // Given
            val first = listOf(Competency(id = 1, name = "Competency 1"))
            val renamed = listOf(Competency(id = 1, name = "Communication"))
            repository.competencies.value = first

            usecase.competencies().test {
                // When / Then: the Catalogue as it is, then after a rename
                assertEquals(first, awaitItem())
                repository.competencies.value = renamed
                assertEquals(renamed, awaitItem())
            }
        }
}
