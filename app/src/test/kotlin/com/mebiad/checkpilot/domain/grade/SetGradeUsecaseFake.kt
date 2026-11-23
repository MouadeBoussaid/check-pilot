package com.mebiad.checkpilot.domain.grade

import com.mebiad.checkpilot.domain.session.CrewMember

class SetGradeUsecaseFake : SetGradeUsecase {
    /** Each Grade set, in order, with the Session it was set in. */
    val sets = mutableListOf<Pair<String, CompetencyGrade>>()

    override fun set(
        sessionId: String,
        competencyId: Long,
        crewMember: CrewMember,
        grade: Grade,
    ) {
        sets += sessionId to CompetencyGrade(competencyId = competencyId, crewMember = crewMember, grade = grade)
    }

    fun reset() {
        sets.clear()
    }
}
