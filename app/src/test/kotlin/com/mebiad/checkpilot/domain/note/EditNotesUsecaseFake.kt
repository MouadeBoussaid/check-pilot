package com.mebiad.checkpilot.domain.note

/** Records each call in [calls] and writes nothing; tests emit the saved Notes through [ObserveNotesUsecaseFake]. */
class EditNotesUsecaseFake : EditNotesUsecase {
    sealed interface Call {
        data class Add(
            val sessionId: String,
            val competencyId: Long,
            val text: String,
            val id: String,
        ) : Call

        data class ChangeText(
            val id: String,
            val text: String,
        ) : Call

        data class SetCmTag(
            val id: String,
            val cmTag: CmTag,
        ) : Call

        data class SetObservableBehaviour(
            val id: String,
            val observableBehaviourId: Long?,
        ) : Call

        data class Delete(
            val id: String,
        ) : Call

        data class Restore(
            val note: Note,
        ) : Call

        data class RemoveEmptyNotes(
            val sessionId: String,
        ) : Call
    }

    val calls = mutableListOf<Call>()

    private var nextId = 1

    override fun add(
        sessionId: String,
        competencyId: Long,
        text: String,
    ): String {
        val id = "added-${nextId++}"
        calls += Call.Add(sessionId = sessionId, competencyId = competencyId, text = text, id = id)
        return id
    }

    override fun changeText(
        id: String,
        text: String,
    ) {
        calls += Call.ChangeText(id = id, text = text)
    }

    override fun setCmTag(
        id: String,
        cmTag: CmTag,
    ) {
        calls += Call.SetCmTag(id = id, cmTag = cmTag)
    }

    override fun setObservableBehaviour(
        id: String,
        observableBehaviourId: Long?,
    ) {
        calls += Call.SetObservableBehaviour(id = id, observableBehaviourId = observableBehaviourId)
    }

    override fun delete(id: String) {
        calls += Call.Delete(id)
    }

    override fun restore(note: Note) {
        calls += Call.Restore(note)
    }

    override fun removeEmptyNotes(sessionId: String) {
        calls += Call.RemoveEmptyNotes(sessionId)
    }

    fun reset() {
        calls.clear()
        nextId = 1
    }
}
