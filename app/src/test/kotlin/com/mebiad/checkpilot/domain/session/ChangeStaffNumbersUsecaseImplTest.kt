package com.mebiad.checkpilot.domain.session

import com.mebiad.checkpilot.domain.grade.CompetencyGrade
import com.mebiad.checkpilot.domain.grade.Grade
import com.mebiad.checkpilot.domain.grade.GradeRepositoryFake
import com.mebiad.checkpilot.domain.note.CmTag
import com.mebiad.checkpilot.domain.note.Note
import com.mebiad.checkpilot.domain.note.NoteRepositoryFake
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class ChangeStaffNumbersUsecaseImplTest {
    private val dispatcher = StandardTestDispatcher()
    private val sessions = SessionRepositoryFake()
    private val notes = NoteRepositoryFake()
    private val grades = GradeRepositoryFake()
    private val usecase =
        ChangeStaffNumbersUsecaseImpl(
            sessions = sessions,
            notes = notes,
            grades = grades,
            dispatcher = dispatcher
        )

    @Test
    fun aStaffNumberIsAddedToTheEmptySlotAndCorrectedInTheOther() =
        runTest(dispatcher) {
            // Given
            sessions.insert(session(cm1 = "79152", cm2 = null))

            // When
            usecase.set(SESSION, CrewMember.CM1, staffNumber("79125"))
            usecase.set(SESSION, CrewMember.CM2, staffNumber("04512"))

            // Then
            assertEquals(listOf(session(cm1 = "79125", cm2 = "04512")), sessions.sessions)
        }

    @Test
    fun removingTheOnlyCrewMemberIsRefused() =
        runTest(dispatcher) {
            // Given
            sessions.insert(session(cm1 = null, cm2 = "04512"))

            // When
            val removal = usecase.remove(SESSION, CrewMember.CM2, deleteGrades = true)

            // Then
            assertEquals(StaffNumberRemoval.RefusedOnlyCrewMember, removal)
            assertEquals(listOf(session(cm1 = null, cm2 = "04512")), sessions.sessions)
        }

    @Test
    fun removingACrewMemberWithoutNotesOrGradesEmptiesTheirSlot() =
        runTest(dispatcher) {
            // Given
            sessions.insert(session(cm1 = "79125", cm2 = "04512"))

            // When
            val removal = usecase.remove(SESSION, CrewMember.CM1, deleteGrades = false)

            // Then
            assertEquals(StaffNumberRemoval.Removed, removal)
            assertEquals(listOf(session(cm1 = null, cm2 = "04512")), sessions.sessions)
        }

    @Test
    fun removingACrewMemberWithNotesTaggedToThemIsRefusedWithTheirCount() =
        runTest(dispatcher) {
            // Given
            sessions.insert(session(cm1 = "79125", cm2 = "04512"))
            notes.insert(note("note-1", SESSION, CmTag.CM2))
            notes.insert(note("note-2", SESSION, CmTag.CM2))
            notes.insert(note("note-3", SESSION, CmTag.BOTH))
            notes.insert(note("note-4", SESSION, CmTag.CM1))
            notes.insert(note("note-5", "other-session", CmTag.CM2))

            // When
            val removal = usecase.remove(SESSION, CrewMember.CM2, deleteGrades = true)

            // Then
            assertEquals(StaffNumberRemoval.RefusedNotesTagged(count = 2), removal)
            assertEquals(listOf(session(cm1 = "79125", cm2 = "04512")), sessions.sessions)
        }

    @Test
    fun removingACrewMemberWithGradesAsksToConfirmTheirDeletionFirst() =
        runTest(dispatcher) {
            // Given
            sessions.insert(session(cm1 = "79125", cm2 = "04512"))
            (1L..4L).forEach {
                grades.set(
                    SESSION,
                    competencyId = it,
                    crewMember = CrewMember.CM2,
                    grade = Grade.EFFECTIVE
                )
            }
            grades.set(SESSION, competencyId = 1, crewMember = CrewMember.CM1, grade = Grade.ADEQUATE)

            // When
            val removal = usecase.remove(SESSION, CrewMember.CM2, deleteGrades = false)

            // Then
            assertEquals(StaffNumberRemoval.ConfirmGradesDeletion(count = 4), removal)
            assertEquals(listOf(session(cm1 = "79125", cm2 = "04512")), sessions.sessions)
            assertEquals(5, grades.gradesOf(SESSION).size)
        }

    @Test
    fun removingACrewMemberWithGradesOnceConfirmedDeletesTheirGradesOnly() =
        runTest(dispatcher) {
            // Given
            sessions.insert(session(cm1 = "79125", cm2 = "04512"))
            (1L..4L).forEach {
                grades.set(
                    SESSION,
                    competencyId = it,
                    crewMember = CrewMember.CM2,
                    grade = Grade.EFFECTIVE
                )
            }
            grades.set(SESSION, competencyId = 1, crewMember = CrewMember.CM1, grade = Grade.ADEQUATE)

            // When
            val removal = usecase.remove(SESSION, CrewMember.CM2, deleteGrades = true)

            // Then
            assertEquals(StaffNumberRemoval.Removed, removal)
            assertEquals(listOf(session(cm1 = "79125", cm2 = null)), sessions.sessions)
            assertEquals(
                listOf(CompetencyGrade(competencyId = 1, crewMember = CrewMember.CM1, grade = Grade.ADEQUATE)),
                grades.gradesOf(SESSION)
            )
        }

    private fun note(
        id: String,
        sessionId: String,
        cmTag: CmTag,
    ): Note =
        Note(
            id = id,
            sessionId = sessionId,
            competencyId = 1,
            text = "Readback incomplete",
            cmTag = cmTag,
            observableBehaviourId = null,
            sortIndex = 0
        )

    private fun staffNumber(digits: String): StaffNumber = checkNotNull(StaffNumber.parse(digits))

    private fun session(
        cm1: String?,
        cm2: String?,
    ): Session =
        Session(
            id = SESSION,
            createdAt = Instant.parse("2026-11-22T09:30:00Z"),
            date = LocalDate.of(2026, 11, 22),
            cm1StaffNumber = cm1?.let(::staffNumber),
            cm2StaffNumber = cm2?.let(::staffNumber),
            departure = checkNotNull(AirportCode.parse("DOH")),
            destination = checkNotNull(AirportCode.parse("SLL"))
        )

    private companion object {
        const val SESSION = "session-1"
    }
}
