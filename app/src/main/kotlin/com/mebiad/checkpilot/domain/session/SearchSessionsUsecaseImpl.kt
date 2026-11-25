package com.mebiad.checkpilot.domain.session

import com.mebiad.checkpilot.domain.catalogue.CatalogueRepository
import com.mebiad.checkpilot.domain.grade.GradeRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import java.time.LocalDate

class SearchSessionsUsecaseImpl(
    private val sessions: SessionRepository,
    private val grades: GradeRepository,
    private val catalogue: CatalogueRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : SearchSessionsUsecase {
    override fun search(
        text: String,
        date: LocalDate?,
    ): Flow<List<SessionSummary>> {
        val filter = SessionFilter.of(text, date) ?: return flowOf(emptyList())
        return combine(
            sessions.search(filter),
            grades.observeGradedCounts(),
            catalogue.observeCompetencies()
        ) { found, counts, competencies ->
            val countsBySession = counts.groupBy { it.sessionId }
            found.map { session ->
                SessionSummary(
                    session = session,
                    gradedCounts = countsBySession[session.id].orEmpty().associate { it.crewMember to it.count },
                    competencyCount = competencies.size
                )
            }
        }.flowOn(dispatcher)
    }
}
