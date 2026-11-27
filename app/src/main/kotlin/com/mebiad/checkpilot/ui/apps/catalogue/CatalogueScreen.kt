package com.mebiad.checkpilot.ui.apps.catalogue

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mebiad.checkpilot.R
import com.mebiad.checkpilot.ui.generic.components.BackTopBar
import com.mebiad.checkpilot.ui.generic.components.PrimaryButton
import com.mebiad.checkpilot.ui.generic.components.checkPilotFieldColors
import kotlin.math.abs

@Composable
fun CatalogueScreen(
    viewModel: CatalogueViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CatalogueContent(
        state = state,
        onBack = onBack,
        onAddCompetency = viewModel::onAddCompetency,
        onToggle = viewModel::onToggle,
        onRenameCompetency = viewModel::onRenameCompetency,
        onDeleteCompetency = viewModel::onDeleteCompetency,
        onMoveCompetency = viewModel::onMoveCompetency,
        onAddObservableBehaviour = viewModel::onAddObservableBehaviour,
        onRenameObservableBehaviour = viewModel::onRenameObservableBehaviour,
        onDeleteObservableBehaviour = viewModel::onDeleteObservableBehaviour,
        onMoveObservableBehaviour = viewModel::onMoveObservableBehaviour
    )
    state.nameDialog?.let { dialog ->
        NameDialog(dialog = dialog, onSave = viewModel::onNameSave, onDismiss = viewModel::onNameDismiss)
    }
}

@Composable
private fun CatalogueContent(
    state: CatalogueUiState,
    onBack: () -> Unit,
    onAddCompetency: () -> Unit,
    onToggle: (Long) -> Unit,
    onRenameCompetency: (Long) -> Unit,
    onDeleteCompetency: (Long) -> Unit,
    onMoveCompetency: (id: Long, by: Int) -> Unit,
    onAddObservableBehaviour: (Long) -> Unit,
    onRenameObservableBehaviour: (Long) -> Unit,
    onDeleteObservableBehaviour: (Long) -> Unit,
    onMoveObservableBehaviour: (id: Long, by: Int) -> Unit,
) {
    // The top bar is the list's sticky header rather than the Scaffold's, so "+ Competency" sits in the scrolling
    // container the way every other control of the editor does.
    Scaffold(contentWindowInsets = WindowInsets.navigationBars) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            stickyHeader(key = HEADER_KEY) {
                BackTopBar(
                    title = stringResource(R.string.catalogue_title),
                    onBack = onBack,
                    actions = {
                        AddButton(
                            text = stringResource(R.string.catalogue_add_competency),
                            onClick = onAddCompetency,
                            outlined = true,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                )
            }
            items(state.competencies, key = { it.id }) { competency ->
                CompetencyCard(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    competency = competency,
                    onToggle = { onToggle(competency.id) },
                    onRename = { onRenameCompetency(competency.id) },
                    onDelete = { onDeleteCompetency(competency.id) },
                    onMove = { by -> onMoveCompetency(competency.id, by) },
                    onAddObservableBehaviour = { onAddObservableBehaviour(competency.id) },
                    onRenameObservableBehaviour = onRenameObservableBehaviour,
                    onDeleteObservableBehaviour = onDeleteObservableBehaviour,
                    onMoveObservableBehaviour = onMoveObservableBehaviour
                )
            }
        }
    }
}

