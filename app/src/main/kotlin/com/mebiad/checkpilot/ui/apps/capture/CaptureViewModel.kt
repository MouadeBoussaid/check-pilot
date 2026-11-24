package com.mebiad.checkpilot.ui.apps.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mebiad.checkpilot.domain.catalogue.Competency
import com.mebiad.checkpilot.domain.catalogue.ObservableBehaviour
import com.mebiad.checkpilot.domain.catalogue.ObserveCatalogueUsecase
import com.mebiad.checkpilot.domain.note.CmTag
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

/** [showCmTags] is true in a Session with two Crew Members, where each Note says which one it is about. */
data class CaptureUiState(
    val session: Session? = null,
    val competencies: List<CompetencyNotes> = emptyList(),
    val showCmTags: Boolean = false,
)

/** [linkedObservableBehaviours] are the OBS BEH its Notes link, each once, in Catalogue order. */
data class CompetencyNotes(
    val competency: Competency,
    val fields: List<NoteField>,
    val linkedObservableBehaviours: List<ObservableBehaviour> = emptyList(),
)

/**
 * One Note field on the capture screen. [key] stays the same while the field exists, also when the empty field below a
 * Competency's Notes becomes a Note as its first character is typed. [isNote] is false only for that empty field.
 */
data class NoteField(
    val key: String,
    val text: String,
    val isNote: Boolean,
    val cmTag: CmTag = CmTag.BOTH,
    val observableBehaviour: ObservableBehaviour? = null,
)

sealed interface CaptureEvent {
    /** [token] answers the undo snackbar of the change. */
    val token: Long

    /** A Note was erased. */
    data class NoteErased(
        override val token: Long,
    ) : CaptureEvent

    /** A Note was moved to another Competency. */
    data class NoteMoved(
        override val token: Long,
    ) : CaptureEvent
}

/**
 * What the capture screen knows before the database does, so a field never waits for a write: Notes typed into the
 * empty field of a Competency but not saved yet ([added], by id, each with its Competency id), and the latest typed
 * text of each Note ([texts]). A Note typed into the empty field keeps that field's key ([keys]); [emptyFieldCounts]
 * gives each Competency's empty field a fresh key after it became a Note. [hidden] holds the ids of erased Notes,
 * [restored] the copies Undo wrote back, by id, and [textsBeforeEmptied] what each Note said before it was emptied.
 * [cmTags] and [observableBehaviours] hold the latest selected CM Tag and OBS BEH id of each Note, null for none.
 * [movedTo] holds the Competency id each moved Note was last moved to.
 */
