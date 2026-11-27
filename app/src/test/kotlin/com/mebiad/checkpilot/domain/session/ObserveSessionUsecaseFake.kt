package com.mebiad.checkpilot.domain.session

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class ObserveSessionUsecaseFake : ObserveSessionUsecase {
    val sessions = MutableStateFlow<List<Session>>(emptyList())

    val last = MutableStateFlow<Session?>(null)

    override fun session(id: String): Flow<Session?> = sessions.map { all -> all.find { it.id == id } }

    override fun lastSession(): Flow<Session?> = last
}
