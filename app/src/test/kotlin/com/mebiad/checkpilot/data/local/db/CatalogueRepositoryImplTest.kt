package com.mebiad.checkpilot.data.local.db

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mebiad.checkpilot.domain.catalogue.ObservableBehaviour
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CatalogueRepositoryImplTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val database by lazy {
        CheckPilotDatabase.build(
            context = ApplicationProvider.getApplicationContext(),
            name = folder.root.resolve(CheckPilotDatabase.FILE_NAME).absolutePath,
            openHelperFactory = null
        )
    }
    private val repository by lazy { CatalogueRepositoryImpl(dao = database.competencyDao()) }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun competenciesComeInCatalogueOrderNotIdOrder() =
        runTest {
            // Given: Competency 9 moved to the top of the placeholder Catalogue
            database.openHelper.writableDatabase.execSQL("UPDATE competency SET sortIndex = -1 WHERE id = 9")

            // When
            val competencies = repository.observeCompetencies().first()

            // Then
            assertEquals(
                listOf(9L, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L),
                competencies.map { it.id }
            )
            assertEquals("Competency 9", competencies.first().name)
        }

    @Test
    fun eachCompetencyComesWithItsOwnObservableBehavioursInCatalogueOrder() =
        runTest {
            // Given: 3g moved to the top of Competency 3's OBS BEH
            database.openHelper.writableDatabase.execSQL(
                "UPDATE observable_behaviour SET sortIndex = -1 WHERE name = '3g'"
            )

            // When
            val competency3 = repository.observeCompetencies().first().single { it.id == 3L }

            // Then
            assertEquals(
                listOf("3g", "3a", "3b", "3c", "3d", "3e", "3f"),
                competency3.observableBehaviours.map { it.name }
            )
            assertEquals(setOf(3L), competency3.observableBehaviours.map { it.competencyId }.toSet())
        }

    @Test
    fun findObservableBehaviourReturnsItWithItsCompetencyOrNull() =
        runTest {
            // Given
            val observable3c =
                repository.observeCompetencies().first().single { it.id == 3L }.observableBehaviours.single {
                    it.name == "3c"
                }

            // When
            val found = repository.findObservableBehaviour(observable3c.id)
            val missing = repository.findObservableBehaviour(9999L)

            // Then
            assertEquals(ObservableBehaviour(id = observable3c.id, competencyId = 3L, name = "3c"), found)
            assertEquals(null, missing)
        }
}
