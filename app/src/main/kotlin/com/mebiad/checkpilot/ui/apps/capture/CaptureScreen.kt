package com.mebiad.checkpilot.ui.apps.capture

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mebiad.checkpilot.domain.catalogue.Competency
import com.mebiad.checkpilot.domain.session.Session
import com.mebiad.checkpilot.domain.session.StaffNumber
import com.mebiad.checkpilot.ui.generic.components.CrewMemberDot
import com.mebiad.checkpilot.ui.generic.components.CrewMemberSlot
import com.mebiad.checkpilot.ui.generic.components.RouteText
import com.mebiad.checkpilot.ui.generic.format.toDisplayText
import com.mebiad.checkpilot.ui.generic.theme.CheckPilotTheme

@Composable
fun CaptureScreen(viewModel: CaptureViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CaptureContent(state = state)
}

@Composable
private fun CaptureContent(state: CaptureUiState) {
    Scaffold { innerPadding ->
        val session = state.session ?: return@Scaffold
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = innerPadding
        ) {
            item { SessionHeader(session = session) }
            itemsIndexed(state.competencies, key = { _, competency -> competency.id }) { index, competency ->
                CompetencyHeader(number = index + 1, competency = competency)
            }
        }
    }
}

@Composable
private fun SessionHeader(session: Session) {
    Column(modifier = Modifier.statusBarsPadding().padding(top = 16.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
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
