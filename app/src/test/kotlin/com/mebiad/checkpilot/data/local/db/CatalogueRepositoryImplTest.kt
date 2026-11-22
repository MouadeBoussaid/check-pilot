package com.mebiad.checkpilot.data.local.db

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
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
}
