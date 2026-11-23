package com.mebiad.checkpilot.data.local.db

import com.mebiad.checkpilot.data.local.db.dao.NoteDao
import com.mebiad.checkpilot.data.local.db.entity.NoteEntity
import com.mebiad.checkpilot.domain.note.CmTag
import com.mebiad.checkpilot.domain.note.Note
import com.mebiad.checkpilot.domain.note.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NoteRepositoryImpl(
    private val dao: NoteDao,
) : NoteRepository {
    override fun observe(sessionId: String): Flow<List<Note>> =
        dao.observe(sessionId).map { notes -> notes.map { it.toNote() } }

    override suspend fun insert(note: Note) = dao.insert(note.toEntity())

    override suspend fun updateText(
        id: String,
        text: String,
    ) = dao.updateText(id = id, text = text)

    override suspend fun delete(id: String) = dao.delete(id)

    override suspend fun deleteEmpty(sessionId: String) = dao.deleteEmpty(sessionId)

    override suspend fun highestSortIndex(
        sessionId: String,
        competencyId: Long,
    ): Long? = dao.highestSortIndex(sessionId = sessionId, competencyId = competencyId)
}

private fun Note.toEntity(): NoteEntity =
    NoteEntity(
        id = id,
        sessionId = sessionId,
        competencyId = competencyId,
        text = text,
        cmTag = cmTag.name,
        observableBehaviourId = observableBehaviourId,
        sortIndex = sortIndex
    )

private fun NoteEntity.toNote(): Note =
    Note(
        id = id,
        sessionId = sessionId,
        competencyId = competencyId,
        text = text,
        cmTag = CmTag.valueOf(cmTag),
        observableBehaviourId = observableBehaviourId,
        sortIndex = sortIndex
    )
