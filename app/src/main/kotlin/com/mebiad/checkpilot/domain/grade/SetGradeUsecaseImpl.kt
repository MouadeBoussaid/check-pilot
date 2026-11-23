package com.mebiad.checkpilot.domain.grade

import com.mebiad.checkpilot.domain.session.CrewMember
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SetGradeUsecaseImpl(
    private val repository: GradeRepository,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : SetGradeUsecase {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    override fun set(
        sessionId: String,
        competencyId: Long,
        crewMember: CrewMember,
        grade: Grade,
    ) {
        scope.launch { repository.set(sessionId, competencyId, crewMember, grade) }
    }
}
