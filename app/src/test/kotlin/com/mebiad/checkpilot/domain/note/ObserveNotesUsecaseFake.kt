package com.mebiad.checkpilot.domain.note

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class ObserveNotesUsecaseFake : ObserveNotesUsecase {
    val notes = MutableStateFlow<List<Note>>(emptyList())

    override fun notes(sessionId: String): Flow<List<Note>> =
        notes.map { all -> all.filter { it.sessionId == sessionId }.sortedBy { it.sortIndex } }
}
