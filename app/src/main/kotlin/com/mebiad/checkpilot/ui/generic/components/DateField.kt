package com.mebiad.checkpilot.ui.generic.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mebiad.checkpilot.R
import com.mebiad.checkpilot.ui.generic.format.toDisplayText
import com.mebiad.checkpilot.ui.generic.format.toDisplayTextWithDay
import com.mebiad.checkpilot.ui.generic.theme.CheckPilotTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Shows [date] and opens a date picker on tap; [onDateChange] gets the date picked with OK. */
@Composable
fun DateField(
    date: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    var picking by rememberSaveable { mutableStateOf(false) }
    Surface(
        onClick = { picking = true },
        modifier = modifier.fillMaxWidth().heightIn(min = 64.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = date.toDisplayText(), style = MaterialTheme.typography.bodyLarge)
            Icon(
                painter = painterResource(R.drawable.ic_calendar),
                contentDescription = stringResource(R.string.setup_change_date),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    if (picking) {
        PickDateDialog(
            initial = date,
            onPick = { picked ->
                picking = false
                onDateChange(picked)
            },
            onDismiss = { picking = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PickDateDialog(
    initial: LocalDate,
    onPick: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberDatePickerState(initialSelectedDateMillis = initial.toUtcMillis())
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = { onPick(state.selectedDateMillis?.toUtcLocalDate() ?: initial) }) {
                Text(text = stringResource(R.string.date_picker_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(text = stringResource(R.string.date_picker_cancel)) }
        },
        shape = MaterialTheme.shapes.extraLarge
    ) {
        DatePicker(
            state = state,
            title = {
                Text(
                    text = stringResource(R.string.date_picker_title),
                    modifier = Modifier.padding(start = 24.dp, top = 16.dp),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            headline = {
                Text(
                    text = (state.selectedDateMillis?.toUtcLocalDate() ?: initial).toDisplayTextWithDay(),
                    modifier = Modifier.padding(start = 24.dp, bottom = 12.dp),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            showModeToggle = false,
            colors = datePickerColors()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun datePickerColors() =
    DatePickerDefaults.colors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        todayContentColor = MaterialTheme.colorScheme.primary,
        todayDateBorderColor = MaterialTheme.colorScheme.primary,
        dividerColor = CheckPilotTheme.colors.hint
    )

// The date picker works in UTC midnight milliseconds, whatever the phone's time zone.
private fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toUtcLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
