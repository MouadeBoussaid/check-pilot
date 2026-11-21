package com.mebiad.checkpilot.data.local.db

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CatalogueSeedCallbackTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val path by lazy { folder.root.resolve(CheckPilotDatabase.FILE_NAME).absolutePath }
    private val opened = mutableListOf<CheckPilotDatabase>()

    @After
    fun closeDatabases() {
        opened.forEach { it.close() }
    }

    @Test
    fun aNewDatabaseHoldsCompetency1To9EachWithObsBehAToG() {
        // Given
        val database = open()

        // When
        val catalogue = database.catalogue()

        // Then
        val expected =
            (1..9).associate { number ->
                "Competency $number" to listOf("a", "b", "c", "d", "e", "f", "g").map { "$number$it" }
            }
        assertEquals(expected.toList(), catalogue.toList())
    }

    @Test
    fun reopeningTheSameFileDoesNotSeedAgainAfterACompetencyWasRenamed() {
        // Given
        val first = open()
        first.openHelper.writableDatabase.execSQL(
            "UPDATE competency SET name = 'Communication' WHERE name = 'Competency 1'"
        )
        first.close()

        // When
        val catalogue = open().catalogue()

        // Then
        assertEquals(9, catalogue.size)
        assertEquals("Communication", catalogue.keys.first())
        assertEquals(listOf("1a", "1b", "1c", "1d", "1e", "1f", "1g"), catalogue.getValue("Communication"))
    }

    private fun open(): CheckPilotDatabase =
        CheckPilotDatabase
            .build(ApplicationProvider.getApplicationContext(), path, openHelperFactory = null)
            .also { opened += it }

    /** Competency names in Catalogue order, each with its OBS BEH names in order. */
    private fun CheckPilotDatabase.catalogue(): Map<String, List<String>> {
        val catalogue = linkedMapOf<String, MutableList<String>>()
        openHelper.readableDatabase
            .query(
                """
                SELECT c.name, o.name FROM competency c
                LEFT JOIN observable_behaviour o ON o.competencyId = c.id
                ORDER BY c.sortIndex, o.sortIndex
                """.trimIndent()
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    val behaviours = catalogue.getOrPut(cursor.getString(0)) { mutableListOf() }
                    if (!cursor.isNull(1)) behaviours += cursor.getString(1)
                }
            }
        return catalogue
    }
}
