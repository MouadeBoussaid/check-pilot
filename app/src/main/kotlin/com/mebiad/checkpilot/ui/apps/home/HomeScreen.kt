package com.mebiad.checkpilot.ui.apps.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mebiad.checkpilot.R
import com.mebiad.checkpilot.domain.session.Session
import com.mebiad.checkpilot.domain.session.StaffNumber
import com.mebiad.checkpilot.ui.generic.components.CrewMemberDot
import com.mebiad.checkpilot.ui.generic.components.CrewMemberSlot
import com.mebiad.checkpilot.ui.generic.components.PrimaryButton
import com.mebiad.checkpilot.ui.generic.components.RouteText
import com.mebiad.checkpilot.ui.generic.format.toDisplayText
import com.mebiad.checkpilot.ui.generic.theme.CheckPilotTheme

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onContinueLastSession: (sessionId: String) -> Unit,
    onNewSession: () -> Unit,
    onOpenSession: () -> Unit,
    onSettings: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HomeContent(
        state = state,
        onContinueLastSession = onContinueLastSession,
        onNewSession = onNewSession,
        onOpenSession = onOpenSession,
        onSettings = onSettings
    )
}

@Composable
private fun HomeContent(
    state: HomeUiState,
    onContinueLastSession: (sessionId: String) -> Unit,
    onNewSession: () -> Unit,
    onOpenSession: () -> Unit,
    onSettings: () -> Unit,
) {
    Scaffold { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.headlineMedium
                )
                IconButton(onClick = onSettings) {
                    Icon(
                        painter = painterResource(R.drawable.ic_settings),
                        contentDescription = stringResource(R.string.home_settings),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            state.lastSession?.let { session ->
                LastSessionCard(session = session, onContinue = { onContinueLastSession(session.id) })
            }
            OutlinedButton(
                onClick = onNewSession,
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(width = 2.dp, color = MaterialTheme.colorScheme.primary),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = null,
                    modifier = Modifier.padding(end = 12.dp).size(24.dp)
                )
                Text(text = stringResource(R.string.home_new_session), style = MaterialTheme.typography.labelLarge)
            }
            Button(
                onClick = onOpenSession,
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = MaterialTheme.shapes.medium,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_lock),
                    contentDescription = null,
                    modifier = Modifier.padding(end = 12.dp).size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(text = stringResource(R.string.home_open_session), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun LastSessionCard(
    session: Session,
    onContinue: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.home_last_session),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = session.date.toDisplayText(),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            RouteText(
                departure = session.departure,
                destination = session.destination,
                style = CheckPilotTheme.monoTypography.large
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                session.cm1StaffNumber?.let { CrewMemberStaffNumber(slot = CrewMemberSlot.CM1, staffNumber = it) }
                session.cm2StaffNumber?.let { CrewMemberStaffNumber(slot = CrewMemberSlot.CM2, staffNumber = it) }
            }
            PrimaryButton(text = stringResource(R.string.home_continue_last_session), onClick = onContinue)
        }
    }
}

@Composable
private fun CrewMemberStaffNumber(
    slot: CrewMemberSlot,
    staffNumber: StaffNumber,
) {
    Row(
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
