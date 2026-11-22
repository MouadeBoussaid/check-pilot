package com.mebiad.checkpilot.ui.apps.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mebiad.checkpilot.domain.catalogue.Competency
import com.mebiad.checkpilot.domain.catalogue.ObserveCatalogueUsecase
import com.mebiad.checkpilot.domain.session.ObserveSessionUsecase
import com.mebiad.checkpilot.domain.session.Session
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class CaptureUiState(
    val session: Session? = null,
    val competencies: List<Competency> = emptyList(),
)

class CaptureViewModel(
    sessionId: String,
    observeSession: ObserveSessionUsecase,
    observeCatalogue: ObserveCatalogueUsecase,
) : ViewModel() {
    val state: StateFlow<CaptureUiState> =
        combine(observeSession.session(sessionId), observeCatalogue.competencies(), ::CaptureUiState)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), CaptureUiState())

    private companion object {
        const val STOP_TIMEOUT_MS = 5000L
    }
}
