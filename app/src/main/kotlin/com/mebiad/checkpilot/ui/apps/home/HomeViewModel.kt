package com.mebiad.checkpilot.ui.apps.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mebiad.checkpilot.domain.session.ObserveSessionUsecase
import com.mebiad.checkpilot.domain.session.Session
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val lastSession: Session? = null,
)

class HomeViewModel(
    observeSession: ObserveSessionUsecase,
) : ViewModel() {
    val state: StateFlow<HomeUiState> =
        observeSession
            .lastSession()
            .map(::HomeUiState)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HomeUiState())

    private companion object {
        const val STOP_TIMEOUT_MS = 5000L
    }
}
