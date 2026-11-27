package com.mebiad.checkpilot.ui.apps.sessions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mebiad.checkpilot.core.clock.Clock
import com.mebiad.checkpilot.domain.pin.PinUsecase
import com.mebiad.checkpilot.domain.session.DeleteSessionUsecase
import com.mebiad.checkpilot.domain.session.DuplicateSessionUsecase
import com.mebiad.checkpilot.domain.session.SearchSessionsUsecase
import com.mebiad.checkpilot.domain.session.SessionSummary
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class SessionsUiState(
    val unlocked: Boolean = false,
    val query: String = "",
    val date: LocalDate? = null,
    /** Where the date picker opens while no date is picked. */
    val today: LocalDate,
    val sessions: List<SessionSummary> = emptyList(),
    /** The Session whose Delete waits for a confirmation. */
    val pendingDelete: SessionSummary? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class SessionsViewModel(
    private val search: SearchSessionsUsecase,
    private val delete: DeleteSessionUsecase,
    private val duplicate: DuplicateSessionUsecase,
    private val pin: PinUsecase,
    private val clock: Clock,
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val date = MutableStateFlow<LocalDate?>(null)
    private val pendingDelete = MutableStateFlow<SessionSummary?>(null)

    private val sessions: Flow<List<SessionSummary>> =
        combine(pin.unlocked, query, date, ::Triple)
            .flatMapLatest { (unlocked, query, date) ->
                if (unlocked) search.search(query, date) else flowOf(emptyList())
            }

    val state: StateFlow<SessionsUiState> =
        combine(pin.unlocked, query, date, sessions, pendingDelete) { unlocked, query, date, sessions, pendingDelete ->
            SessionsUiState(
                unlocked = unlocked,
                query = query,
                date = date,
                today = clock.today(),
                sessions = sessions,
                pendingDelete = pendingDelete
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            SessionsUiState(today = clock.today())
        )

    fun onQueryChange(text: String) {
        query.value = text
    }

    fun onDatePick(picked: LocalDate) {
        date.value = picked
    }

    fun onDateClear() {
        date.value = null
    }

    fun onDeleteAsk(summary: SessionSummary) {
        pendingDelete.value = summary
    }

    fun onDeleteConfirm() {
        val summary = pendingDelete.value ?: return
        pendingDelete.value = null
        delete.delete(summary.session.id)
    }

    fun onDeleteCancel() {
        pendingDelete.value = null
    }

    fun onDuplicate(summary: SessionSummary) {
        duplicate.duplicate(summary.session.id)
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5000L
    }
}
