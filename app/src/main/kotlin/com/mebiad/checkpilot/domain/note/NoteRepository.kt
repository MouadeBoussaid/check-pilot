package com.mebiad.checkpilot.domain.note

import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    /** Emits the Session's Notes, each Competency's in [Note.sortIndex] order. */
    fun observe(sessionId: String): Flow<List<Note>>

    suspend fun find(id: String): Note?

    /** Inserts [note], or replaces the Note with its id, as Undo does. */
    suspend fun insert(note: Note)

    suspend fun updateText(
        id: String,
        text: String,
    )

    suspend fun updateCmTag(
        id: String,
        cmTag: CmTag,
    )

    /** Links the OBS BEH [observableBehaviourId] to the Note, or unlinks its OBS BEH when null. */
    suspend fun updateObservableBehaviour(
        id: String,
        observableBehaviourId: Long?,
    )

    suspend fun delete(id: String)

    /** Deletes the Session's Notes whose text is empty. */
    suspend fun deleteEmpty(sessionId: String)

    /** The highest [Note.sortIndex] among the Competency's Notes in the Session, or null while it has none. */
    suspend fun highestSortIndex(
        sessionId: String,
        competencyId: Long,
    ): Long?
}
