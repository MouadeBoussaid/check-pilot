package com.mebiad.checkpilot.domain.catalogue

import kotlinx.coroutines.flow.Flow

interface CatalogueRepository {
    /** Emits every Competency in Catalogue order, again whenever the Catalogue changes. */
    fun observeCompetencies(): Flow<List<Competency>>

    suspend fun findObservableBehaviour(id: Long): ObservableBehaviour?

    /** Adds a Competency with no OBS BEH after the last one. */
    suspend fun addCompetency(name: String)

    suspend fun renameCompetency(
        id: Long,
        name: String,
    )

    /** Adds an OBS BEH to the Competency [competencyId], after its last one. */
    suspend fun addObservableBehaviour(
        competencyId: Long,
        name: String,
    )

    suspend fun renameObservableBehaviour(
        id: Long,
        name: String,
    )

    /** Puts the Competencies in the order of [ids], which holds every Competency's id. */
    suspend fun reorderCompetencies(ids: List<Long>)

    /** Puts one Competency's OBS BEH in the order of [ids], which holds every one of their ids. */
    suspend fun reorderObservableBehaviours(ids: List<Long>)

    /** Deletes the Competency with its OBS BEH. Fails while a Note or Grade refers to it. */
    suspend fun deleteCompetency(id: Long)

    /** Deletes the OBS BEH. Fails while a Note links it. */
    suspend fun deleteObservableBehaviour(id: Long)

    /** Emits how many Sessions use each Competency and OBS BEH, again whenever Notes or Grades change. */
    fun observeUsage(): Flow<CatalogueUsage>
}
