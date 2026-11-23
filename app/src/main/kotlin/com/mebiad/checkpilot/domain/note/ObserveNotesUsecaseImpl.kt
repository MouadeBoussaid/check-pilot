package com.mebiad.checkpilot.domain.note

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn

class ObserveNotesUsecaseImpl(
    private val repository: NoteRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ObserveNotesUsecase {
    override fun notes(sessionId: String): Flow<List<Note>> = repository.observe(sessionId).flowOn(dispatcher)
}
