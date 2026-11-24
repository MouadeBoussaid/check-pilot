package com.mebiad.checkpilot.ui.apps.capture

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mebiad.checkpilot.R
import com.mebiad.checkpilot.domain.catalogue.Competency
import com.mebiad.checkpilot.domain.catalogue.ObservableBehaviour
import com.mebiad.checkpilot.domain.note.CmTag
import com.mebiad.checkpilot.domain.session.CrewMember
import com.mebiad.checkpilot.domain.session.Session
import com.mebiad.checkpilot.domain.session.StaffNumber
import com.mebiad.checkpilot.ui.generic.components.CmTagBadge
import com.mebiad.checkpilot.ui.generic.components.CrewMemberDot
import com.mebiad.checkpilot.ui.generic.components.CrewMemberSlot
import com.mebiad.checkpilot.ui.generic.components.PrimaryButton
import com.mebiad.checkpilot.ui.generic.components.RouteText
import com.mebiad.checkpilot.ui.generic.components.SecondaryButton
import com.mebiad.checkpilot.ui.generic.components.UndoSnackbarHost
import com.mebiad.checkpilot.ui.generic.components.color
import com.mebiad.checkpilot.ui.generic.components.label
import com.mebiad.checkpilot.ui.generic.components.showUndo
import com.mebiad.checkpilot.ui.generic.components.slot
import com.mebiad.checkpilot.ui.generic.format.toDisplayText
import com.mebiad.checkpilot.ui.generic.theme.CheckPilotTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun CaptureScreen(
    viewModel: CaptureViewModel,
    onGenerateDebrief: (CrewMember) -> Unit,
    onDone: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val noteErased = stringResource(R.string.capture_note_erased)
    val noteMoved = stringResource(R.string.capture_note_moved)
    val undo = stringResource(R.string.undo)
    LaunchedEffect(viewModel) {
        // collectLatest: a newer change replaces the snackbar showing, which makes the older change final.
        viewModel.events.collectLatest { event ->
            val message =
                when (event) {
                    is CaptureEvent.NoteErased -> noteErased
                    is CaptureEvent.NoteMoved -> noteMoved
                }
            if (snackbarHostState.showUndo(message = message, undoLabel = undo)) viewModel.undo(event.token)
        }
    }
    CaptureContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onFieldTextChange = viewModel::onFieldTextChange,
        onFieldFocusLost = viewModel::onFieldFocusLost,
        onDeleteField = viewModel::onDeleteField,
        onCmTagSelect = viewModel::onCmTagSelect,
        onObservableBehaviourSelect = viewModel::onObservableBehaviourSelect,
        onMoveField = viewModel::onMoveField,
        onGenerateDebrief = onGenerateDebrief,
        onDone = onDone
    )
}

