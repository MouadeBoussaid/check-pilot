package com.mebiad.checkpilot.domain.grade

import com.mebiad.checkpilot.domain.session.CrewMember

/** How many Competencies have a Grade for one Crew Member of a Session. */
data class GradedCount(
    val sessionId: String,
    val crewMember: CrewMember,
    val count: Int,
)
