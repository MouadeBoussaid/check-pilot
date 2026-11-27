package com.mebiad.checkpilot.data.local.db

import com.mebiad.checkpilot.data.local.db.dao.GradeDao
import com.mebiad.checkpilot.data.local.db.entity.GradeEntity
import com.mebiad.checkpilot.domain.grade.CompetencyGrade
import com.mebiad.checkpilot.domain.grade.Grade
import com.mebiad.checkpilot.domain.grade.GradeRepository
import com.mebiad.checkpilot.domain.grade.GradedCount
import com.mebiad.checkpilot.domain.session.CrewMember
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GradeRepositoryImpl(
    private val dao: GradeDao,
) : GradeRepository {
    override suspend fun set(
        sessionId: String,
        competencyId: Long,
        crewMember: CrewMember,
        grade: Grade,
    ) = dao.upsert(
        GradeEntity(
            sessionId = sessionId,
            competencyId = competencyId,
            crewMember = crewMember.name,
            value = grade.name
        )
    )

    override suspend fun deleteOf(
        sessionId: String,
        crewMember: CrewMember,
    ) = dao.deleteOf(sessionId, crewMember.name)

    override fun observe(sessionId: String): Flow<List<CompetencyGrade>> =
        dao.observe(sessionId).map { rows -> rows.map { it.toCompetencyGrade() } }

    override fun observeGradedCounts(): Flow<List<GradedCount>> =
        dao.observeGradedCounts().map { rows ->
            rows.map { GradedCount(it.sessionId, CrewMember.valueOf(it.crewMember), it.count) }
        }
}

// Rows are only ever written from the enums' names, so parsing them back can't fail.
private fun GradeEntity.toCompetencyGrade(): CompetencyGrade =
    CompetencyGrade(
        competencyId = competencyId,
        crewMember = CrewMember.valueOf(crewMember),
        grade = Grade.valueOf(value)
    )
