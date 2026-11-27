package com.mebiad.checkpilot.domain.catalogue

/**
 * How many Sessions use each Competency and OBS BEH, by id: a Session uses a Competency when one of its Notes or Grades
 * refers to it, and an OBS BEH when one of its Notes links it. An id missing from a map is used in no Session.
 */
data class CatalogueUsage(
    val competencies: Map<Long, Int> = emptyMap(),
    val observableBehaviours: Map<Long, Int> = emptyMap(),
) {
    fun sessionsUsingCompetency(id: Long): Int = competencies[id] ?: 0

    fun sessionsUsingObservableBehaviour(id: Long): Int = observableBehaviours[id] ?: 0
}
