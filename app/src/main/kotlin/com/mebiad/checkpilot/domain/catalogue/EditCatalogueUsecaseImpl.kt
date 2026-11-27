package com.mebiad.checkpilot.domain.catalogue

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

class EditCatalogueUsecaseImpl(
    private val repository: CatalogueRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : EditCatalogueUsecase {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    // One consumer keeps the edits in call order, so a move reads the order the previous move left.
    private val edits = Channel<suspend () -> Unit>(Channel.UNLIMITED)

    init {
        scope.launch {
            for (edit in edits) {
                // A failed edit is lost on its own; the editor keeps showing the Catalogue as it is.
                try {
                    edit()
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                }
            }
        }
    }

    override fun usage(): Flow<CatalogueUsage> = repository.observeUsage().flowOn(dispatcher)

    override fun addCompetency(name: String) = withName(name) { repository.addCompetency(it) }

    override fun renameCompetency(
        id: Long,
        name: String,
    ) = withName(name) { repository.renameCompetency(id = id, name = it) }

    override fun addObservableBehaviour(
        competencyId: Long,
        name: String,
    ) = withName(name) { repository.addObservableBehaviour(competencyId = competencyId, name = it) }

    override fun renameObservableBehaviour(
        id: Long,
        name: String,
    ) = withName(name) { repository.renameObservableBehaviour(id = id, name = it) }

    override fun moveCompetency(
        id: Long,
        index: Int,
    ) = enqueue {
        val ids = repository.observeCompetencies().first().map { it.id }
        if (id in ids) repository.reorderCompetencies(ids.moved(id, index))
    }

    override fun moveObservableBehaviour(
        id: Long,
        index: Int,
    ) = enqueue {
        val ids =
            repository
                .observeCompetencies()
                .first()
                .map { competency -> competency.observableBehaviours.map { it.id } }
                .firstOrNull { id in it } ?: return@enqueue
        repository.reorderObservableBehaviours(ids.moved(id, index))
    }

    // The database's RESTRICT keys refuse it too; checking first keeps a refused delete from reaching them.
    override fun deleteCompetency(id: Long) =
        enqueue {
            if (repository.observeUsage().first().sessionsUsingCompetency(id) == 0) repository.deleteCompetency(id)
        }

    override fun deleteObservableBehaviour(id: Long) =
        enqueue {
            if (repository.observeUsage().first().sessionsUsingObservableBehaviour(id) == 0) {
                repository.deleteObservableBehaviour(id)
            }
        }

    private fun withName(
        name: String,
        edit: suspend (String) -> Unit,
    ) {
        val trimmed = name.trim()
        if (trimmed.isNotEmpty()) enqueue { edit(trimmed) }
    }

    private fun enqueue(edit: suspend () -> Unit) {
        edits.trySend(edit)
    }
}

private fun List<Long>.moved(
    id: Long,
    index: Int,
): List<Long> {
    val others = this - id
    return others.toMutableList().apply { add(index.coerceIn(0, others.size), id) }
}
