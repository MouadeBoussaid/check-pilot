package com.mebiad.checkpilot.domain.grade

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class ObserveGradesUsecaseFake : ObserveGradesUsecase {
    /** The Grades per Session id. */
    val grades = MutableStateFlow<Map<String, List<CompetencyGrade>>>(emptyMap())

    override fun grades(sessionId: String): Flow<List<CompetencyGrade>> = grades.map { it[sessionId].orEmpty() }
}
