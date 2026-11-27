package com.mebiad.checkpilot.ui.apps.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mebiad.checkpilot.domain.catalogue.ObserveCatalogueUsecase
import com.mebiad.checkpilot.domain.session.ChangeStaffNumbersUsecase
import com.mebiad.checkpilot.domain.session.CrewMember
import com.mebiad.checkpilot.domain.session.ObserveSessionUsecase
import com.mebiad.checkpilot.domain.session.StaffNumber
import com.mebiad.checkpilot.domain.session.StaffNumberRemoval
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The pop-ups of the capture screen's Staff Number chips, at most one open; null while closed. */
data class StaffNumbersUiState(
    val editing: StaffNumberEdit? = null,
    val confirming: CrewMemberRemoval? = null,
)

/**
 * The Edit Staff Number pop-up of [crewMember]; [isNew] while their slot is empty, so there is nothing to remove.
 * [refusal] says why the last Save or Remove was refused.
 */
data class StaffNumberEdit(
    val crewMember: CrewMember,
    val text: String,
    val isNew: Boolean,
    val refusal: StaffNumberRefusal? = null,
)

/** Asks to confirm removing [crewMember], whose [gradeCount] Grades out of [competencyCount] would be deleted. */
data class CrewMemberRemoval(
    val crewMember: CrewMember,
    val staffNumber: StaffNumber,
    val gradeCount: Int,
    val competencyCount: Int,
)

sealed interface StaffNumberRefusal {
    data object NotFourToSixDigits : StaffNumberRefusal

    data object OnlyCrewMember : StaffNumberRefusal

    data class NotesTagged(
        val count: Int,
    ) : StaffNumberRefusal
}

class StaffNumbersViewModel(
    private val sessionId: String,
    observeSession: ObserveSessionUsecase,
    observeCatalogue: ObserveCatalogueUsecase,
    private val changeStaffNumbers: ChangeStaffNumbersUsecase,
) : ViewModel() {
    private val session = observeSession.session(sessionId).stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private val competencies =
        observeCatalogue.competencies().stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            emptyList()
        )

    private val mutableState = MutableStateFlow(StaffNumbersUiState())
    val state: StateFlow<StaffNumbersUiState> = mutableState.asStateFlow()

    fun onEdit(crewMember: CrewMember) {
        val staffNumber = session.value?.staffNumberOf(crewMember)
        mutableState.update {
            it.copy(
                editing =
                    StaffNumberEdit(
                        crewMember = crewMember,
                        text = staffNumber?.digits.orEmpty(),
                        isNew = staffNumber == null
                    )
            )
        }
    }

    fun onTextChange(text: String) {
        mutableState.update { state -> state.copy(editing = state.editing?.copy(text = text, refusal = null)) }
    }

    fun onSave() {
        val editing = mutableState.value.editing ?: return
        val staffNumber = StaffNumber.parse(editing.text)
        if (staffNumber == null) {
            mutableState.update { it.copy(editing = editing.copy(refusal = StaffNumberRefusal.NotFourToSixDigits)) }
            return
        }
        mutableState.update { it.copy(editing = null) }
        viewModelScope.launch { changeStaffNumbers.set(sessionId, editing.crewMember, staffNumber) }
    }

    fun onRemove() {
        val editing = mutableState.value.editing ?: return
        viewModelScope.launch {
            val removal = changeStaffNumbers.remove(sessionId, editing.crewMember, deleteGrades = false)
            mutableState.value = stateAfter(removal, editing)
        }
    }

    fun onConfirmRemoval() {
        val confirming = mutableState.value.confirming ?: return
        mutableState.value = StaffNumbersUiState()
        viewModelScope.launch {
            val removal = changeStaffNumbers.remove(sessionId, confirming.crewMember, deleteGrades = true)
            // Only a change made since the confirmation opened can refuse it now; reopen the pop-up to say why.
            if (removal != StaffNumberRemoval.Removed) {
                val editing =
                    StaffNumberEdit(
                        crewMember = confirming.crewMember,
                        text = confirming.staffNumber.digits,
                        isNew = false
                    )
                mutableState.value = stateAfter(removal, editing)
            }
        }
    }

    fun onDismiss() {
        mutableState.value = StaffNumbersUiState()
    }

    private fun stateAfter(
        removal: StaffNumberRemoval,
        editing: StaffNumberEdit,
    ): StaffNumbersUiState =
        when (removal) {
            StaffNumberRemoval.Removed -> {
                StaffNumbersUiState()
            }

            StaffNumberRemoval.RefusedOnlyCrewMember -> {
                StaffNumbersUiState(editing = editing.copy(refusal = StaffNumberRefusal.OnlyCrewMember))
            }

            is StaffNumberRemoval.RefusedNotesTagged -> {
                StaffNumbersUiState(editing = editing.copy(refusal = StaffNumberRefusal.NotesTagged(removal.count)))
            }

            is StaffNumberRemoval.ConfirmGradesDeletion -> {
                val staffNumber = session.value?.staffNumberOf(editing.crewMember)
                if (staffNumber == null) {
                    StaffNumbersUiState()
                } else {
                    StaffNumbersUiState(
                        confirming =
                            CrewMemberRemoval(
                                crewMember = editing.crewMember,
                                staffNumber = staffNumber,
                                gradeCount = removal.count,
                                competencyCount = competencies.value.size
                            )
                    )
                }
            }
        }
}
