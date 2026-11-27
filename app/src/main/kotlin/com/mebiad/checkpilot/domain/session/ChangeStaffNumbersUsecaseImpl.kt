package com.mebiad.checkpilot.domain.session

import com.mebiad.checkpilot.domain.grade.GradeRepository
import com.mebiad.checkpilot.domain.note.NoteRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class ChangeStaffNumbersUsecaseImpl(
    private val sessions: SessionRepository,
    private val notes: NoteRepository,
    private val grades: GradeRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ChangeStaffNumbersUsecase {
    override suspend fun set(
        sessionId: String,
        crewMember: CrewMember,
        staffNumber: StaffNumber,
    ) = withContext(dispatcher) { sessions.updateStaffNumber(sessionId, crewMember, staffNumber) }

    override suspend fun remove(
        sessionId: String,
        crewMember: CrewMember,
        deleteGrades: Boolean,
    ): StaffNumberRemoval =
        withContext(dispatcher) {
            val session = sessions.observe(sessionId).first() ?: return@withContext StaffNumberRemoval.Removed
            if (session.crewMembers == listOf(crewMember)) return@withContext StaffNumberRemoval.RefusedOnlyCrewMember
            val tagged = notes.observe(sessionId).first().count { it.cmTag.name == crewMember.name }
            if (tagged > 0) return@withContext StaffNumberRemoval.RefusedNotesTagged(tagged)
            val graded = grades.observe(sessionId).first().count { it.crewMember == crewMember }
            if (graded > 0 && !deleteGrades) return@withContext StaffNumberRemoval.ConfirmGradesDeletion(graded)
            if (graded > 0) grades.deleteOf(sessionId, crewMember)
            sessions.updateStaffNumber(sessionId, crewMember, staffNumber = null)
            StaffNumberRemoval.Removed
        }
}
