package com.mebiad.checkpilot.data.local.db

import com.mebiad.checkpilot.data.local.db.dao.CompetencyDao
import com.mebiad.checkpilot.data.local.db.entity.ObservableBehaviourEntity
import com.mebiad.checkpilot.domain.catalogue.CatalogueRepository
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
}

private fun ObservableBehaviourEntity.toObservableBehaviour(): ObservableBehaviour =
    ObservableBehaviour(id = id, competencyId = competencyId, name = name)
