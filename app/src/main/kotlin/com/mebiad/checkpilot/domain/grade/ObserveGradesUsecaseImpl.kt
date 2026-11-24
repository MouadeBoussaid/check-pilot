package com.mebiad.checkpilot.domain.grade

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn

class ObserveGradesUsecaseImpl(
    private val repository: GradeRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ObserveGradesUsecase {
    override fun grades(sessionId: String): Flow<List<CompetencyGrade>> =
        repository.observe(sessionId).flowOn(dispatcher)
}
