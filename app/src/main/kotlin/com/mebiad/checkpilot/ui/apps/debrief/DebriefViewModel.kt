package com.mebiad.checkpilot.ui.apps.debrief

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mebiad.checkpilot.domain.debrief.Debrief
import com.mebiad.checkpilot.domain.debrief.ObserveDebriefUsecase
import com.mebiad.checkpilot.domain.grade.Grade
import com.mebiad.checkpilot.domain.grade.SetGradeUsecase
import com.mebiad.checkpilot.domain.session.ChangeSessionDateUsecase
import com.mebiad.checkpilot.domain.session.CrewMember
import com.mebiad.checkpilot.domain.session.ObserveSessionUsecase
import com.mebiad.checkpilot.domain.session.Session
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

/**
 * [debrief] is the [selected] Crew Member's, null until it has loaded. [ungradedWarning] is the number of Competencies
 * without a Grade while the message asking to leave or stay shows, null otherwise.
 */
data class DebriefUiState(
    val selected: CrewMember,
    val session: Session? = null,
    val debrief: Debrief? = null,
    val ungradedWarning: Int? = null,
)

sealed interface DebriefEvent {
    data object Leave : DebriefEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
class DebriefViewModel(
    private val sessionId: String,
    crewMember: CrewMember,
    observeSession: ObserveSessionUsecase,
    observeDebrief: ObserveDebriefUsecase,
    private val setGrade: SetGradeUsecase,
    private val changeSessionDate: ChangeSessionDateUsecase,
) : ViewModel() {
    private val selected = MutableStateFlow(crewMember)
    private val ungradedWarning = MutableStateFlow<Int?>(null)

    private val mutableEvents = MutableSharedFlow<DebriefEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<DebriefEvent> = mutableEvents.asSharedFlow()

    private val session = observeSession.session(sessionId).shareIn(viewModelScope, SharingStarted.WhileSubscribed(), 1)

    // Every Crew Member's Debrief is watched, so the toggle switches without a moment of an empty Debrief.
    private val debriefs: Flow<Map<CrewMember, Debrief>> =
        session
            .map { it?.crewMembers.orEmpty() }
            .distinctUntilChanged()
            .flatMapLatest { crewMembers ->
                if (crewMembers.isEmpty()) {
                    flowOf(emptyMap())
                } else {
                    combine(crewMembers.map { observeDebrief.debrief(sessionId, it) }) { debriefs ->
                        debriefs.associateBy { it.crewMember }
                    }
                }
            }

    val state: StateFlow<DebriefUiState> =
        combine(selected, session, debriefs, ungradedWarning) { selected, session, debriefs, ungradedWarning ->
            DebriefUiState(
                selected = selected,
                session = session,
                debrief = debriefs[selected],
                ungradedWarning = ungradedWarning
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), DebriefUiState(crewMember))

    /** Shows the [crewMember]'s Debrief in place, as the CM1 / CM2 toggle does. */
    fun onSelect(crewMember: CrewMember) {
        selected.value = crewMember
    }

    fun onGradeSelect(
        competencyId: Long,
        grade: Grade,
    ) {
        setGrade.set(sessionId, competencyId, selected.value, grade)
    }

    fun onDateChange(date: LocalDate) {
        changeSessionDate.change(sessionId, date)
    }

    /** Leaving by the system back or the back arrow first asks while Competencies have no Grade. */
    fun onBack() {
        val ungraded = state.value.debrief?.ungradedCount ?: 0
        if (ungraded > 0) ungradedWarning.value = ungraded else onLeave()
    }

    fun onStay() {
        ungradedWarning.value = null
    }

    fun onLeave() {
        ungradedWarning.value = null
        mutableEvents.tryEmit(DebriefEvent.Leave)
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5000L
    }
}
