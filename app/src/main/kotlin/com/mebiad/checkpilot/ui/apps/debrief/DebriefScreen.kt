package com.mebiad.checkpilot.ui.apps.debrief

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mebiad.checkpilot.R
import com.mebiad.checkpilot.domain.debrief.Debrief
import com.mebiad.checkpilot.domain.debrief.DebriefCompetency
import com.mebiad.checkpilot.domain.debrief.DebriefNote
import com.mebiad.checkpilot.domain.grade.Grade
import com.mebiad.checkpilot.domain.session.CrewMember
import com.mebiad.checkpilot.domain.session.Session
import com.mebiad.checkpilot.ui.generic.components.CmTagBadge
import com.mebiad.checkpilot.ui.generic.components.CrewMemberDot
import com.mebiad.checkpilot.ui.generic.components.PickDateDialog
import com.mebiad.checkpilot.ui.generic.components.PrimaryButton
import com.mebiad.checkpilot.ui.generic.components.RouteText
import com.mebiad.checkpilot.ui.generic.components.label
import com.mebiad.checkpilot.ui.generic.components.slot
import com.mebiad.checkpilot.ui.generic.format.toDisplayText
import com.mebiad.checkpilot.ui.generic.theme.CheckPilotTheme
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun DebriefScreen(
    viewModel: DebriefViewModel,
    onLeave: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentOnLeave by rememberUpdatedState(onLeave)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                DebriefEvent.Leave -> currentOnLeave()
            }
        }
    }
    BackHandler { viewModel.onBack() }
    DebriefContent(
        state = state,
        onBack = viewModel::onBack,
        onSelect = viewModel::onSelect,
        onGradeSelect = viewModel::onGradeSelect,
        onDateChange = viewModel::onDateChange,
        onStay = viewModel::onStay,
        onLeave = viewModel::onLeave
    )
}

@Composable
private fun DebriefContent(
    state: DebriefUiState,
    onBack: () -> Unit,
    onSelect: (CrewMember) -> Unit,
    onGradeSelect: (competencyId: Long, grade: Grade) -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onStay: () -> Unit,
    onLeave: () -> Unit,
) {
    var notesShownOf by rememberSaveable { mutableStateOf<Long?>(null) }
    Scaffold { innerPadding ->
        val session = state.session ?: return@Scaffold
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            DebriefTopBar(session = session, onBack = onBack, onDateChange = onDateChange)
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(start = 16.dp, end = 16.dp, bottom = 24.dp)
            ) {
                if (session.hasTwoCrewMembers) {
                    CrewMemberToggle(session = session, selected = state.selected, onSelect = onSelect)
                }
                val debrief = state.debrief ?: return@Column
                SummaryHeader(debrief = debrief)
                debrief.competencies.forEach { debriefCompetency ->
                    key(debriefCompetency.competency.id) {
                        CompetencyCard(
                            debriefCompetency = debriefCompetency,
                            onShowNotes = { notesShownOf = debriefCompetency.competency.id },
                            onGradeSelect = { grade -> onGradeSelect(debriefCompetency.competency.id, grade) }
                        )
                    }
                }
            }
        }
        val shown = state.debrief?.competencies?.firstOrNull { it.competency.id == notesShownOf }
        if (shown != null && shown.notes.isNotEmpty()) {
            NotesSheet(
                crewMember = state.selected,
                debriefCompetency = shown,
                showCmTags = session.hasTwoCrewMembers,
                onClose = { notesShownOf = null }
            )
        }
        state.ungradedWarning?.let { ungraded ->
            LeaveDialog(ungraded = ungraded, onStay = onStay, onLeave = onLeave)
        }
    }
}

