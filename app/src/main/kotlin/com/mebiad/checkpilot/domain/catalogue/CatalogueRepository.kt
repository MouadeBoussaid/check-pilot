package com.mebiad.checkpilot.domain.catalogue

import kotlinx.coroutines.flow.Flow

interface CatalogueRepository {
    /** Emits every Competency in Catalogue order, again whenever the Catalogue changes. */
    fun observeCompetencies(): Flow<List<Competency>>

    suspend fun findObservableBehaviour(id: Long): ObservableBehaviour?
}
