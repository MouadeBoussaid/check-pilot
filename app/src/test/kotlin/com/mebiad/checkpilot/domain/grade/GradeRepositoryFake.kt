package com.mebiad.checkpilot.domain.grade

import com.mebiad.checkpilot.domain.session.CrewMember
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class GradeRepositoryFake : GradeRepository {
    private val gradesState = MutableStateFlow<Map<Triple<String, Long, CrewMember>, Grade>>(emptyMap())

    /** The Grades of [sessionId], as set. */
    fun gradesOf(sessionId: String): List<CompetencyGrade> = gradesState.value.toGrades(sessionId)

    override suspend fun set(
        sessionId: String,
        competencyId: Long,
        crewMember: CrewMember,
        grade: Grade,
    ) {
        gradesState.value += Triple(sessionId, competencyId, crewMember) to grade
    }

    override fun observe(sessionId: String): Flow<List<CompetencyGrade>> = gradesState.map { it.toGrades(sessionId) }

    override fun observeGradedCounts(): Flow<List<GradedCount>> =
        gradesState.map { grades ->
            grades.keys
                .groupingBy { (sessionId, _, crewMember) -> sessionId to crewMember }
                .eachCount()
                .map { (key, count) -> GradedCount(sessionId = key.first, crewMember = key.second, count = count) }
        }

    fun reset() {
        gradesState.value = emptyMap()
    }

    private fun Map<Triple<String, Long, CrewMember>, Grade>.toGrades(sessionId: String): List<CompetencyGrade> =
        filterKeys { it.first == sessionId }.map { (key, grade) ->
            CompetencyGrade(competencyId = key.second, crewMember = key.third, grade = grade)
        }
}
