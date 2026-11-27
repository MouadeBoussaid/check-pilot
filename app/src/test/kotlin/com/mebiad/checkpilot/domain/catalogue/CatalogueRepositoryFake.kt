package com.mebiad.checkpilot.domain.catalogue

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class CatalogueRepositoryFake : CatalogueRepository {
    val competencies = MutableStateFlow<List<Competency>>(emptyList())

    /** What [observeUsage] emits. Unlike the database, deleting doesn't check it, so tests see who checks. */
    val usage = MutableStateFlow(CatalogueUsage())

    override fun observeCompetencies(): Flow<List<Competency>> = competencies

    override suspend fun findObservableBehaviour(id: Long): ObservableBehaviour? =
        observableBehaviours().singleOrNull { it.id == id }

    override suspend fun addCompetency(name: String) {
        competencies.value += Competency(id = (competencies.value.maxOfOrNull { it.id } ?: 0) + 1, name = name)
    }

    override suspend fun renameCompetency(
        id: Long,
        name: String,
    ) {
        competencies.value = competencies.value.map { if (it.id == id) it.copy(name = name) else it }
    }

    override suspend fun addObservableBehaviour(
        competencyId: Long,
        name: String,
    ) {
        val id = (observableBehaviours().maxOfOrNull { it.id } ?: 0) + 1
        updateObservableBehaviours(competencyId) { it + ObservableBehaviour(id, competencyId, name) }
    }

    override suspend fun renameObservableBehaviour(
        id: Long,
        name: String,
    ) {
        competencies.value =
            competencies.value.map { competency ->
                competency.copy(
                    observableBehaviours =
                        competency.observableBehaviours.map { if (it.id == id) it.copy(name = name) else it }
                )
            }
    }

    override suspend fun reorderCompetencies(ids: List<Long>) {
        competencies.value = competencies.value.sortedBy { ids.indexOf(it.id) }
    }

    override suspend fun reorderObservableBehaviours(ids: List<Long>) {
        val competencyId = observableBehaviours().first { it.id in ids }.competencyId
        updateObservableBehaviours(competencyId) { behaviours -> behaviours.sortedBy { ids.indexOf(it.id) } }
    }

    override suspend fun deleteCompetency(id: Long) {
        competencies.value = competencies.value.filter { it.id != id }
    }

    override suspend fun deleteObservableBehaviour(id: Long) {
        competencies.value =
            competencies.value.map { competency ->
                competency.copy(observableBehaviours = competency.observableBehaviours.filter { it.id != id })
            }
    }

    override fun observeUsage(): Flow<CatalogueUsage> = usage

    private fun observableBehaviours(): List<ObservableBehaviour> =
        competencies.value.flatMap { it.observableBehaviours }

    private fun updateObservableBehaviours(
        competencyId: Long,
        update: (List<ObservableBehaviour>) -> List<ObservableBehaviour>,
    ) {
        competencies.value =
            competencies.value.map {
                if (it.id == competencyId) it.copy(observableBehaviours = update(it.observableBehaviours)) else it
            }
    }
}
