package com.mebiad.checkpilot.domain.note

/** A Note filed under [competencyId] in a Session; [sortIndex] orders the Notes of one Competency. */
data class Note(
    val id: String,
    val sessionId: String,
    val competencyId: Long,
    val text: String,
    val cmTag: CmTag,
    val observableBehaviourId: Long?,
    val sortIndex: Long,
)

/** Which Crew Member a Note is about. The names are the codes stored in the database, so don't rename them. */
enum class CmTag {
    CM1,
    CM2,
    BOTH,
}