@Composable
private fun CaptureContent(
    state: CaptureUiState,
    snackbarHostState: SnackbarHostState,
    onFieldTextChange: (competencyId: Long, key: String, text: String) -> Unit,
    onFieldFocusLost: (key: String) -> Unit,
    onDeleteField: (key: String) -> Unit,
    onCmTagSelect: (key: String, cmTag: CmTag) -> Unit,
    onObservableBehaviourSelect: (key: String, observableBehaviourId: Long?) -> Unit,
    onMoveField: (key: String, competencyId: Long) -> Unit,
    onGenerateDebrief: (CrewMember) -> Unit,
    onDone: () -> Unit,
) {
    var pickingCrewMember by remember { mutableStateOf(false) }
    var movingKey by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current
    Scaffold(snackbarHost = { UndoSnackbarHost(hostState = snackbarHostState) }) { innerPadding ->
        val session = state.session ?: return@Scaffold
        // A tap on anything that doesn't take it itself ends the editing of the focused Note.
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } }
        ) {
            RouteHeader(session = session)
            // A plain scrolling Column, not a lazy list: a Note scrolled off screen must keep its focus.
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                StaffNumberChips(session = session)
                state.competencies.forEachIndexed { index, competencyNotes ->
                    val competencyId = competencyNotes.competency.id
                    CompetencyHeader(
                        number = index + 1,
                        competency = competencyNotes.competency,
                        linkedObservableBehaviours = competencyNotes.linkedObservableBehaviours
                    )
                    competencyNotes.fields.forEach { field ->
                        key(field.key) {
                            NoteFieldCard(
                                field = field,
                                testTag = "note-field-$competencyId",
                                showCmTags = state.showCmTags,
                                observableBehaviours = competencyNotes.competency.observableBehaviours,
                                onTextChange = { text -> onFieldTextChange(competencyId, field.key, text) },
                                onFocusLost = { onFieldFocusLost(field.key) },
                                onDelete = { onDeleteField(field.key) },
                                onCmTagSelect = { cmTag -> onCmTagSelect(field.key, cmTag) },
                                onObservableBehaviourSelect = { id -> onObservableBehaviourSelect(field.key, id) },
                                onMove = { movingKey = field.key }
                            )
                        }
                    }
                }
                PrimaryButton(
                    text = stringResource(R.string.capture_generate_debrief),
                    onClick = {
                        if (session.hasTwoCrewMembers) {
                            pickingCrewMember = true
                        } else {
                            onGenerateDebrief(session.crewMembers.single())
                        }
                    },
                    icon = painterResource(R.drawable.ic_debrief),
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 32.dp)
                )
                SecondaryButton(
                    text = stringResource(R.string.capture_done),
                    onClick = onDone,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp)
                )
            }
        }
        if (pickingCrewMember) {
            PickCrewMemberDialog(
                session = session,
                onPick = { crewMember ->
                    pickingCrewMember = false
                    onGenerateDebrief(crewMember)
                },
                onDismiss = { pickingCrewMember = false }
            )
        }
        movingKey?.let { key ->
            val moving =
                state.competencies.firstNotNullOfOrNull { competencyNotes ->
                    competencyNotes.fields
                        .find { it.key == key && it.isNote }
                        ?.let { field -> field to competencyNotes.competency }
                }
            if (moving == null) {
                LaunchedEffect(key) { movingKey = null }
            } else {
                MoveNoteSheet(
                    field = moving.first,
                    from = moving.second,
                    competencies = state.competencies.map { it.competency },
                    onPick = { competency ->
                        movingKey = null
                        onMoveField(key, competency.id)
                    },
                    onClose = { movingKey = null }
                )
            }
        }
    }
}

/** Asks which Competency to move a Note to, listing every Competency with the Note's own one marked. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoveNoteSheet(
    field: NoteField,
    from: Competency,
    competencies: List<Competency>,
    onPick: (Competency) -> Unit,
    onClose: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, bottom = 24.dp)
        ) {
            Row(modifier = Modifier.padding(start = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.capture_move_note_to),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { scope.launch { sheetState.hide() }.invokeOnCompletion { onClose() } }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(R.string.close)
                    )
                }
            }
            MovingNoteCard(field = field, modifier = Modifier.padding(top = 16.dp, bottom = 16.dp))
            competencies.forEachIndexed { index, competency ->
                MoveTarget(
                    number = index + 1,
                    competency = competency,
                    isCurrent = competency.id == from.id,
                    onClick = { onPick(competency) }
                )
            }
        }
    }
}

@Composable
private fun MovingNoteCard(
    field: NoteField,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.medium)
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.capture_move_note_quote, field.text),
            style = MaterialTheme.typography.bodyLarge
        )
        field.observableBehaviour?.let { observableBehaviour ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                Icon(
                    painter = painterResource(R.drawable.ic_info),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(R.string.capture_move_note_link_removed, observableBehaviour.name),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** A Competency in the Move Note sheet: tapping it moves the Note there, except the one the Note is in now. */
