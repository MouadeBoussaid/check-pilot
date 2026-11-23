package com.mebiad.checkpilot.domain.note

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.util.UUID

class EditNotesUsecaseImpl(
    private val repository: NoteRepository,
    private val newId: () -> String = { UUID.randomUUID().toString() },
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : EditNotesUsecase {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    // One consumer keeps the writes in call order: a single-parallelism dispatcher alone doesn't, because Room's
    // suspending calls resume on other threads and let the next write start.
    private val writes = Channel<suspend () -> Unit>(Channel.UNLIMITED)

    init {
        scope.launch {
            for (write in writes) {
                // A failed write loses that change only; the Assessor keeps typing and the next writes still land.
                try {
                    write()
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                }
            }
        }
    }

    override fun add(
        sessionId: String,
        competencyId: Long,
        text: String,
    ): String {
        val id = newId()
        enqueue {
            val sortIndex = (repository.highestSortIndex(sessionId, competencyId) ?: -1) + 1
            repository.insert(
                Note(
                    id = id,
                    sessionId = sessionId,
                    competencyId = competencyId,
                    text = text,
                    cmTag = CmTag.BOTH,
                    observableBehaviourId = null,
                    sortIndex = sortIndex
                )
            )
        }
        return id
    }

    override fun changeText(
        id: String,
        text: String,
    ) {
        enqueue { repository.updateText(id = id, text = text) }
    }

    override fun delete(id: String) = enqueue { repository.delete(id) }

    override fun restore(note: Note) = enqueue { repository.insert(note) }

    override fun removeEmptyNotes(sessionId: String) = enqueue { repository.deleteEmpty(sessionId) }

    private fun enqueue(write: suspend () -> Unit) {
        writes.trySend(write)
    }
}
