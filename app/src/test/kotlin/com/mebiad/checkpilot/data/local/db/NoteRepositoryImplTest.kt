package com.mebiad.checkpilot.data.local.db

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mebiad.checkpilot.domain.note.CmTag
import com.mebiad.checkpilot.domain.note.Note
import com.mebiad.checkpilot.domain.session.AirportCode
import com.mebiad.checkpilot.domain.session.Session
import com.mebiad.checkpilot.domain.session.StaffNumber
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class NoteRepositoryImplTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val database by lazy {
        CheckPilotDatabase.build(
            context = ApplicationProvider.getApplicationContext(),
            name = folder.root.resolve(CheckPilotDatabase.FILE_NAME).absolutePath,
            openHelperFactory = null
        )
    }
    private val repository by lazy { NoteRepositoryImpl(dao = database.noteDao()) }

    @Before
    fun seedSessions() =
        runTest {
            val sessions = SessionRepositoryImpl(dao = database.sessionDao())
            sessions.insert(session(id = "session-1"))
            sessions.insert(session(id = "session-2"))
        }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun insertedNotesAreObservedPerSessionInSortIndexOrder() =
        runTest {
            // Given
            val second = note(id = "b", sortIndex = 2, cmTag = CmTag.CM2, observableBehaviourId = 2)
            val first = note(id = "a", sortIndex = 1)
            val otherSession = note(id = "c", sessionId = "session-2", sortIndex = 0)

            // When
            repository.insert(second)
            repository.insert(first)
            repository.insert(otherSession)

            // Then
            assertEquals(listOf(first, second), repository.observe("session-1").first())
        }

    @Test
    fun updatingTheTextChangesOnlyThatNotesText() =
        runTest {
            // Given
            val note = note(id = "a", sortIndex = 1, cmTag = CmTag.CM1, observableBehaviourId = 3)
            repository.insert(note)
            repository.insert(note(id = "b", sortIndex = 2))

            // When
            repository.updateText(id = "a", text = "Late flaps")

            // Then
            assertEquals(
                listOf(note.copy(text = "Late flaps"), note(id = "b", sortIndex = 2)),
                repository.observe("session-1").first()
            )
        }

    @Test
    fun aDeletedNoteIsGoneAndCanBeInsertedAgainWithTheSameId() =
        runTest {
            // Given
            val note = note(id = "a", sortIndex = 1, cmTag = CmTag.CM2, observableBehaviourId = 2)
            repository.insert(note)

            // When / Then: deleted
            repository.delete("a")
            assertEquals(emptyList<Note>(), repository.observe("session-1").first())

            // When / Then: inserted again
            repository.insert(note)
            assertEquals(listOf(note), repository.observe("session-1").first())
        }

    @Test
    fun deletingTheEmptyNotesOfASessionKeepsTheOthers() =
        runTest {
            // Given
            repository.insert(note(id = "empty", sortIndex = 1, text = ""))
            repository.insert(note(id = "kept", sortIndex = 2))
            repository.insert(note(id = "other-session-empty", sessionId = "session-2", sortIndex = 1, text = ""))

            // When
            repository.deleteEmpty("session-1")

            // Then
            assertEquals(listOf("kept"), repository.observe("session-1").first().map { it.id })
            assertEquals(listOf("other-session-empty"), repository.observe("session-2").first().map { it.id })
        }

    @Test
    fun theHighestSortIndexIsPerSessionAndCompetency() =
        runTest {
            // Given
            repository.insert(note(id = "a", sortIndex = 4))
            repository.insert(note(id = "b", sortIndex = 7))
            repository.insert(note(id = "c", competencyId = 2, sortIndex = 9))
            repository.insert(note(id = "d", sessionId = "session-2", sortIndex = 11))

            // When
            val highest = repository.highestSortIndex(sessionId = "session-1", competencyId = 1)
            val none = repository.highestSortIndex(sessionId = "session-1", competencyId = 3)

            // Then
            assertEquals(7L, highest)
            assertEquals(null, none)
        }

    private fun note(
        id: String,
        sortIndex: Long,
        sessionId: String = "session-1",
        competencyId: Long = 1,
        text: String = "Note $id",
        cmTag: CmTag = CmTag.BOTH,
        observableBehaviourId: Long? = null,
    ): Note =
        Note(
            id = id,
            sessionId = sessionId,
            competencyId = competencyId,
            text = text,
            cmTag = cmTag,
            observableBehaviourId = observableBehaviourId,
            sortIndex = sortIndex
        )

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
}
