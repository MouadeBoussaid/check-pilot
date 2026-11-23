package com.mebiad.checkpilot.ui.apps.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mebiad.checkpilot.domain.catalogue.Competency
import com.mebiad.checkpilot.domain.catalogue.ObserveCatalogueUsecase
import com.mebiad.checkpilot.domain.note.EditNotesUsecase
import com.mebiad.checkpilot.domain.note.Note
import com.mebiad.checkpilot.domain.note.ObserveNotesUsecase
import com.mebiad.checkpilot.domain.session.ObserveSessionUsecase
import com.mebiad.checkpilot.domain.session.Session
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class CaptureUiState(
    val session: Session? = null,
    val competencies: List<CompetencyNotes> = emptyList(),
)

data class CompetencyNotes(
    val competency: Competency,
    val fields: List<NoteField>,
)

/**
 * One Note field on the capture screen. [key] stays the same while the field exists, also when the empty field below a
 * Competency's Notes becomes a Note as its first character is typed. [isNote] is false only for that empty field.
 */
data class NoteField(
    val key: String,
    val text: String,
    val isNote: Boolean,
)

sealed interface CaptureEvent {
    /** A Note was erased; [token] answers its undo snackbar. */
    data class NoteErased(
        val token: Long,
    ) : CaptureEvent
}

/**
 * What the capture screen knows before the database does, so a field never waits for a write: Notes typed into the
 * empty field of a Competency but not saved yet ([added], by id, each with its Competency id), and the latest typed
 * text of each Note ([texts]). A Note typed into the empty field keeps that field's key ([keys]); [emptyFieldCounts]
 * gives each Competency's empty field a fresh key after it became a Note. [hidden] holds the ids of erased Notes,
 * [restored] the copies Undo wrote back, by id, and [textsBeforeEmptied] what each Note said before it was emptied.
 */
private data class Overlay(
    val added: Map<String, Long> = emptyMap(),
    val texts: Map<String, String> = emptyMap(),
    val keys: Map<String, String> = emptyMap(),
    val emptyFieldCounts: Map<Long, Int> = emptyMap(),
    val hidden: Set<String> = emptySet(),
    val restored: Map<String, Note> = emptyMap(),
    val textsBeforeEmptied: Map<String, String> = emptyMap(),
) {
    fun emptyFieldKey(competencyId: Long): String = "new-$competencyId-${emptyFieldCounts[competencyId] ?: 0}"

    fun keyOf(noteId: String): String = keys[noteId] ?: noteId

    fun noteIdOf(key: String): String = keys.entries.firstOrNull { it.value == key }?.key ?: key
}

