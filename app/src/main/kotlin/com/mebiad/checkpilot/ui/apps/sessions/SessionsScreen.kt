package com.mebiad.checkpilot.ui.apps.sessions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mebiad.checkpilot.R
import com.mebiad.checkpilot.domain.session.CrewMember
import com.mebiad.checkpilot.domain.session.Session
import com.mebiad.checkpilot.domain.session.SessionSummary
import com.mebiad.checkpilot.ui.generic.components.BackTopBar
import com.mebiad.checkpilot.ui.generic.components.CrewMemberDot
import com.mebiad.checkpilot.ui.generic.components.PickDateDialog
import com.mebiad.checkpilot.ui.generic.components.RouteText
import com.mebiad.checkpilot.ui.generic.components.SecondaryButton
import com.mebiad.checkpilot.ui.generic.components.checkPilotFieldColors
import com.mebiad.checkpilot.ui.generic.components.slot
import com.mebiad.checkpilot.ui.generic.format.toDisplayText
import com.mebiad.checkpilot.ui.generic.theme.CheckPilotTheme
import java.time.LocalDate

@Composable
fun SessionsScreen(
    viewModel: SessionsViewModel,
    onBack: () -> Unit,
    onCapture: (sessionId: String) -> Unit,
    onDebrief: (sessionId: String, crewMember: CrewMember) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SessionsContent(
        state = state,
        onBack = onBack,
        onQueryChange = viewModel::onQueryChange,
        onDatePick = viewModel::onDatePick,
        onDateClear = viewModel::onDateClear,
        onCapture = onCapture,
        onDebrief = onDebrief,
        onDeleteAsk = viewModel::onDeleteAsk,
        onDeleteConfirm = viewModel::onDeleteConfirm,
        onDeleteCancel = viewModel::onDeleteCancel
    )
}

@Composable
private fun SessionsContent(
    state: SessionsUiState,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onDatePick: (LocalDate) -> Unit,
    onDateClear: () -> Unit,
    onCapture: (sessionId: String) -> Unit,
    onDebrief: (sessionId: String, crewMember: CrewMember) -> Unit,
    onDeleteAsk: (SessionSummary) -> Unit,
    onDeleteConfirm: () -> Unit,
    onDeleteCancel: () -> Unit,
) {
    var picking by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        topBar = { BackTopBar(title = stringResource(R.string.sessions_title), onBack = onBack) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            SearchBar(
                query = state.query,
                dateFiltered = state.date != null,
                onQueryChange = onQueryChange,
                onFilterByDate = { picking = true }
            )
            state.date?.let { DateFilter(date = it, onClear = onDateClear) }
            SessionList(
                state = state,
                onCapture = onCapture,
                onDebrief = onDebrief,
                onDeleteAsk = onDeleteAsk
            )
        }
    }
    if (picking) {
        PickDateDialog(
            initial = state.date ?: state.today,
            onPick = { date ->
                picking = false
                onDatePick(date)
            },
            onDismiss = { picking = false }
        )
    }
    state.pendingDelete?.let { summary ->
        DeleteDialog(session = summary.session, onDelete = onDeleteConfirm, onCancel = onDeleteCancel)
    }
}

@Composable
private fun SearchBar(
    query: String,
    dateFiltered: Boolean,
    onQueryChange: (String) -> Unit,
    onFilterByDate: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text(text = stringResource(R.string.sessions_search_hint)) },
            leadingIcon = { Icon(painter = painterResource(R.drawable.ic_search), contentDescription = null) },
            singleLine = true,
            keyboardOptions =
                KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    autoCorrectEnabled = false,
                    imeAction = ImeAction.Search
                ),
            shape = MaterialTheme.shapes.medium,
            colors = checkPilotFieldColors()
        )
        Surface(
            onClick = onFilterByDate,
            modifier = Modifier.size(56.dp),
            shape = MaterialTheme.shapes.medium,
            color = if (dateFiltered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background,
            contentColor =
                if (dateFiltered) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.outline)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.ic_calendar),
                    contentDescription = stringResource(R.string.sessions_filter_by_date)
                )
            }
        }
    }
}

