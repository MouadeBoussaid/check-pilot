package com.mebiad.checkpilot.domain.catalogue

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class EditCatalogueUsecaseFake : EditCatalogueUsecase {
    /** One edit as it was called. */
    sealed interface Edit {
        data class AddCompetency(
            val name: String,
        ) : Edit

        data class RenameCompetency(
            val id: Long,
            val name: String,
        ) : Edit

        data class MoveCompetency(
            val id: Long,
            val index: Int,
        ) : Edit

        data class DeleteCompetency(
            val id: Long,
        ) : Edit

        data class AddObservableBehaviour(
            val competencyId: Long,
            val name: String,
        ) : Edit

        data class RenameObservableBehaviour(
            val id: Long,
            val name: String,
        ) : Edit

        data class MoveObservableBehaviour(
            val id: Long,
            val index: Int,
        ) : Edit

        data class DeleteObservableBehaviour(
            val id: Long,
        ) : Edit
    }

    val usage = MutableStateFlow(CatalogueUsage())

    /** Every edit called, in order. */
    val edits = mutableListOf<Edit>()

    override fun usage(): Flow<CatalogueUsage> = usage

    override fun addCompetency(name: String) {
        edits += Edit.AddCompetency(name)
    }

    override fun renameCompetency(
        id: Long,
        name: String,
    ) {
        edits += Edit.RenameCompetency(id, name)
    }

    override fun addObservableBehaviour(
        competencyId: Long,
        name: String,
    ) {
        edits += Edit.AddObservableBehaviour(competencyId, name)
    }

    override fun renameObservableBehaviour(
        id: Long,
        name: String,
    ) {
        edits += Edit.RenameObservableBehaviour(id, name)
    }

    override fun moveCompetency(
        id: Long,
        index: Int,
    ) {
        edits += Edit.MoveCompetency(id, index)
    }

    override fun moveObservableBehaviour(
        id: Long,
        index: Int,
    ) {
        edits += Edit.MoveObservableBehaviour(id, index)
    }

    override fun deleteCompetency(id: Long) {
        edits += Edit.DeleteCompetency(id)
    }

    override fun deleteObservableBehaviour(id: Long) {
        edits += Edit.DeleteObservableBehaviour(id)
    }

    fun reset() {
        usage.value = CatalogueUsage()
        edits.clear()
    }
}