class CaptureViewModel(
    private val sessionId: String,
    observeSession: ObserveSessionUsecase,
    observeCatalogue: ObserveCatalogueUsecase,
    observeNotes: ObserveNotesUsecase,
    private val editNotes: EditNotesUsecase,
) : ViewModel() {
    private val overlay = MutableStateFlow(Overlay())

    private val savedNotes: StateFlow<List<Note>> =
        observeNotes.notes(sessionId).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val mutableEvents = MutableSharedFlow<CaptureEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<CaptureEvent> = mutableEvents.asSharedFlow()

    private var lastToken = 0L

    /** The copy the current undo snackbar would write back, with its token; a newer erase replaces it. */
    private var undoable: Pair<Long, Note>? = null

    val state: StateFlow<CaptureUiState> =
        combine(
            observeSession.session(sessionId),
            observeCatalogue.competencies(),
            savedNotes,
            overlay
        ) { session, competencies, notes, overlay ->
            CaptureUiState(
                session = session,
                competencies = competencies.map { competency -> competency.withFields(notes, overlay) }
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), CaptureUiState())

    init {
        editNotes.removeEmptyNotes(sessionId)
    }

    fun onFieldTextChange(
        competencyId: Long,
        key: String,
        text: String,
    ) {
        val current = overlay.value
        if (key == current.emptyFieldKey(competencyId)) {
            if (text.isEmpty()) return
            val id = editNotes.add(sessionId = sessionId, competencyId = competencyId, text = text)
            overlay.value =
                current.copy(
                    added = current.added + (id to competencyId),
                    texts = current.texts + (id to text),
                    keys = current.keys + (id to key),
                    emptyFieldCounts =
                        current.emptyFieldCounts + (competencyId to (current.emptyFieldCounts[competencyId] ?: 0) + 1)
                )
            return
        }
        val id = current.noteIdOf(key)
        val before = textOf(id) ?: return
        editNotes.changeText(id = id, text = text)
        overlay.value =
            current.copy(
                texts = current.texts + (id to text),
                textsBeforeEmptied =
                    if (text.isEmpty() && before.isNotEmpty()) {
                        current.textsBeforeEmptied + (id to before)
                    } else {
                        current.textsBeforeEmptied
                    }
            )
    }

    /** Erases the Note in [key] if it was left empty. */
    fun onFieldFocusLost(key: String) {
        val id = overlay.value.noteIdOf(key)
        if (textOf(id)?.isEmpty() == true) erase(id, undoText = overlay.value.textsBeforeEmptied[id])
    }

    /** Erases the Note in [key], as its ✕ does. */
    fun onDeleteField(key: String) {
        val current = overlay.value
        val id = current.noteIdOf(key)
        val text = textOf(id) ?: return
        erase(id, undoText = text.ifEmpty { current.textsBeforeEmptied[id] })
    }

    /** Writes back the Note erased with [token], if no newer erase came after it. */
    fun undo(token: Long) {
        val (current, copy) = undoable ?: return
        if (current != token) return
        undoable = null
        editNotes.restore(copy)
        overlay.value =
            overlay.value.let {
                it.copy(
                    hidden = it.hidden - copy.id,
                    restored = it.restored + (copy.id to copy),
                    texts = it.texts + (copy.id to copy.text),
                    textsBeforeEmptied = it.textsBeforeEmptied - copy.id
                )
            }
    }

    private fun textOf(id: String): String? {
        val current = overlay.value
        if (id in current.hidden) return null
        return current.texts[id] ?: savedNoteOf(id)?.text
    }

    private fun savedNoteOf(id: String): Note? = savedNotes.value.find { it.id == id } ?: overlay.value.restored[id]

    /**
     * Deletes the Note and offers to undo it with [undoText]. A Note whose first write hasn't reached the database yet
     * has no row to copy, so it goes without an undo snackbar.
     */
    private fun erase(
        id: String,
        undoText: String?,
    ) {
        val copy = savedNoteOf(id)?.let { row -> undoText?.takeIf { it.isNotEmpty() }?.let { row.copy(text = it) } }
        editNotes.delete(id)
        overlay.value = overlay.value.let { it.copy(hidden = it.hidden + id) }
        if (copy == null) return
        val token = ++lastToken
        undoable = token to copy
        mutableEvents.tryEmit(CaptureEvent.NoteErased(token))
    }

    private fun Competency.withFields(
        notes: List<Note>,
        overlay: Overlay,
    ): CompetencyNotes {
        val savedIds = notes.map { it.id }.toSet()
        val restored = overlay.restored.values.filter { it.id !in savedIds }
        val saved =
            (notes + restored)
                .filter { it.competencyId == id && it.id !in overlay.hidden }
                .sortedBy { it.sortIndex }
                .map { it.id to it.text }
        val unsaved =
            overlay.added
                .filter { (noteId, competencyId) ->
                    competencyId == id && noteId !in savedIds &&
                        noteId !in overlay.hidden
                }.keys
        val noteFields =
            (saved + unsaved.map { it to "" }).map { (noteId, text) ->
                NoteField(key = overlay.keyOf(noteId), text = overlay.texts[noteId] ?: text, isNote = true)
            }
        return CompetencyNotes(
            competency = this,
            fields = noteFields + NoteField(key = overlay.emptyFieldKey(id), text = "", isNote = false)
        )
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5000L
    }
}