private data class Overlay(
    val added: Map<String, Long> = emptyMap(),
    val texts: Map<String, String> = emptyMap(),
    val keys: Map<String, String> = emptyMap(),
    val emptyFieldCounts: Map<Long, Int> = emptyMap(),
    val hidden: Set<String> = emptySet(),
    val restored: Map<String, Note> = emptyMap(),
    val textsBeforeEmptied: Map<String, String> = emptyMap(),
    val cmTags: Map<String, CmTag> = emptyMap(),
    val observableBehaviours: Map<String, Long?> = emptyMap(),
    val movedTo: Map<String, Long> = emptyMap(),
) {
    /**
     * Where [note] shows: under the Competency it was last moved to, after that Competency's Notes until the database
     * has the move, or in the place Undo wrote it back to.
     */
    fun placed(note: Note): Note {
        val target = movedTo[note.id]
        if (target != null) {
            return note.copy(
                competencyId = target,
                sortIndex = if (note.competencyId == target) note.sortIndex else Long.MAX_VALUE
            )
        }
        val copy = restored[note.id] ?: return note
        return note.copy(competencyId = copy.competencyId, sortIndex = copy.sortIndex)
    }

    fun competencyIdOfAdded(noteId: String): Long? = movedTo[noteId] ?: added[noteId]

    fun observableBehaviourIdOf(
        noteId: String,
        saved: Long?,
    ): Long? = if (noteId in observableBehaviours) observableBehaviours[noteId] else saved

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
                showCmTags = session?.hasTwoCrewMembers == true,
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

    fun onCmTagSelect(
        key: String,
        cmTag: CmTag,
    ) {
        val current = overlay.value
        val id = current.noteIdOf(key)
        if (textOf(id) == null) return
        editNotes.setCmTag(id = id, cmTag = cmTag)
        overlay.value = current.copy(cmTags = current.cmTags + (id to cmTag))
    }

    /** Links the OBS BEH [observableBehaviourId] to the Note in [key], or unlinks its OBS BEH when null. */
    fun onObservableBehaviourSelect(
        key: String,
        observableBehaviourId: Long?,
    ) {
        val current = overlay.value
        val id = current.noteIdOf(key)
        if (textOf(id) == null) return
        editNotes.setObservableBehaviour(id = id, observableBehaviourId = observableBehaviourId)
        overlay.value =
            current.copy(observableBehaviours = current.observableBehaviours + (id to observableBehaviourId))
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

    /** Moves the Note in [key] to the end of the Competency [competencyId]'s Notes. */
    fun onMoveField(
        key: String,
        competencyId: Long,
    ) {
        val current = overlay.value
        val id = current.noteIdOf(key)
        val text = textOf(id) ?: return
        val from = savedNoteOf(id)?.let { current.placed(it).competencyId } ?: current.competencyIdOfAdded(id)
        if (from == competencyId) return
        val copy = undoCopyOf(id, text)
        editNotes.move(id = id, competencyId = competencyId)
        overlay.value =
            current.copy(
                movedTo = current.movedTo + (id to competencyId),
                restored = current.restored - id,
                observableBehaviours = current.observableBehaviours + (id to null)
            )
        if (copy != null) offerUndo(copy, CaptureEvent::NoteMoved)
    }

    /** Writes back the Note erased or moved with [token], if no newer change came after it. */
    fun undo(token: Long) {
        val (current, copy) = undoable ?: return
        if (current != token) return
        undoable = null
        editNotes.restore(copy)
        overlay.value =
            overlay.value.let {
                it.copy(
                    hidden = it.hidden - copy.id,
                    movedTo = it.movedTo - copy.id,
                    restored = it.restored + (copy.id to copy),
                    cmTags = it.cmTags + (copy.id to copy.cmTag),
                    observableBehaviours = it.observableBehaviours + (copy.id to copy.observableBehaviourId),
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
        val copy = undoCopyOf(id, undoText)
        editNotes.delete(id)
        overlay.value = overlay.value.let { it.copy(hidden = it.hidden + id) }
        if (copy != null) offerUndo(copy, CaptureEvent::NoteErased)
    }

    /** The Note as it shows, with [text], for Undo to write back; null while it has no row to copy. */
    private fun undoCopyOf(
        id: String,
        text: String?,
    ): Note? {
        val current = overlay.value
        val row = savedNoteOf(id) ?: return null
        if (text.isNullOrEmpty()) return null
        return current.placed(row).copy(
            text = text,
            cmTag = current.cmTags[id] ?: row.cmTag,
            observableBehaviourId = current.observableBehaviourIdOf(id, row.observableBehaviourId)
        )
    }

    /** Keeps [copy] for the undo snackbar of a new change, which makes any older change final. */
    private fun offerUndo(
        copy: Note,
        event: (token: Long) -> CaptureEvent,
    ) {
        val token = ++lastToken
        undoable = token to copy
        mutableEvents.tryEmit(event(token))
    }

    private fun Competency.withFields(
        notes: List<Note>,
        overlay: Overlay,
    ): CompetencyNotes {
        val savedIds = notes.map { it.id }.toSet()
        val restored = overlay.restored.values.filter { it.id !in savedIds }
        val saved =
            (notes + restored)
                .map { overlay.placed(it) }
                .filter { it.competencyId == id && it.id !in overlay.hidden }
                .sortedBy { it.sortIndex }
        val unsaved =
            overlay.added
                .keys
                .filter { noteId ->
                    overlay.competencyIdOfAdded(noteId) == id && noteId !in savedIds && noteId !in overlay.hidden
                }
        val noteFields =
            saved.map { note ->
                NoteField(
                    key = overlay.keyOf(note.id),
                    text = overlay.texts[note.id] ?: note.text,
                    isNote = true,
                    cmTag = overlay.cmTags[note.id] ?: note.cmTag,
                    observableBehaviour =
                        observableBehaviourOf(
                            overlay.observableBehaviourIdOf(note.id, note.observableBehaviourId)
                        )
                )
            } +
                unsaved.map { noteId ->
                    NoteField(
                        key = overlay.keyOf(noteId),
                        text = overlay.texts[noteId] ?: "",
                        isNote = true,
                        cmTag = overlay.cmTags[noteId] ?: CmTag.BOTH,
                        observableBehaviour = observableBehaviourOf(overlay.observableBehaviourIdOf(noteId, null))
                    )
                }
        val linked = noteFields.mapNotNull { it.observableBehaviour }.toSet()
        return CompetencyNotes(
            competency = this,
            fields = noteFields + NoteField(key = overlay.emptyFieldKey(id), text = "", isNote = false),
            linkedObservableBehaviours = observableBehaviours.filter { it in linked }
        )
    }

    private fun Competency.observableBehaviourOf(id: Long?): ObservableBehaviour? =
        id?.let { observableBehaviours.find { it.id == id } }

    private companion object {
        const val STOP_TIMEOUT_MS = 5000L
    }
}
