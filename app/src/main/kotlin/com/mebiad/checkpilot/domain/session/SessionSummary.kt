package com.mebiad.checkpilot.domain.session

/** A Session as the Session list shows it: with how many of the [competencyCount] Competencies have a Grade. */
data class SessionSummary(
    val session: Session,
    val gradedCounts: Map<CrewMember, Int>,
    val competencyCount: Int,
) {
    fun gradedCountOf(crewMember: CrewMember): Int = gradedCounts[crewMember] ?: 0
}
