package com.mebiad.checkpilot.data.local.db

import com.mebiad.checkpilot.data.local.db.dao.CompetencyDao
import com.mebiad.checkpilot.data.local.db.entity.ObservableBehaviourEntity
import com.mebiad.checkpilot.domain.catalogue.CatalogueRepository
import com.mebiad.checkpilot.domain.catalogue.CatalogueUsage
import com.mebiad.checkpilot.domain.catalogue.Competency
import com.mebiad.checkpilot.domain.catalogue.ObservableBehaviour
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class CatalogueRepositoryImpl(
    private val dao: CompetencyDao,
) : CatalogueRepository {
    override fun observeCompetencies(): Flow<List<Competency>> =
        combine(dao.observeAll(), dao.observeObservableBehaviours()) { competencies, observableBehaviours ->
            val byCompetency = observableBehaviours.groupBy { it.competencyId }
            competencies.map { competency ->
                Competency(
                    id = competency.id,
                    name = competency.name,
                    observableBehaviours = byCompetency[competency.id].orEmpty().map { it.toObservableBehaviour() }
                )
            }
        }

    override suspend fun findObservableBehaviour(id: Long): ObservableBehaviour? =
        dao.findObservableBehaviour(id)?.toObservableBehaviour()

    override suspend fun addCompetency(name: String) = dao.insertLast(name)

    override suspend fun renameCompetency(
        id: Long,
        name: String,
    ) = dao.rename(id = id, name = name)

    override suspend fun addObservableBehaviour(
        competencyId: Long,
        name: String,
    ) = dao.insertObservableBehaviourLast(competencyId = competencyId, name = name)

    override suspend fun renameObservableBehaviour(
        id: Long,
        name: String,
    ) = dao.renameObservableBehaviour(id = id, name = name)

    override suspend fun reorderCompetencies(ids: List<Long>) = dao.reorder(ids)

    override suspend fun reorderObservableBehaviours(ids: List<Long>) = dao.reorderObservableBehaviours(ids)

    override suspend fun deleteCompetency(id: Long) = dao.delete(id)

    override suspend fun deleteObservableBehaviour(id: Long) = dao.deleteObservableBehaviour(id)

    override fun observeUsage(): Flow<CatalogueUsage> =
        combine(dao.observeCompetencyUsage(), dao.observeObservableBehaviourUsage()) { competencies, behaviours ->
            CatalogueUsage(
                competencies = competencies.associate { it.id to it.sessions },
                observableBehaviours = behaviours.associate { it.id to it.sessions }
            )
        }
}

private fun ObservableBehaviourEntity.toObservableBehaviour(): ObservableBehaviour =
    ObservableBehaviour(id = id, competencyId = competencyId, name = name)
