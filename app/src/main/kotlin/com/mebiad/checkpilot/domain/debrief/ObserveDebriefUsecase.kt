package com.mebiad.checkpilot.domain.debrief

import com.mebiad.checkpilot.domain.session.CrewMember
import kotlinx.coroutines.flow.Flow

interface ObserveDebriefUsecase {
    /** Emits the [crewMember]'s Debrief of the Session, again whenever the Catalogue, Notes or Grades change. */
    fun debrief(
        sessionId: String,
        crewMember: CrewMember,
    ): Flow<Debrief>
}
