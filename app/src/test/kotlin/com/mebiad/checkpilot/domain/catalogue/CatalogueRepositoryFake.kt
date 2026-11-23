package com.mebiad.checkpilot.domain.catalogue

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class CatalogueRepositoryFake : CatalogueRepository {
    val competencies = MutableStateFlow<List<Competency>>(emptyList())

    override fun observeCompetencies(): Flow<List<Competency>> = competencies

    override suspend fun findObservableBehaviour(id: Long): ObservableBehaviour? =
        competencies.value.flatMap { it.observableBehaviours }.singleOrNull { it.id == id }
}
