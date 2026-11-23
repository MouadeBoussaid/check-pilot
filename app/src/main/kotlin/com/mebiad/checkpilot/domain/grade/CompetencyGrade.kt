package com.mebiad.checkpilot.domain.grade

import com.mebiad.checkpilot.domain.session.CrewMember

/** The [grade] one Crew Member has for the Competency [competencyId] in a Session. */
data class CompetencyGrade(
    val competencyId: Long,
    val crewMember: CrewMember,
    val grade: Grade,
)
