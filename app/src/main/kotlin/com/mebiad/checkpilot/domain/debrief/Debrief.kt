package com.mebiad.checkpilot.domain.debrief

import com.mebiad.checkpilot.domain.catalogue.Competency
import com.mebiad.checkpilot.domain.catalogue.ObservableBehaviour
import com.mebiad.checkpilot.domain.grade.Grade
import com.mebiad.checkpilot.domain.note.Note
import com.mebiad.checkpilot.domain.session.CrewMember

/** One Crew Member's Debrief: every Competency of the Catalogue, the ones with the most evidence first. */
data class Debrief(
    val crewMember: CrewMember,
    val competencies: List<DebriefCompetency>,
) {
    val noteCount: Int
        get() = competencies.sumOf { it.notes.size }

    /** The distinct OBS BEH the Notes about the Crew Member link. */
    val observableBehaviourCount: Int
        get() = competencies.flatMap { it.observableBehaviours }.distinct().size

    /** Competencies with a Grade; Not Observed counts. */
    val gradedCount: Int
        get() = competencies.count { it.grade != null }

    val ungradedCount: Int
        get() = competencies.size - gradedCount
}

/** A Competency in a Debrief, with the [notes] about the Crew Member in their capture order and their [grade]. */
data class DebriefCompetency(
    val competency: Competency,
    val notes: List<DebriefNote>,
    val grade: Grade?,
) {
    /** The distinct OBS BEH the [notes] link, in the order they are first linked. */
    val observableBehaviours: List<ObservableBehaviour>
        get() = notes.mapNotNull { it.observableBehaviour }.distinct()
}

/** A Note about the Crew Member, with the OBS BEH it links, if any. */
data class DebriefNote(
    val note: Note,
    val observableBehaviour: ObservableBehaviour?,
)
