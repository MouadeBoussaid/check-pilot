package com.mebiad.checkpilot.domain.debrief

import com.mebiad.checkpilot.domain.session.CrewMember
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

class ObserveDebriefUsecaseFake : ObserveDebriefUsecase {
    /** The Debriefs per Session id and Crew Member; a missing one never emits. */
    val debriefs = MutableStateFlow<Map<Pair<String, CrewMember>, Debrief>>(emptyMap())

    override fun debrief(
        sessionId: String,
        crewMember: CrewMember,
    ): Flow<Debrief> = debriefs.map { it[sessionId to crewMember] }.filterNotNull()
}
