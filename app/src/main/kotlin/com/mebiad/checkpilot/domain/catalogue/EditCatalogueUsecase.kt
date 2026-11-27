package com.mebiad.checkpilot.domain.catalogue

import kotlinx.coroutines.flow.Flow

/**
 * Every edit returns at once; the edits land in the database in the order they were called. Names are trimmed, and a
 * blank name changes nothing.
 */
interface EditCatalogueUsecase {
    /** Emits how many Sessions use each Competency and OBS BEH, again whenever Notes or Grades change. */
    fun usage(): Flow<CatalogueUsage>

    /** Adds a Competency with no OBS BEH at the end of the Catalogue. */
    fun addCompetency(name: String)

    fun renameCompetency(
        id: Long,
        name: String,
    )

    /** Adds an OBS BEH at the end of the Competency [competencyId]'s. */
    fun addObservableBehaviour(
        competencyId: Long,
        name: String,
    )

    fun renameObservableBehaviour(
        id: Long,
        name: String,
    )

    /** Moves the Competency [id] to [index] in the Catalogue, or to its nearest end, the others keeping their order. */
    fun moveCompetency(
        id: Long,
        index: Int,
    )

    /** Moves the OBS BEH [id] to [index] in its Competency's, or to their nearest end, the others keeping their order. */
    fun moveObservableBehaviour(
        id: Long,
        index: Int,
    )

    /** Deletes the Competency [id] with its OBS BEH, unless a Session uses it. */
    fun deleteCompetency(id: Long)

    /** Deletes the OBS BEH [id], unless a Session uses it. */
    fun deleteObservableBehaviour(id: Long)
}