@Composable
private fun DebriefTopBar(
    session: Session,
    onBack: () -> Unit,
    onDateChange: (LocalDate) -> Unit,
) {
    var picking by rememberSaveable { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(start = 4.dp, end = 16.dp, top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = stringResource(R.string.back)
            )
        }
        Text(text = stringResource(R.string.debrief_title), style = MaterialTheme.typography.headlineSmall)
        RouteText(
            departure = session.departure,
            destination = session.destination,
            style = CheckPilotTheme.monoTypography.extraSmall,
            modifier = Modifier.weight(1f)
        )
        Surface(
            onClick = { picking = true },
            modifier = Modifier.heightIn(min = 48.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.background,
            border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = session.date.toDisplayText(), style = MaterialTheme.typography.titleSmall)
                Icon(
                    painter = painterResource(R.drawable.ic_edit),
                    contentDescription = stringResource(R.string.debrief_change_date),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
    if (picking) {
        PickDateDialog(
            initial = session.date,
            onPick = { picked ->
                picking = false
                onDateChange(picked)
            },
            onDismiss = { picking = false }
        )
    }
}

/** Switches the Debrief between the two Crew Members in place; it isn't leaving the Debrief. */
@Composable
private fun CrewMemberToggle(
    session: Session,
    selected: CrewMember,
    onSelect: (CrewMember) -> Unit,
) {
    val shape = MaterialTheme.shapes.medium
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .background(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = shape)
                .padding(4.dp)
                .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        session.crewMembers.forEach { crewMember ->
            val isSelected = crewMember == selected
            val background =
                if (isSelected) {
                    Modifier.background(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = shape)
                } else {
                    Modifier
                }
            Row(
                modifier =
                    Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .clip(shape)
                        .then(background)
                        .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(crewMember) })
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val contentColor =
                    if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                CrewMemberDot(slot = crewMember.slot)
                Text(
                    text = stringResource(crewMember.slot.label),
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor
                )
                session.staffNumberOf(crewMember)?.let { staffNumber ->
                    Text(
                        text = staffNumber.digits,
                        style = CheckPilotTheme.monoTypography.extraSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryHeader(debrief: Debrief) {
    val total = debrief.competencies.size
    Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = pluralStringResource(R.plurals.debrief_grades_set, total, debrief.gradedCount, total),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = pluralStringResource(R.plurals.debrief_note_count, debrief.noteCount, debrief.noteCount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.debrief_obs_beh_count, debrief.observableBehaviourCount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(total) { index ->
                val color =
                    if (index < debrief.gradedCount) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    }
                Box(modifier = Modifier.weight(1f).height(4.dp).background(color = color, shape = CircleShape))
            }
        }
    }
}

/** The Competency's evidence for the Crew Member and its Grade selector; only the selector and the eye take a tap. */
@Composable
private fun CompetencyCard(
    debriefCompetency: DebriefCompetency,
    onShowNotes: () -> Unit,
    onGradeSelect: (Grade) -> Unit,
) {
    val hasNotes = debriefCompetency.notes.isNotEmpty()
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .background(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.medium)
                .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = debriefCompetency.competency.name, style = MaterialTheme.typography.titleLarge)
                if (hasNotes) {
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val noteCount = debriefCompetency.notes.size
                        Text(
                            text = pluralStringResource(R.plurals.debrief_note_count, noteCount, noteCount),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = debriefCompetency.observableBehaviours.joinToString("  ") { it.name },
                            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (hasNotes) {
                IconButton(onClick = onShowNotes) {
                    Icon(
                        painter = painterResource(R.drawable.ic_visibility),
                        contentDescription = stringResource(R.string.debrief_show_notes)
                    )
                }
            }
        }
        GradeSelector(
            grade = debriefCompetency.grade,
            onSelect = onGradeSelect,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}

@Composable
private fun GradeSelector(
    grade: Grade?,
    onSelect: (Grade) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    var width by remember { mutableIntStateOf(0) }
    val shape = MaterialTheme.shapes.medium
    val borderColor = if (expanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .onSizeChanged { width = it.width }
                    .clip(shape)
                    .border(width = if (expanded) 2.dp else 1.dp, color = borderColor, shape = shape)
                    .clickable(role = Role.DropdownList) { expanded = true }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (grade != null) {
                Text(
                    text = stringResource(grade.label),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
            } else {
                Text(
                    text = stringResource(R.string.debrief_select_grade),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                painter = painterResource(R.drawable.ic_expand_more),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(with(LocalDensity.current) { width.toDp() })
        ) {
            Grade.entries.forEach { choice ->
                DropdownMenuItem(
                    text = { Text(text = stringResource(choice.label), style = MaterialTheme.typography.bodyLarge) },
                    onClick = {
                        expanded = false
                        onSelect(choice)
                    }
                )
                if (choice == Grade.NOT_OBSERVED) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

/** The eye pop-up: the Notes about the Crew Member under one Competency, each with its OBS BEH. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotesSheet(
    crewMember: CrewMember,
    debriefCompetency: DebriefCompetency,
    showCmTags: Boolean,
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
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = debriefCompetency.competency.name, style = MaterialTheme.typography.headlineSmall)
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val noteCount = debriefCompetency.notes.size
                        CrewMemberDot(slot = crewMember.slot)
                        Text(
                            text = stringResource(crewMember.slot.label),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = pluralStringResource(R.plurals.debrief_note_count, noteCount, noteCount),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(
                    onClick = { scope.launch { sheetState.hide() }.invokeOnCompletion { onClose() } }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(R.string.close)
                    )
                }
            }
            debriefCompetency.notes.forEach { debriefNote ->
                key(debriefNote.note.id) {
                    NoteCard(
                        debriefNote = debriefNote,
                        showCmTag = showCmTags,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun NoteCard(
    debriefNote: DebriefNote,
    showCmTag: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.small)
                .padding(16.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            Text(
                text = debriefNote.note.text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            if (showCmTag) CmTagBadge(cmTag = debriefNote.note.cmTag)
        }
        val observableBehaviour = debriefNote.observableBehaviour
        Text(
            text = observableBehaviour?.name ?: stringResource(R.string.debrief_no_obs_beh),
            style =
                if (observableBehaviour != null) {
                    MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic)
                } else {
                    MaterialTheme.typography.bodySmall
                },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

/** Asks before leaving a Debrief whose [ungraded] Competencies have no Grade; dismissing it stays. */
@Composable
private fun LeaveDialog(
    ungraded: Int,
    onStay: () -> Unit,
    onLeave: () -> Unit,
) {
    Dialog(onDismissRequest = onStay) {
        Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainer) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = pluralStringResource(R.plurals.debrief_competencies_without_grade, ungraded, ungraded),
                    style = MaterialTheme.typography.headlineSmall
                )
                PrimaryButton(
                    text = stringResource(R.string.debrief_stay),
                    onClick = onStay,
                    modifier = Modifier.padding(top = 24.dp)
                )
                OutlinedButton(
                    onClick = onLeave,
                    modifier = Modifier.padding(top = 12.dp).fillMaxWidth().height(56.dp),
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Text(
                        text = stringResource(R.string.debrief_leave),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

private val Grade.label: Int
    @StringRes get() =
        when (this) {
            Grade.NOT_OBSERVED -> R.string.grade_not_observed
            Grade.EXEMPLARY -> R.string.grade_exemplary
            Grade.EFFECTIVE -> R.string.grade_effective
            Grade.ADEQUATE -> R.string.grade_adequate
            Grade.MINIMUM_ACCEPTABLE_LEVEL -> R.string.grade_minimum_acceptable_level
            Grade.NOT_YET_COMPETENT -> R.string.grade_not_yet_competent
        }
