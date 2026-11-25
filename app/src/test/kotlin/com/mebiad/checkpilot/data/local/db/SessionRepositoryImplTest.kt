package com.mebiad.checkpilot.data.local.db

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mebiad.checkpilot.domain.grade.CompetencyGrade
import com.mebiad.checkpilot.domain.grade.Grade
import com.mebiad.checkpilot.domain.note.CmTag
import com.mebiad.checkpilot.domain.note.Note
import com.mebiad.checkpilot.domain.session.AirportCode
import com.mebiad.checkpilot.domain.session.CrewMember
import com.mebiad.checkpilot.domain.session.Session
import com.mebiad.checkpilot.domain.session.SessionFilter
import com.mebiad.checkpilot.domain.session.StaffNumber
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class SessionRepositoryImplTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val database by lazy {
        CheckPilotDatabase.build(
            context = ApplicationProvider.getApplicationContext(),
            name = folder.root.resolve(CheckPilotDatabase.FILE_NAME).absolutePath,
            openHelperFactory = null
        )
    }
    private val repository by lazy { SessionRepositoryImpl(dao = database.sessionDao()) }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun anInsertedSessionIsObservedAsItWasSaved() =
        runTest {
            // Given
            val session =
                Session(
                    id = "session-1",
                    createdAt = Instant.parse("2026-11-22T09:30:00Z"),
                    date = LocalDate.of(2026, 11, 1),
                    cm1StaffNumber = null,
                    cm2StaffNumber = checkNotNull(StaffNumber.parse("04512")),
                    departure = checkNotNull(AirportCode.parse("DOH")),
                    destination = checkNotNull(AirportCode.parse("SLL"))
                )

            // When
            repository.insert(session)

            // Then
            assertEquals(session, repository.observe("session-1").first())
        }

    @Test
    fun aChangedDateIsObservedAndTheRestOfTheSessionIsKept() =
        runTest {
            // Given
            val session = session(id = "session-1", createdAt = Instant.parse("2026-11-22T09:30:00Z"))
            repository.insert(session)

            // When
            repository.updateDate(id = "session-1", date = LocalDate.of(2026, 11, 5))

            // Then
            assertEquals(session.copy(date = LocalDate.of(2026, 11, 5)), repository.observe("session-1").first())
        }

    @Test
    fun anUnknownSessionIsObservedAsNull() =
        runTest {
            // Given: no Session saved

            // When
            val session = repository.observe("missing").first()

            // Then
            assertNull(session)
        }

    @Test
    fun theLastSessionIsTheLatestCreatedWithTiesGoingToTheLaterInsert() =
        runTest {
            // Given
            val tieTime = Instant.parse("2026-11-22T10:00:00Z")
            repository.insert(session(id = "b-first-at-tie", createdAt = tieTime))
            repository.insert(session(id = "c-older", createdAt = Instant.parse("2026-11-22T08:00:00Z")))
            repository.insert(session(id = "a-second-at-tie", createdAt = tieTime))

            // When
            val last = repository.observeLast().first()

            // Then
            assertEquals("a-second-at-tie", last?.id)
        }

    @Test
    fun withoutSessionsThereIsNoLastSession() =
        runTest {
            // Given: no Session saved

            // When
            val last = repository.observeLast().first()

            // Then
            assertNull(last)
        }

    @Test
    fun withoutConditionsTheSearchListsEverySessionNewestDateFirstThenLatestCreated() =
        runTest {
            // Given
            val tieTime = Instant.parse("2026-11-22T10:00:00Z")
            repository.insert(session(id = "older-date", createdAt = tieTime, date = LocalDate.of(2026, 11, 1)))
            repository.insert(session(id = "tie-first", createdAt = tieTime))
            repository.insert(session(id = "created-earlier", createdAt = tieTime.minusSeconds(60)))
            repository.insert(session(id = "tie-second", createdAt = tieTime))
            repository.insert(session(id = "newer-date", createdAt = tieTime, date = LocalDate.of(2026, 11, 23)))

            // When
            val found = repository.search(SessionFilter()).first()

            // Then
            assertEquals(
                listOf("newer-date", "tie-second", "tie-first", "created-earlier", "older-date"),
                found.map { it.id }
            )
        }

    @Test
    fun aStaffNumberPrefixMatchesTheStartOfEitherCrewMembersStaffNumber() =
        runTest {
            // Given
            repository.insert(session(id = "cm1", cm1 = "79125", cm2 = null))
            repository.insert(session(id = "cm2", cm1 = "66021", cm2 = "79100"))
            repository.insert(session(id = "inside", cm1 = "17912", cm2 = "04512"))

            // When
            val found = repository.search(SessionFilter(staffNumberPrefix = "791")).first()

            // Then
            assertEquals(setOf("cm1", "cm2"), found.map { it.id }.toSet())
        }

    @Test
    fun anAirportPrefixMatchesTheStartOfTheDepartureOrTheDestination() =
        runTest {
            // Given
            repository.insert(session(id = "to", departure = "DOH", destination = "SLL"))
            repository.insert(session(id = "from", departure = "SLL", destination = "DOH"))
            repository.insert(session(id = "neither", departure = "DOH", destination = "LHR"))

            // When
            val found = repository.search(SessionFilter(airportPrefix = "SL")).first()

            // Then
            assertEquals(setOf("to", "from"), found.map { it.id }.toSet())
        }

    @Test
    fun theDateMustMatchExactlyAndEveryConditionMustHold() =
        runTest {
            // Given
            val eighth = LocalDate.of(2026, 11, 8)
            repository.insert(
                session(id = "match", date = eighth, cm1 = "79125", departure = "SLL", destination = "DOH")
            )
            repository.insert(
                session(
                    id = "other-date",
                    date = eighth.plusDays(1),
                    cm1 = "79125",
                    departure = "SLL",
                    destination = "DOH"
                )
            )
            repository.insert(
                session(id = "other-staff", date = eighth, cm1 = "81530", departure = "SLL", destination = "DOH")
            )
            repository.insert(
                session(id = "other-airport", date = eighth, cm1 = "79125", departure = "MCT", destination = "DOH")
            )

            // When
            val found =
                repository
                    .search(SessionFilter(staffNumberPrefix = "79125", airportPrefix = "SLL", date = eighth))
                    .first()

            // Then
            assertEquals(listOf("match"), found.map { it.id })
        }

    @Test
    fun deletingASessionDeletesItsNotesAndGradesAndLeavesTheOthers() =
        runTest {
            // Given
            val notes = NoteRepositoryImpl(dao = database.noteDao())
            val grades = GradeRepositoryImpl(dao = database.gradeDao())
            listOf("deleted", "kept").forEach { id ->
                repository.insert(session(id = id))
                notes.insert(
                    Note(
                        id = "note-$id",
                        sessionId = id,
                        competencyId = 1,
                        text = "Late gear",
                        cmTag = CmTag.BOTH,
                        observableBehaviourId = null,
                        sortIndex = 0
                    )
                )
                grades.set(sessionId = id, competencyId = 1, crewMember = CrewMember.CM1, grade = Grade.ADEQUATE)
            }

            // When
            repository.delete("deleted")

            // Then
            assertEquals(listOf("kept"), repository.search(SessionFilter()).first().map { it.id })
            assertEquals(emptyList<Note>(), notes.observe("deleted").first())
            assertEquals(emptyList<CompetencyGrade>(), grades.observe("deleted").first())
            assertEquals(listOf("note-kept"), notes.observe("kept").first().map { it.id })
            assertEquals(1, grades.observe("kept").first().size)
        }

    private fun session(
        id: String,
        createdAt: Instant = Instant.parse("2026-11-22T09:30:00Z"),
        date: LocalDate = LocalDate.of(2026, 11, 22),
        cm1: String? = "79125",
        cm2: String? = null,
        departure: String = "DOH",
        destination: String = "SLL",
    ): Session =
        Session(
            id = id,
            createdAt = createdAt,
            date = date,
            cm1StaffNumber = cm1?.let { checkNotNull(StaffNumber.parse(it)) },
            cm2StaffNumber = cm2?.let { checkNotNull(StaffNumber.parse(it)) },
            departure = checkNotNull(AirportCode.parse(departure)),
            destination = checkNotNull(AirportCode.parse(destination))
        )
}