@Composable
private fun DateFilter(
    date: LocalDate,
    onClear: () -> Unit,
) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Row(
                modifier = Modifier.padding(start = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = date.toDisplayText(), style = MaterialTheme.typography.titleSmall)
                IconButton(onClick = onClear) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(R.string.sessions_clear_date)
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionList(
    state: SessionsUiState,
    onCapture: (sessionId: String) -> Unit,
    onDebrief: (sessionId: String, crewMember: CrewMember) -> Unit,
    onDeleteAsk: (SessionSummary) -> Unit,
) {
    if (state.sessions.isEmpty()) {
        val filtered = state.query.isNotBlank() || state.date != null
        Text(
            text = stringResource(if (filtered) R.string.sessions_none_found else R.string.sessions_none_saved),
            modifier = Modifier.padding(24.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(state.sessions, key = { it.session.id }) { summary ->
            SessionCard(
                summary = summary,
                onCapture = { onCapture(summary.session.id) },
                onDebrief = { onDebrief(summary.session.id, summary.session.crewMembers.first()) },
                onDelete = { onDeleteAsk(summary) }
            )
        }
    }
}

@Composable
private fun SessionCard(
    summary: SessionSummary,
    onCapture: () -> Unit,
    onDebrief: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier.padding(start = 20.dp, top = 20.dp, end = 8.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SessionHeading(session = summary.session, modifier = Modifier.padding(end = 12.dp))
            CrewMembers(session = summary.session, summary = summary)
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CardButton(text = stringResource(R.string.sessions_capture), onClick = onCapture)
                CardButton(text = stringResource(R.string.sessions_debrief), onClick = onDebrief)
                Spacer(modifier = Modifier.weight(1f))
                MoreOptions(onDelete = onDelete)
            }
        }
    }
}

@Composable
private fun SessionHeading(
    session: Session,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        RouteText(
            departure = session.departure,
            destination = session.destination,
            style = CheckPilotTheme.monoTypography.medium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = session.date.toDisplayText(),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Each present Crew Member's dot and Staff Number, followed by their Grade count when [summary] is given. */
@Composable
private fun CrewMembers(
    session: Session,
    summary: SessionSummary?,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        session.crewMembers.forEach { crewMember ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CrewMemberDot(slot = crewMember.slot)
                Text(
                    text = checkNotNull(session.staffNumberOf(crewMember)).digits,
                    style = CheckPilotTheme.monoTypography.extraSmall
                )
                summary?.let {
                    Text(
                        text =
                            stringResource(
                                R.string.sessions_grade_count,
                                it.gradedCountOf(crewMember),
                                it.competencyCount
                            ),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun CardButton(
    text: String,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.height(56.dp),
        shape = MaterialTheme.shapes.medium,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
        contentPadding = PaddingValues(horizontal = 24.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun MoreOptions(onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }, modifier = Modifier.size(56.dp)) {
            Icon(
                painter = painterResource(R.drawable.ic_more_vert),
                contentDescription = stringResource(R.string.sessions_more_options)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = MaterialTheme.shapes.medium,
            containerColor = MaterialTheme.colorScheme.surfaceBright,
            border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.outline)
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.sessions_delete),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                },
                onClick = {
                    expanded = false
                    onDelete()
                },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_delete),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
            )
        }
    }
}

@Composable
private fun DeleteDialog(
    session: Session,
    onDelete: () -> Unit,
    onCancel: () -> Unit,
) {
    Dialog(onDismissRequest = onCancel) {
        Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainer) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = stringResource(R.string.sessions_delete_title),
                    style = MaterialTheme.typography.headlineSmall
                )
                Surface(
                    modifier = Modifier.padding(top = 20.dp).fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SessionHeading(session = session)
                        CrewMembers(session = session, summary = null)
                    }
                }
                Text(
                    text = stringResource(R.string.sessions_delete_body),
                    modifier = Modifier.padding(top = 20.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = onDelete,
                    modifier = Modifier.padding(top = 24.dp).fillMaxWidth().height(56.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                ) {
                    Text(text = stringResource(R.string.sessions_delete), style = MaterialTheme.typography.labelLarge)
                }
                SecondaryButton(
                    text = stringResource(R.string.cancel),
                    onClick = onCancel,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
    }
}
