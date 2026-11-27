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

    override suspend fun updateStaffNumber(
        id: String,
        crewMember: CrewMember,
        staffNumber: StaffNumber?,
    ) {
        sessionsState.value =
            sessionsState.value.map {
                when {
                    it.id != id -> it
                    crewMember == CrewMember.CM1 -> it.copy(cm1StaffNumber = staffNumber)
                    else -> it.copy(cm2StaffNumber = staffNumber)
                }
            }
    }

    override fun observe(id: String): Flow<Session?> = sessionsState.map { sessions -> sessions.find { it.id == id } }

    override fun observeLast(): Flow<Session?> =
        sessionsState.map { sessions ->
            val latest = sessions.maxOfOrNull { it.createdAt }
            sessions.lastOrNull { it.createdAt == latest }
        }

    override fun search(filter: SessionFilter): Flow<List<Session>> =
        sessionsState.map { sessions ->
            sessions
                .withIndex()
                .filter { (_, session) -> filter.matches(session) }
                .sortedWith(
                    compareByDescending<IndexedValue<Session>> { it.value.date }
                        .thenByDescending { it.value.createdAt }
                        .thenByDescending { it.index }
                ).map { it.value }
        }

    override suspend fun delete(id: String) {
        sessionsState.value = sessionsState.value.filterNot { it.id == id }
    }

    override suspend fun deleteAll() {
        sessionsState.value = emptyList()
    }

    fun reset() {
        sessionsState.value = emptyList()
    }
}

private fun SessionFilter.matches(session: Session): Boolean {
    val staffNumbers = listOfNotNull(session.cm1StaffNumber?.digits, session.cm2StaffNumber?.digits)
    val airports = listOf(session.departure.letters, session.destination.letters)
    return (staffNumberPrefix == null || staffNumbers.any { it.startsWith(staffNumberPrefix) }) &&
        (airportPrefix == null || airports.any { it.startsWith(airportPrefix) }) &&
        (date == null || session.date == date)
}
