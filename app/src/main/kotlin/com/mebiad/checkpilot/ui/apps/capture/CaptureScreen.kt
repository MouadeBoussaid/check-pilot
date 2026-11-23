package com.mebiad.checkpilot.ui.apps.capture

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mebiad.checkpilot.R
import com.mebiad.checkpilot.domain.catalogue.Competency
import com.mebiad.checkpilot.domain.session.Session
import com.mebiad.checkpilot.domain.session.StaffNumber
import com.mebiad.checkpilot.ui.generic.components.CrewMemberDot
import com.mebiad.checkpilot.ui.generic.components.CrewMemberSlot
import com.mebiad.checkpilot.ui.generic.components.RouteText
import com.mebiad.checkpilot.ui.generic.components.SecondaryButton
import com.mebiad.checkpilot.ui.generic.components.UndoSnackbarHost
import com.mebiad.checkpilot.ui.generic.components.showUndo
import com.mebiad.checkpilot.ui.generic.format.toDisplayText
import com.mebiad.checkpilot.ui.generic.theme.CheckPilotTheme
import kotlinx.coroutines.flow.collectLatest

@Composable
fun CaptureScreen(
    viewModel: CaptureViewModel,
    onDone: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val noteErased = stringResource(R.string.capture_note_erased)
    val undo = stringResource(R.string.undo)
    LaunchedEffect(viewModel) {
        // collectLatest: a newer erase replaces the snackbar showing, which makes the older erase final.
        viewModel.events.collectLatest { event ->
            when (event) {
                is CaptureEvent.NoteErased -> {
                    if (snackbarHostState.showUndo(message = noteErased, undoLabel = undo)) viewModel.undo(event.token)
                }
            }
        }
    }
    CaptureContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onFieldTextChange = viewModel::onFieldTextChange,
        onFieldFocusLost = viewModel::onFieldFocusLost,
        onDeleteField = viewModel::onDeleteField,
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
    onDone: () -> Unit,
) {
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
                    CompetencyHeader(number = index + 1, competency = competencyNotes.competency)
                    competencyNotes.fields.forEach { field ->
                        key(field.key) {
                            NoteFieldCard(
                                field = field,
                                testTag = "note-field-$competencyId",
                                onTextChange = { text -> onFieldTextChange(competencyId, field.key, text) },
                                onFocusLost = { onFieldFocusLost(field.key) },
                                onDelete = { onDeleteField(field.key) }
                            )
                        }
                    }
                }
                SecondaryButton(
                    text = stringResource(R.string.capture_done),
                    onClick = onDone,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 32.dp, bottom = 24.dp)
                )
            }
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
 */
@Composable
private fun NoteFieldCard(
    field: NoteField,
    testTag: String,
    onTextChange: (String) -> Unit,
    onFocusLost: () -> Unit,
    onDelete: () -> Unit,
) {
    var value by remember { mutableStateOf(TextFieldValue(field.text, TextRange(field.text.length))) }
    var focused by remember { mutableStateOf(false) }
    val shape = MaterialTheme.shapes.small
    val border =
        when {
            !field.isNote -> Modifier.dashedBorder(color = MaterialTheme.colorScheme.outline, shape = shape)
            focused -> Modifier.border(width = 2.dp, color = MaterialTheme.colorScheme.primary, shape = shape)
            else -> Modifier
        }
    val background =
        if (field.isNote) Modifier.background(MaterialTheme.colorScheme.surfaceContainerLow, shape) else Modifier
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .then(background)
                .then(border)
                .heightIn(min = 56.dp),
        verticalAlignment = Alignment.Top
    ) {
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
        if (focused && field.isNote) {
            IconButton(onClick = onDelete, modifier = Modifier.padding(top = 4.dp, end = 4.dp)) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.capture_delete_note),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
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

@Composable
private fun CompetencyHeader(
    number: Int,
    competency: Competency,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
        Text(text = competency.name, style = MaterialTheme.typography.titleLarge)
    }
}
