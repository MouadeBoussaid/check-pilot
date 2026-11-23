package com.mebiad.checkpilot.domain.note

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class NoteRepositoryFake : NoteRepository {
    private val notesState = MutableStateFlow<List<Note>>(emptyList())

    val notes: List<Note>
        get() = notesState.value

    /** Makes each insert suspend this long first, as a slow database write would. */
    var insertDelayMs = 0L

    /** Makes inserting a Note with one of these ids throw, as a broken database write would. */
    var failingIds = emptySet<String>()

    override fun observe(sessionId: String): Flow<List<Note>> =
        notesState.map { notes -> notes.filter { it.sessionId == sessionId }.sortedBy { it.sortIndex } }

    override suspend fun find(id: String): Note? = notes.singleOrNull { it.id == id }

    override suspend fun insert(note: Note) {
        delay(insertDelayMs)
        check(note.id !in failingIds) { "Insert of ${note.id} failed" }
        notesState.value = notesState.value.filterNot { it.id == note.id } + note
    }

    override suspend fun updateText(
        id: String,
        text: String,
    ) {
        notesState.value = notesState.value.map { if (it.id == id) it.copy(text = text) else it }
    }

    override suspend fun updateCmTag(
        id: String,
        cmTag: CmTag,
    ) {
        notesState.value = notesState.value.map { if (it.id == id) it.copy(cmTag = cmTag) else it }
    }

    override suspend fun updateObservableBehaviour(
        id: String,
        observableBehaviourId: Long?,
    ) {
        notesState.value =
            notesState.value.map { if (it.id == id) it.copy(observableBehaviourId = observableBehaviourId) else it }
    }

    override suspend fun delete(id: String) {
        notesState.value = notesState.value.filterNot { it.id == id }
    }

    override suspend fun deleteEmpty(sessionId: String) {
        notesState.value = notesState.value.filterNot { it.sessionId == sessionId && it.text.isEmpty() }
    }

    override suspend fun highestSortIndex(
        sessionId: String,
        competencyId: Long,
    ): Long? =
        notes.filter { it.sessionId == sessionId && it.competencyId == competencyId }.maxOfOrNull { it.sortIndex }

    fun reset() {
        notesState.value = emptyList()
        insertDelayMs = 0L
        failingIds = emptySet()
    }
}
