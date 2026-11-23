package com.mebiad.checkpilot.domain.note

/** Every call returns at once; the writes land in the database in the order they were called. */
interface EditNotesUsecase {
    /** Saves a new Note, tagged BOTH, after the Competency's last one, and returns its id. */
    fun add(
        sessionId: String,
        competencyId: Long,
        text: String,
    ): String

    fun changeText(
        id: String,
        text: String,
    )

    fun setCmTag(
        id: String,
        cmTag: CmTag,
    )

    /**
     * Links the OBS BEH [observableBehaviourId] to the Note, or unlinks its OBS BEH when null. An OBS BEH of another
     * Competency than the Note's is ignored.
     */
    fun setObservableBehaviour(
        id: String,
        observableBehaviourId: Long?,
    )

    fun delete(id: String)

    /** Writes back a Note as it was, with its id, CM Tag, OBS BEH, Competency and place, as Undo does. */
    fun restore(note: Note)

    /** Deletes the Session's empty Notes, left over when the app was killed before an emptied Note lost focus. */
    fun removeEmptyNotes(sessionId: String)
}
