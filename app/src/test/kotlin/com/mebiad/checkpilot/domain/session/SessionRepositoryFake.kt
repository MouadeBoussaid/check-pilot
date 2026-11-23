package com.mebiad.checkpilot.domain.session

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class SessionRepositoryFake : SessionRepository {
    private val sessionsState = MutableStateFlow<List<Session>>(emptyList())

    val sessions: List<Session>
        get() = sessionsState.value

    override suspend fun insert(session: Session) {
        sessionsState.value += session
    }

    override suspend fun updateDate(
        id: String,
        date: LocalDate,
    ) {
        sessionsState.value = sessionsState.value.map { if (it.id == id) it.copy(date = date) else it }
    }

    override fun observe(id: String): Flow<Session?> = sessionsState.map { sessions -> sessions.find { it.id == id } }

    override fun observeLast(): Flow<Session?> =
        sessionsState.map { sessions ->
            val latest = sessions.maxOfOrNull { it.createdAt }
            sessions.lastOrNull { it.createdAt == latest }
        }

    fun reset() {
        sessionsState.value = emptyList()
    }
}