@Composable
private fun MoveTarget(
    number: Int,
    competency: Competency,
    isCurrent: Boolean,
    onClick: () -> Unit,
) {
    val shape = MaterialTheme.shapes.medium
    val modifier =
        if (isCurrent) {
            Modifier.background(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = shape)
        } else {
            Modifier.clip(shape).clickable(onClick = onClick)
        }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .then(modifier)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CompetencyNumber(number = number)
        Text(
            text = competency.name,
            style = MaterialTheme.typography.titleMedium,
            color = if (isCurrent) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (isCurrent) {
            Text(
                text = stringResource(R.string.capture_move_note_now_here),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Asks which Crew Member's Debrief to generate, in a Session with two. */
@Composable
private fun PickCrewMemberDialog(
    session: Session,
    onPick: (CrewMember) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainer) {
            Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 12.dp)) {
                Text(
                    text = stringResource(R.string.capture_generate_debrief),
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = stringResource(R.string.capture_which_crew_member),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
                )
                session.crewMembers.forEach { crewMember ->
                    session.staffNumberOf(crewMember)?.let { staffNumber ->
                        CrewMemberChoice(
                            crewMember = crewMember,
                            staffNumber = staffNumber,
                            onClick = { onPick(crewMember) },
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End).padding(top = 8.dp)) {
                    Text(text = stringResource(R.string.cancel), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun CrewMemberChoice(
    crewMember: CrewMember,
    staffNumber: StaffNumber,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val slot = crewMember.slot
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = 72.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(width = 1.dp, color = slot.color)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CrewMemberDot(slot = slot, modifier = Modifier.size(16.dp))
            Text(
                text = stringResource(R.string.capture_crew_member_debrief, stringResource(slot.label)),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = staffNumber.digits,
                style = CheckPilotTheme.monoTypography.extraSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RouteHeader(session: Session) {
    Column(modifier = Modifier.statusBarsPadding().padding(top = 16.dp)) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            RouteText(
                departure = session.departure,
                destination = session.destination,
                style = CheckPilotTheme.monoTypography.medium
            )
            Text(
                text = session.date.toDisplayText(),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun StaffNumberChips(session: Session) {
    Column {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            session.cm1StaffNumber?.let { StaffNumberChip(slot = CrewMemberSlot.CM1, staffNumber = it) }
            session.cm2StaffNumber?.let { StaffNumberChip(slot = CrewMemberSlot.CM2, staffNumber = it) }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

/**
 * A Note, or the empty field below a Competency's Notes that becomes one as its first character is typed. The field
 * keeps its own text while the Assessor types, so it never waits for the ViewModel or the database.
 *
 * While the Note is active it shows its editing controls: the CM Tag selector in a Session with two Crew Members, and
 * the OBS BEH dropdown and the move button once the Note has text. Otherwise it shows its OBS BEH in small italic under the text.
 */
@Composable
private fun NoteFieldCard(
    field: NoteField,
    testTag: String,
    showCmTags: Boolean,
    observableBehaviours: List<ObservableBehaviour>,
    onTextChange: (String) -> Unit,
    onFocusLost: () -> Unit,
    onDelete: () -> Unit,
    onCmTagSelect: (CmTag) -> Unit,
    onObservableBehaviourSelect: (Long?) -> Unit,
    onMove: () -> Unit,
) {
    var value by remember { mutableStateOf(TextFieldValue(field.text, TextRange(field.text.length))) }
    var focused by remember { mutableStateOf(false) }
    // The dropdown's popup takes the focus from the text field, so the Note stays active while the menu is open.
    var menuOpen by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val active = focused || menuOpen
    val shape = MaterialTheme.shapes.small
    val border =
        when {
            !field.isNote -> Modifier.dashedBorder(color = MaterialTheme.colorScheme.outline, shape = shape)
            active -> Modifier.border(width = 2.dp, color = MaterialTheme.colorScheme.primary, shape = shape)
            else -> Modifier
        }
    val background =
        if (field.isNote) Modifier.background(MaterialTheme.colorScheme.surfaceContainerLow, shape) else Modifier
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .then(background)
                .then(border)
                .heightIn(min = 56.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            BasicTextField(
                value = value,
                onValueChange = { new ->
                    val changed = new.text != value.text
                    value = new
                    if (changed) onTextChange(new.text)
                },
                modifier =
                    Modifier
                        .weight(1f)
                        .testTag(testTag)
                        .focusRequester(focusRequester)
                        .onFocusChanged { focusState ->
                            if (focused && !focusState.isFocused) onFocusLost()
                            focused = focusState.isFocused
                        }.padding(16.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                decorationBox = { innerTextField ->
                    Box {
                        if (value.text.isEmpty() && !field.isNote) {
                            Text(
                                text = stringResource(R.string.capture_add_note),
                                style = MaterialTheme.typography.bodyLarge,
                                color = CheckPilotTheme.colors.hint
                            )
                        }
                        innerTextField()
                    }
                }
            )
            when {
                active && field.isNote -> {
                    IconButton(onClick = onDelete, modifier = Modifier.padding(top = 4.dp, end = 4.dp)) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = stringResource(R.string.capture_delete_note),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                field.isNote && showCmTags && field.cmTag != CmTag.BOTH -> {
                    CmTagBadge(cmTag = field.cmTag, modifier = Modifier.padding(top = 14.dp, end = 16.dp))
                }
            }
        }
        if (field.isNote && !active) {
            field.observableBehaviour?.let { observableBehaviour ->
                ObservableBehaviourLabel(
                    names = listOf(observableBehaviour.name),
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                )
            }
        }
        if (field.isNote && active) {
            if (showCmTags) {
                CmTagSelector(
                    selected = field.cmTag,
                    onSelect = onCmTagSelect,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                )
            }
            if (value.text.isNotEmpty()) {
                Row(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ObservableBehaviourDropdown(
                        selected = field.observableBehaviour,
                        choices = observableBehaviours,
                        expanded = menuOpen,
                        onExpandedChange = { open ->
                            menuOpen = open
                            if (!open) focusRequester.requestFocus()
                        },
                        onSelect = onObservableBehaviourSelect,
                        modifier = Modifier.weight(1f)
                    )
                    MoveNoteButton(onClick = onMove)
                }
            }
        }
    }
}

@Composable
private fun MoveNoteButton(onClick: () -> Unit) {
    val shape = MaterialTheme.shapes.medium
    IconButton(
        onClick = onClick,
        modifier =
            Modifier
                .size(48.dp)
                .border(width = 1.dp, color = MaterialTheme.colorScheme.outline, shape = shape)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_swap_vert),
            contentDescription = stringResource(R.string.capture_move_note),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CmTagSelector(
    selected: CmTag,
    onSelect: (CmTag) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CmTag.entries.forEach { cmTag ->
            CmTagOption(
                cmTag = cmTag,
                selected = cmTag == selected,
                onClick = { onSelect(cmTag) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CmTagOption(
    cmTag: CmTag,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.shapes.medium
    val colors =
        if (selected) {
            Modifier.background(color = MaterialTheme.colorScheme.primary, shape = shape)
        } else {
            Modifier.border(width = 1.dp, color = MaterialTheme.colorScheme.outline, shape = shape)
        }
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Row(
        modifier =
            modifier
                .heightIn(min = 48.dp)
                .clip(shape)
                .then(colors)
                .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
                .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val slot = cmTag.slot
        when {
            selected -> {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            slot != null -> {
                CrewMemberDot(slot = slot)
            }
        }
        Text(text = stringResource(cmTag.label), style = MaterialTheme.typography.labelLarge, color = contentColor)
    }
}

@Composable
private fun ObservableBehaviourDropdown(
    selected: ObservableBehaviour?,
    choices: List<ObservableBehaviour>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelect: (Long?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.shapes.medium
    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clip(shape)
                    .border(width = 1.dp, color = MaterialTheme.colorScheme.outline, shape = shape)
                    .clickable(role = Role.DropdownList) { onExpandedChange(true) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.capture_obs_beh),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = selected?.name ?: stringResource(R.string.capture_obs_beh_none),
                style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Icon(
                painter = painterResource(R.drawable.ic_expand_more),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.capture_obs_beh_none)) },
                onClick = {
                    onSelect(null)
                    onExpandedChange(false)
                }
            )
            choices.forEach { choice ->
                DropdownMenuItem(
                    text = { Text(text = choice.name, fontStyle = FontStyle.Italic) },
                    onClick = {
                        onSelect(choice.id)
                        onExpandedChange(false)
                    }
                )
            }
        }
    }
}

@Composable
private fun ObservableBehaviourLabel(
    names: List<String>,
    modifier: Modifier = Modifier,
) {
    Text(
        text = names.joinToString("  "),
        style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

private fun Modifier.dashedBorder(
    color: Color,
    shape: Shape,
): Modifier =
    drawBehind {
        val stroke =
            Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())))
        val outline = shape.createOutline(size, layoutDirection, this)
        drawOutline(outline = outline, color = color, style = stroke)
    }

@Composable
private fun StaffNumberChip(
    slot: CrewMemberSlot,
    staffNumber: StaffNumber,
) {
    Row(
        modifier =
            Modifier
                .border(width = 1.dp, color = MaterialTheme.colorScheme.outline, shape = MaterialTheme.shapes.large)
                .background(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.large)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CrewMemberDot(slot = slot)
        Text(
            text = stringResource(slot.label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(text = staffNumber.digits, style = CheckPilotTheme.monoTypography.extraSmall)
    }
}

/** The Competency's number and title, with the OBS BEH its Notes link in small italic under the title. */
@Composable
private fun CompetencyHeader(
    number: Int,
    competency: Competency,
    linkedObservableBehaviours: List<ObservableBehaviour>,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompetencyNumber(number = number)
            Text(text = competency.name, style = MaterialTheme.typography.titleLarge)
        }
        if (linkedObservableBehaviours.isNotEmpty()) {
            ObservableBehaviourLabel(
                names = linkedObservableBehaviours.map { it.name },
                modifier = Modifier.padding(start = 40.dp, top = 2.dp)
            )
        }
    }
}

@Composable
private fun CompetencyNumber(number: Int) {
    Box(
        modifier =
            Modifier
                .size(28.dp)
                .background(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.small),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
