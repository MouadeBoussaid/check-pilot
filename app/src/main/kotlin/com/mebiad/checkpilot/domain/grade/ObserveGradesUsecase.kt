package com.mebiad.checkpilot.domain.grade

import kotlinx.coroutines.flow.Flow

interface ObserveGradesUsecase {
    /** Emits the Session's Grades, again whenever they change. A Competency without a row has no Grade. */
    fun grades(sessionId: String): Flow<List<CompetencyGrade>>
}
