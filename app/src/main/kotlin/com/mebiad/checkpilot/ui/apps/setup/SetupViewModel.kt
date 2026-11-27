package com.mebiad.checkpilot.ui.apps.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mebiad.checkpilot.core.clock.Clock
import com.mebiad.checkpilot.domain.session.SessionSetup
import com.mebiad.checkpilot.domain.session.SetupRefusal
import com.mebiad.checkpilot.domain.session.StartSessionResult
import com.mebiad.checkpilot.domain.session.StartSessionUsecase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class SetupUiState(
    val cm1StaffNumber: String = "",
    val cm2StaffNumber: String = "",
    val departure: String = "",
    val destination: String = "",
    val date: LocalDate,
    val refusal: SetupRefusal? = null,
)

sealed interface SetupEvent {
    data class SessionStarted(
        val sessionId: String,
    ) : SetupEvent
}

private fun SetupUiState.toSetup() =
    SessionSetup(
        cm1StaffNumber = cm1StaffNumber,
        cm2StaffNumber = cm2StaffNumber,
        departure = departure,
        destination = destination,
        date = date
    )

class SetupViewModel(
    private val startSession: StartSessionUsecase,
    clock: Clock,
) : ViewModel() {
    private val mutableState = MutableStateFlow(SetupUiState(date = clock.today()))
    val state: StateFlow<SetupUiState> = mutableState.asStateFlow()
    private val mutableEvents = MutableSharedFlow<SetupEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<SetupEvent> = mutableEvents.asSharedFlow()

    fun onCm1StaffNumberChange(text: String) = edit { copy(cm1StaffNumber = text) }

    fun onCm2StaffNumberChange(text: String) = edit { copy(cm2StaffNumber = text) }

    fun onDepartureChange(text: String) = edit { copy(departure = text) }

    fun onDestinationChange(text: String) = edit { copy(destination = text) }

    fun onDateChange(date: LocalDate) = edit { copy(date = date) }

    private var starting: Job? = null

    fun onStart() {
        if (starting?.isActive == true) return
        starting =
            viewModelScope.launch {
                when (val result = startSession.start(state.value.toSetup())) {
                    is StartSessionResult.Started -> mutableEvents.emit(SetupEvent.SessionStarted(result.sessionId))
                    is StartSessionResult.Refused -> mutableState.update { it.copy(refusal = result.reason) }
                }
            }
    }

    private fun edit(change: SetupUiState.() -> SetupUiState) = mutableState.update { it.change().copy(refusal = null) }
}
