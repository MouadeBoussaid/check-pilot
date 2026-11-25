package com.mebiad.checkpilot.data.local.db

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mebiad.checkpilot.domain.catalogue.CatalogueUsage
import com.mebiad.checkpilot.domain.catalogue.ObservableBehaviour
import com.mebiad.checkpilot.domain.grade.Grade
import com.mebiad.checkpilot.domain.note.CmTag
import com.mebiad.checkpilot.domain.note.Note
import com.mebiad.checkpilot.domain.session.AirportCode
import com.mebiad.checkpilot.domain.session.CrewMember
import com.mebiad.checkpilot.domain.session.Session
import com.mebiad.checkpilot.domain.session.StaffNumber
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate

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
    private val sessions by lazy { SessionRepositoryImpl(dao = database.sessionDao()) }
    private val notes by lazy { NoteRepositoryImpl(dao = database.noteDao()) }
    private val grades by lazy { GradeRepositoryImpl(dao = database.gradeDao()) }

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

    @Test
    fun anAddedCompetencyComesLastWithNoObsBeh() =
        runTest {
            // Given
            database.openHelper.writableDatabase.execSQL("UPDATE competency SET sortIndex = -1 WHERE id = 9")

            // When
            repository.addCompetency("Workload Management")

            // Then
            val last = repository.observeCompetencies().first().last()
            assertEquals("Workload Management", last.name)
            assertEquals(10L, last.id)
            assertEquals(emptyList<ObservableBehaviour>(), last.observableBehaviours)
        }

    @Test
    fun anAddedObsBehComesLastInItsCompetency() =
        runTest {
            // Given
            database.openHelper.writableDatabase.execSQL(
                "UPDATE observable_behaviour SET sortIndex = 99 WHERE name = '3a'"
            )

            // When
            repository.addObservableBehaviour(competencyId = 3, name = "Monitors fuel state")

            // Then
            val competencies = repository.observeCompetencies().first()
            assertEquals(
                listOf("3b", "3c", "3d", "3e", "3f", "3g", "3a", "Monitors fuel state"),
                competencies.single { it.id == 3L }.observableBehaviours.map { it.name }
            )
            assertEquals(7, competencies.single { it.id == 4L }.observableBehaviours.size)
        }

    @Test
    fun renamingChangesOnlyTheNameOfThatCompetencyOrObsBeh() =
        runTest {
            // Given
            val observable3c = observableBehaviour("3c")

            // When
            repository.renameCompetency(id = 1, name = "Communication")
            repository.renameObservableBehaviour(id = observable3c.id, name = "Monitors fuel state")

            // Then
            val competencies = repository.observeCompetencies().first()
            assertEquals(listOf("Communication") + (2..9).map { "Competency $it" }, competencies.map { it.name })
            assertEquals(
                listOf("3a", "3b", "Monitors fuel state", "3d", "3e", "3f", "3g"),
                competencies.single { it.id == 3L }.observableBehaviours.map { it.name }
            )
        }

    @Test
    fun reorderingPutsCompetenciesAndObsBehInTheOrderGiven() =
        runTest {
            // Given
            val competency3 = repository.observeCompetencies().first().single { it.id == 3L }
            val ids = competency3.observableBehaviours.map { it.id }

            // When
            repository.reorderCompetencies(listOf(9L, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L))
            repository.reorderObservableBehaviours(listOf(ids.last()) + ids.dropLast(1))

            // Then
            val competencies = repository.observeCompetencies().first()
            assertEquals(listOf(9L, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L), competencies.map { it.id })
            assertEquals(
                listOf("3g", "3a", "3b", "3c", "3d", "3e", "3f"),
                competencies.single { it.id == 3L }.observableBehaviours.map { it.name }
            )
        }

    @Test
    fun usageCountsTheSessionsWhoseNotesOrGradesReferToEachItem() =
        runTest {
            // Given
            val observable2c = observableBehaviour("2c")
            val observable2d = observableBehaviour("2d")
            (1..3).forEach { sessions.insert(session("session-$it")) }
            notes.insert(note("note-1", "session-1", competencyId = 2, observableBehaviourId = observable2c.id))
            notes.insert(note("note-2", "session-1", competencyId = 2, observableBehaviourId = observable2c.id))
            notes.insert(note("note-3", "session-2", competencyId = 2, observableBehaviourId = observable2c.id))
            notes.insert(note("note-4", "session-3", competencyId = 2, observableBehaviourId = observable2d.id))
            grades.set("session-1", competencyId = 1, crewMember = CrewMember.CM1, grade = Grade.EFFECTIVE)
            grades.set("session-2", competencyId = 2, crewMember = CrewMember.CM2, grade = Grade.EFFECTIVE)

            // When
            val usage = repository.observeUsage().first()

            // Then
            assertEquals(
                CatalogueUsage(
                    competencies = mapOf(1L to 1, 2L to 3),
                    observableBehaviours = mapOf(observable2c.id to 2, observable2d.id to 1)
                ),
                usage
            )
        }

    @Test
    fun deletingACompetencyTakesItsObsBehWithItAndItsIdIsNeverReused() =
        runTest {
            // Given
            val observable3e = observableBehaviour("3e")

            // When
            repository.deleteObservableBehaviour(observable3e.id)
            repository.deleteCompetency(9)
            repository.addCompetency("Workload Management")

            // Then
            val competencies = repository.observeCompetencies().first()
            assertEquals(listOf(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 10L), competencies.map { it.id })
            assertEquals(
                listOf("3a", "3b", "3c", "3d", "3f", "3g"),
                competencies.single { it.id == 3L }.observableBehaviours.map { it.name }
            )
            assertEquals(
                0L,
                database.openHelper.readableDatabase
                    .query("SELECT COUNT(*) FROM observable_behaviour WHERE competencyId = 9")
                    .use { cursor ->
                        cursor.moveToFirst()
                        cursor.getLong(0)
                    }
            )
        }

    private suspend fun observableBehaviour(name: String): ObservableBehaviour =
        repository
            .observeCompetencies()
            .first()
            .flatMap { it.observableBehaviours }
            .single { it.name == name }

    private fun session(id: String): Session =
        Session(
            id = id,
            createdAt = Instant.parse("2026-11-22T09:30:00Z"),
            date = LocalDate.of(2026, 11, 1),
            cm1StaffNumber = StaffNumber.parse("79125"),
            cm2StaffNumber = StaffNumber.parse("04512"),
            departure = checkNotNull(AirportCode.parse("DOH")),
            destination = checkNotNull(AirportCode.parse("SLL"))
        )

    private fun note(
        id: String,
        sessionId: String,
        competencyId: Long,
        observableBehaviourId: Long?,
    ): Note =
        Note(
            id = id,
            sessionId = sessionId,
            competencyId = competencyId,
            text = "Late readback",
            cmTag = CmTag.BOTH,
            observableBehaviourId = observableBehaviourId,
            sortIndex = 0
        )
}