@Composable
private fun CompetencyCard(
    competency: CompetencyItem,
    onToggle: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onMove: (by: Int) -> Unit,
    onAddObservableBehaviour: () -> Unit,
    onRenameObservableBehaviour: (Long) -> Unit,
    onDeleteObservableBehaviour: (Long) -> Unit,
    onMoveObservableBehaviour: (id: Long, by: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val drag = rememberDragState(onMove)
    Surface(
        modifier = modifier.fillMaxWidth().then(drag.itemModifier),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border =
            if (competency.expanded) {
                BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
            } else {
                null
            },
        shadowElevation = if (drag.dragging) 8.dp else 0.dp
    ) {
        Column {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 72.dp)
                        .clickable(onClick = onToggle)
                        .padding(start = 4.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DragHandle(onMove = onMove, modifier = drag.handleModifier)
                Column(modifier = Modifier.weight(1f).padding(vertical = 12.dp)) {
                    Text(text = competency.name, style = MaterialTheme.typography.titleLarge)
                    if (competency.expanded && competency.usedInSessions > 0) {
                        UsedIn(sessions = competency.usedInSessions)
                    }
                }
                if (competency.expanded) {
                    if (competency.usedInSessions == 0) DeleteButton(onClick = onDelete)
                    IconButton(onClick = onRename) {
                        Icon(
                            painter = painterResource(R.drawable.ic_edit),
                            contentDescription = stringResource(R.string.catalogue_rename),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    val count = competency.observableBehaviours.size
                    val description = pluralStringResource(R.plurals.catalogue_obs_beh_count, count, count)
                    Text(
                        text = count.toString(),
                        modifier = Modifier.semantics { contentDescription = description },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onToggle) {
                    Icon(
                        painter = painterResource(R.drawable.ic_expand_more),
                        contentDescription =
                            stringResource(
                                if (competency.expanded) R.string.catalogue_collapse else R.string.catalogue_expand
                            ),
                        modifier = Modifier.graphicsLayer { rotationZ = if (competency.expanded) 180f else 0f },
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (competency.expanded) {
                Column(modifier = Modifier.padding(start = 16.dp, end = 8.dp, bottom = 8.dp)) {
                    competency.observableBehaviours.forEach { behaviour ->
                        key(behaviour.id) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            ObservableBehaviourRow(
                                behaviour = behaviour,
                                onRename = { onRenameObservableBehaviour(behaviour.id) },
                                onDelete = { onDeleteObservableBehaviour(behaviour.id) },
                                onMove = { by -> onMoveObservableBehaviour(behaviour.id, by) }
                            )
                        }
                    }
                    AddButton(
                        text = stringResource(R.string.catalogue_add_obs_beh),
                        onClick = onAddObservableBehaviour,
                        outlined = false,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ObservableBehaviourRow(
    behaviour: ObservableBehaviourItem,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onMove: (by: Int) -> Unit,
) {
    val drag = rememberDragState(onMove)
    Surface(
        modifier = Modifier.fillMaxWidth().then(drag.itemModifier),
        color =
            if (drag.dragging) {
                MaterialTheme.colorScheme.surfaceContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
        shadowElevation = if (drag.dragging) 8.dp else 0.dp
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .clickable(onClickLabel = stringResource(R.string.catalogue_rename), onClick = onRename),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DragHandle(onMove = onMove, modifier = drag.handleModifier)
            Text(
                text = behaviour.name,
                modifier = Modifier.weight(1f).padding(vertical = 12.dp),
                style = MaterialTheme.typography.bodyLarge
            )
            if (behaviour.usedInSessions > 0) {
                UsedIn(sessions = behaviour.usedInSessions, modifier = Modifier.padding(start = 12.dp, end = 4.dp))
            } else {
                DeleteButton(onClick = onDelete)
            }
        }
    }
}

/**
 * The handle an item is dragged by. TalkBack can't drag, so the handle also offers "Move up" and "Move down" as
 * accessibility actions.
 */
@Composable
private fun DragHandle(
    onMove: (by: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val moveUp = stringResource(R.string.catalogue_move_up)
    val moveDown = stringResource(R.string.catalogue_move_down)
    val description = stringResource(R.string.catalogue_drag_handle)
    Box(
        modifier =
            modifier
                .size(48.dp)
                .semantics(mergeDescendants = true) {
                    contentDescription = description
                    customActions =
                        listOf(
                            CustomAccessibilityAction(moveUp) {
                                onMove(-1)
                                true
                            },
                            CustomAccessibilityAction(moveDown) {
                                onMove(1)
                                true
                            }
                        )
                },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_drag_indicator),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Follows a vertical drag of an item's handle: the item moves with the finger, and every time it has travelled its own
 * height it moves one place in the list.
 */
private class DragState(
    val dragging: Boolean,
    val itemModifier: Modifier,
    val handleModifier: Modifier,
)

@Composable
private fun rememberDragState(onMove: (by: Int) -> Unit): DragState {
    val currentOnMove by rememberUpdatedState(onMove)
    var offset by remember { mutableFloatStateOf(0f) }
    var height by remember { mutableIntStateOf(0) }
    var dragging by remember { mutableStateOf(false) }
    val itemModifier =
        Modifier
            .onSizeChanged { height = it.height }
            .graphicsLayer { translationY = offset }
    val handleModifier =
        Modifier.pointerInput(Unit) {
            detectVerticalDragGestures(
                onDragStart = { dragging = true },
                onDragEnd = {
                    dragging = false
                    offset = 0f
                },
                onDragCancel = {
                    dragging = false
                    offset = 0f
                }
            ) { change, amount ->
                change.consume()
                offset += amount
                if (height > 0 && abs(offset) >= height) {
                    val by = if (offset > 0) 1 else -1
                    currentOnMove(by)
                    offset -= by * height
                }
            }
        }
    return DragState(dragging = dragging, itemModifier = itemModifier, handleModifier = handleModifier)
}

@Composable
private fun UsedIn(
    sessions: Int,
    modifier: Modifier = Modifier,
) {
    Text(
        text = pluralStringResource(R.plurals.catalogue_used_in_sessions, sessions, sessions),
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun DeleteButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            painter = painterResource(R.drawable.ic_delete),
            contentDescription = stringResource(R.string.catalogue_delete),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** "+ Competency" in the top bar, outlined, and "+ OBS BEH" under an expanded Competency's, as text. */
@Composable
private fun AddButton(
    text: String,
    onClick: () -> Unit,
    outlined: Boolean,
    modifier: Modifier = Modifier,
) {
    val content: @Composable () -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(painter = painterResource(R.drawable.ic_add), contentDescription = null)
            Text(text = text, style = MaterialTheme.typography.labelLarge)
        }
    }
    if (outlined) {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.heightIn(min = 56.dp),
            border = BorderStroke(width = 1.5.dp, color = MaterialTheme.colorScheme.primary)
        ) {
            content()
        }
    } else {
        TextButton(onClick = onClick, modifier = modifier.heightIn(min = 56.dp)) { content() }
    }
}

/** Asks for the name of the item [dialog] adds or renames, starting from its current name. */
@Composable
private fun NameDialog(
    dialog: NameDialog,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by rememberSaveable(dialog, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(dialog.name, selection = TextRange(dialog.name.length)))
    }
    val focusRequester = remember { FocusRequester() }
    val save = { if (value.text.isNotBlank()) onSave(value.text) }
    // With the platform's default width, a text field in a Dialog never lets Robolectric go idle on the acceptance
    // tests' 8000 dp tall screen, so the dialog sets its own width.
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        LaunchedEffect(focusRequester) { focusRequester.requestFocus() }
        Surface(
            modifier = Modifier.padding(horizontal = 24.dp).widthIn(max = 560.dp).fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(text = stringResource(dialog.title), style = MaterialTheme.typography.headlineSmall)
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    modifier = Modifier.padding(top = 20.dp).fillMaxWidth().focusRequester(focusRequester),
                    label = { Text(stringResource(R.string.catalogue_name)) },
                    singleLine = true,
                    keyboardOptions =
                        KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { save() }),
                    shape = MaterialTheme.shapes.medium,
                    colors = checkPilotFieldColors()
                )
                PrimaryButton(
                    text = stringResource(R.string.catalogue_save),
                    onClick = save,
                    modifier = Modifier.padding(top = 24.dp)
                )
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.padding(top = 8.dp).fillMaxWidth().heightIn(min = 56.dp)
                ) {
                    Text(text = stringResource(R.string.cancel), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

private val NameDialog.title: Int
    get() =
        when (this) {
            NameDialog.AddCompetency -> R.string.catalogue_new_competency
            is NameDialog.RenameCompetency -> R.string.catalogue_rename_competency
            is NameDialog.AddObservableBehaviour -> R.string.catalogue_new_obs_beh
            is NameDialog.RenameObservableBehaviour -> R.string.catalogue_rename_obs_beh
        }

private const val HEADER_KEY = "header"
