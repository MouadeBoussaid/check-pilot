package com.mebiad.checkpilot.ui.apps.catalogue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mebiad.checkpilot.domain.catalogue.EditCatalogueUsecase
import com.mebiad.checkpilot.domain.catalogue.ObserveCatalogueUsecase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class CatalogueUiState(
    val competencies: List<CompetencyItem> = emptyList(),
    /** The open dialog that adds or renames an item, or null. */
    val nameDialog: NameDialog? = null,
)

/** What the name dialog adds or renames, and the [name] it opens with. */
sealed interface NameDialog {
    val name: String

    data object AddCompetency : NameDialog {
        override val name = ""
    }

    data class RenameCompetency(
        val id: Long,
        override val name: String,
    ) : NameDialog

    data class AddObservableBehaviour(
        val competencyId: Long,
    ) : NameDialog {
        override val name = ""
    }

    data class RenameObservableBehaviour(
        val id: Long,
        override val name: String,
    ) : NameDialog
}

data class CompetencyItem(
    val id: Long,
    val name: String,
    val usedInSessions: Int,
    val expanded: Boolean,
    val observableBehaviours: List<ObservableBehaviourItem>,
)

data class ObservableBehaviourItem(
    val id: Long,
    val name: String,
    val usedInSessions: Int,
)

class CatalogueViewModel(
    catalogue: ObserveCatalogueUsecase,
    private val edit: EditCatalogueUsecase,
) : ViewModel() {
    private val expanded = MutableStateFlow<Set<Long>>(emptySet())
    private val nameDialog = MutableStateFlow<NameDialog?>(null)

    val state: StateFlow<CatalogueUiState> =
        combine(catalogue.competencies(), edit.usage(), expanded, nameDialog) { competencies, usage, expanded, dialog ->
            CatalogueUiState(
                competencies =
                    competencies.map { competency ->
                        CompetencyItem(
                            id = competency.id,
                            name = competency.name,
                            usedInSessions = usage.sessionsUsingCompetency(competency.id),
                            expanded = competency.id in expanded,
                            observableBehaviours =
                                competency.observableBehaviours.map {
                                    ObservableBehaviourItem(
                                        id = it.id,
                                        name = it.name,
                                        usedInSessions = usage.sessionsUsingObservableBehaviour(it.id)
                                    )
                                }
                        )
                    },
                nameDialog = dialog
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), CatalogueUiState())

    fun onToggle(competencyId: Long) {
        expanded.update { if (competencyId in it) it - competencyId else it + competencyId }
    }

    /** Moves the Competency [by] places down, or up when negative, from where the editor shows it. */
    fun onMoveCompetency(
        id: Long,
        by: Int,
    ) {
        val index = state.value.competencies.indexOfFirst { it.id == id }
        if (index >= 0) edit.moveCompetency(id = id, index = index + by)
    }

    /** Moves the OBS BEH [by] places down, or up when negative, from where the editor shows it. */
    fun onMoveObservableBehaviour(
        id: Long,
        by: Int,
    ) {
        val index =
            state.value.competencies
                .map { competency -> competency.observableBehaviours.indexOfFirst { it.id == id } }
                .firstOrNull { it >= 0 } ?: return
        edit.moveObservableBehaviour(id = id, index = index + by)
    }

    fun onAddCompetency() {
        nameDialog.value = NameDialog.AddCompetency
    }

    fun onRenameCompetency(id: Long) {
        val competency = state.value.competencies.firstOrNull { it.id == id } ?: return
        nameDialog.value = NameDialog.RenameCompetency(id = id, name = competency.name)
    }

    fun onAddObservableBehaviour(competencyId: Long) {
        nameDialog.value = NameDialog.AddObservableBehaviour(competencyId)
    }

    fun onRenameObservableBehaviour(id: Long) {
        val behaviour =
            state.value.competencies
                .flatMap { it.observableBehaviours }
                .firstOrNull { it.id == id } ?: return
        nameDialog.value = NameDialog.RenameObservableBehaviour(id = id, name = behaviour.name)
    }

    fun onNameSave(name: String) {
        when (val dialog = nameDialog.value ?: return) {
            NameDialog.AddCompetency -> edit.addCompetency(name)
            is NameDialog.RenameCompetency -> edit.renameCompetency(id = dialog.id, name = name)
            is NameDialog.AddObservableBehaviour -> edit.addObservableBehaviour(dialog.competencyId, name)
            is NameDialog.RenameObservableBehaviour -> edit.renameObservableBehaviour(id = dialog.id, name = name)
        }
        nameDialog.value = null
    }

    fun onNameDismiss() {
        nameDialog.value = null
    }

    fun onDeleteCompetency(id: Long) = edit.deleteCompetency(id)

    fun onDeleteObservableBehaviour(id: Long) = edit.deleteObservableBehaviour(id)

    private companion object {
        const val STOP_TIMEOUT_MS = 5000L
    }
}
