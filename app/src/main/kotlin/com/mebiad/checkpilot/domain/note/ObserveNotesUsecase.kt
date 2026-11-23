package com.mebiad.checkpilot.domain.note

import kotlinx.coroutines.flow.Flow

interface ObserveNotesUsecase {
    /** Emits the Session's Notes, each Competency's in [Note.sortIndex] order, again whenever they change. */
    fun notes(sessionId: String): Flow<List<Note>>
}
