package com.mebiad.checkpilot.domain.grade

import com.mebiad.checkpilot.domain.session.CrewMember

interface SetGradeUsecase {
    /** Saves the Crew Member's Grade for the Competency, replacing the one set before. Returns at once. */
    fun set(
        sessionId: String,
        competencyId: Long,
        crewMember: CrewMember,
        grade: Grade,
    )
}
