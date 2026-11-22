package com.mebiad.checkpilot.ui.apps.setup

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mebiad.checkpilot.R
import com.mebiad.checkpilot.domain.session.SetupRefusal
import com.mebiad.checkpilot.ui.generic.components.AirportCodeField
import com.mebiad.checkpilot.ui.generic.components.BackTopBar
import com.mebiad.checkpilot.ui.generic.components.CrewMemberSlot
import com.mebiad.checkpilot.ui.generic.components.DateField
import com.mebiad.checkpilot.ui.generic.components.ErrorMessage
import com.mebiad.checkpilot.ui.generic.components.PrimaryButton
import com.mebiad.checkpilot.ui.generic.components.StaffNumberField
import java.time.LocalDate

@Composable
fun SetupScreen(
    viewModel: SetupViewModel,
    onBack: () -> Unit,
    onSessionStarted: (sessionId: String) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is SetupEvent.SessionStarted -> onSessionStarted(event.sessionId)
            }
        }
    }
    SetupContent(
        state = state,
        onBack = onBack,
        onCm1StaffNumberChange = viewModel::onCm1StaffNumberChange,
        onCm2StaffNumberChange = viewModel::onCm2StaffNumberChange,
        onDepartureChange = viewModel::onDepartureChange,
        onDestinationChange = viewModel::onDestinationChange,
        onDateChange = viewModel::onDateChange,
        onStart = viewModel::onStart
    )
}

@Composable
private fun SetupContent(
    state: SetupUiState,
    onBack: () -> Unit,
    onCm1StaffNumberChange: (String) -> Unit,
    onCm2StaffNumberChange: (String) -> Unit,
    onDepartureChange: (String) -> Unit,
    onDestinationChange: (String) -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onStart: () -> Unit,
) {
    Scaffold(
        topBar = { BackTopBar(title = stringResource(R.string.setup_title), onBack = onBack) },
        bottomBar = {
            PrimaryButton(
                text = stringResource(R.string.setup_start),
                onClick = onStart,
                modifier =
                    Modifier
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionTitle(R.string.setup_crew_members)
            StaffNumberField(
                slot = CrewMemberSlot.CM1,
                value = state.cm1StaffNumber,
                onValueChange = onCm1StaffNumberChange
            )
            StaffNumberField(
                slot = CrewMemberSlot.CM2,
                value = state.cm2StaffNumber,
                onValueChange = onCm2StaffNumberChange
            )
            val staffNumberRefusal = state.refusal?.takeIf { it.isAboutStaffNumbers() }
            if (staffNumberRefusal != null) {
                ErrorMessage(text = stringResource(staffNumberRefusal.message()))
            } else {
                Text(
                    text = stringResource(R.string.setup_staff_number_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SectionTitle(R.string.setup_route)
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AirportCodeField(
                    label = stringResource(R.string.setup_from),
                    value = state.departure,
                    onValueChange = onDepartureChange,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_forward),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                AirportCodeField(
                    label = stringResource(R.string.setup_to),
                    value = state.destination,
                    onValueChange = onDestinationChange,
                    modifier = Modifier.weight(1f),
                    imeAction = ImeAction.Done
                )
            }
            state.refusal?.takeUnless { it.isAboutStaffNumbers() }?.let { refusal ->
                ErrorMessage(text = stringResource(refusal.message()))
            }

            SectionTitle(R.string.setup_date)
            DateField(date = state.date, onDateChange = onDateChange)
        }
    }
}

@Composable
private fun SectionTitle(
    @StringRes text: Int,
) {
    Text(
        text = stringResource(text),
        modifier = Modifier.padding(top = 12.dp),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

private fun SetupRefusal.isAboutStaffNumbers(): Boolean =
    this == SetupRefusal.NO_STAFF_NUMBER || this == SetupRefusal.STAFF_NUMBER_NOT_4_TO_6_DIGITS

@StringRes
private fun SetupRefusal.message(): Int =
    when (this) {
        SetupRefusal.NO_STAFF_NUMBER -> R.string.setup_no_staff_number
        SetupRefusal.STAFF_NUMBER_NOT_4_TO_6_DIGITS -> R.string.setup_staff_number_not_4_to_6_digits
        SetupRefusal.AIRPORT_CODE_MISSING -> R.string.setup_airport_code_missing
        SetupRefusal.AIRPORT_CODE_NOT_3_LETTERS -> R.string.setup_airport_code_not_3_letters
    }
