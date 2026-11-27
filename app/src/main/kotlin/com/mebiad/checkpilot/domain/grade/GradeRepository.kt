package com.mebiad.checkpilot.domain.grade

import com.mebiad.checkpilot.domain.session.CrewMember
import kotlinx.coroutines.flow.Flow

interface GradeRepository {
    /** Sets the Grade, replacing the one the Crew Member had for the Competency. */
    suspend fun set(
        sessionId: String,
        competencyId: Long,
        crewMember: CrewMember,
        grade: Grade,
    )

    /** Deletes every Grade the Crew Member has in the Session. */
    suspend fun deleteOf(
        sessionId: String,
        crewMember: CrewMember,
    )

    /** Emits the Session's Grades, again whenever they change. */
    fun observe(sessionId: String): Flow<List<CompetencyGrade>>

    /** Emits the graded counts of every Session and Crew Member with at least one Grade, again whenever they change. */
    fun observeGradedCounts(): Flow<List<GradedCount>>
}
