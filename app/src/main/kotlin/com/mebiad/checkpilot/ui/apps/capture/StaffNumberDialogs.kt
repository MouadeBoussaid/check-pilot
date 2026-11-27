package com.mebiad.checkpilot.ui.apps.capture

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mebiad.checkpilot.R
import com.mebiad.checkpilot.ui.generic.components.CrewMemberDot
import com.mebiad.checkpilot.ui.generic.components.ErrorMessage
import com.mebiad.checkpilot.ui.generic.components.SecondaryButton
import com.mebiad.checkpilot.ui.generic.components.StaffNumberField
import com.mebiad.checkpilot.ui.generic.components.slot
import com.mebiad.checkpilot.ui.generic.theme.CheckPilotTheme

/** The pop-up [state] has open, if any: editing a Staff Number, or confirming the removal of a Crew Member. */
@Composable
fun StaffNumberDialogs(
    state: StaffNumbersUiState,
    onTextChange: (String) -> Unit,
    onSave: () -> Unit,
    onRemove: () -> Unit,
    onConfirmRemoval: () -> Unit,
    onDismiss: () -> Unit,
) {
    state.editing?.let { editing ->
        EditStaffNumberDialog(
            editing = editing,
            onTextChange = onTextChange,
            onSave = onSave,
            onRemove = onRemove,
            onDismiss = onDismiss
        )
    }
    state.confirming?.let { removal ->
        RemoveCrewMemberDialog(removal = removal, onConfirm = onConfirmRemoval, onCancel = onDismiss)
    }
}

@Composable
private fun EditStaffNumberDialog(
    editing: StaffNumberEdit,
    onTextChange: (String) -> Unit,
    onSave: () -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    val slot = editing.crewMember.slot
    val crewMemberLabel = stringResource(slot.label)
    // With the platform's default width, a text field in a Dialog never lets Robolectric go idle on the acceptance
    // tests' tall screens, so the dialog sets its own width.
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.padding(horizontal = 24.dp).widthIn(max = 560.dp).fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text =
                        stringResource(
                            if (editing.isNew) R.string.capture_add_staff_number else R.string.capture_edit_staff_number
                        ),
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text =
                        stringResource(
                            if (editing.isNew) {
                                R.string.capture_add_staff_number_hint
                            } else {
                                R.string.capture_edit_staff_number_hint
                            }
                        ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
                )
                StaffNumberField(slot = slot, value = editing.text, onValueChange = onTextChange)
                if (!editing.isNew) {
                    OutlinedButton(
                        onClick = onRemove,
                        modifier = Modifier.padding(top = 16.dp).fillMaxWidth().heightIn(min = 56.dp),
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.outlineVariant),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_delete),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = stringResource(R.string.capture_remove_crew_member, crewMemberLabel),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
                editing.refusal?.let { refusal ->
                    Surface(
                        modifier = Modifier.padding(top = 12.dp).fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.error.copy(alpha = REFUSAL_BACKGROUND_ALPHA)
                    ) {
                        ErrorMessage(text = refusal.message(crewMemberLabel), modifier = Modifier.padding(16.dp))
                    }
                }
                Row(
                    modifier = Modifier.padding(top = 24.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 56.dp)) {
                        Text(text = stringResource(R.string.cancel), style = MaterialTheme.typography.labelLarge)
                    }
                    Button(
                        onClick = onSave,
                        modifier = Modifier.heightIn(min = 56.dp),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(text = stringResource(R.string.capture_save), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun StaffNumberRefusal.message(crewMemberLabel: String): String =
    when (this) {
        StaffNumberRefusal.NotFourToSixDigits -> {
            stringResource(R.string.setup_staff_number_not_4_to_6_digits)
        }

        StaffNumberRefusal.OnlyCrewMember -> {
            stringResource(R.string.setup_no_staff_number)
        }

        is StaffNumberRefusal.NotesTagged -> {
            pluralStringResource(R.plurals.capture_notes_tagged, count, count, crewMemberLabel)
        }
    }

@Composable
private fun RemoveCrewMemberDialog(
    removal: CrewMemberRemoval,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    val slot = removal.crewMember.slot
    val crewMemberLabel = stringResource(slot.label)
    Dialog(onDismissRequest = onCancel) {
        Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainer) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CrewMemberDot(slot = slot)
                    Text(
                        text = stringResource(R.string.capture_remove_crew_member_question, crewMemberLabel),
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
                Row(
                    modifier = Modifier.padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = removal.staffNumber.digits, style = CheckPilotTheme.monoTypography.extraSmall)
                    Text(
                        text =
                            stringResource(
                                R.string.capture_grades_progress,
                                removal.gradeCount,
                                removal.competencyCount
                            ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text =
                        pluralStringResource(
                            R.plurals.capture_grades_will_be_deleted,
                            removal.gradeCount,
                            removal.gradeCount,
                            crewMemberLabel
                        ),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 16.dp)
                )
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.padding(top = 24.dp).fillMaxWidth().height(56.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                ) {
                    Text(
                        text = stringResource(R.string.capture_remove_crew_member_and_grades, crewMemberLabel),
                        style = MaterialTheme.typography.labelLarge
                    )
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

private const val REFUSAL_BACKGROUND_ALPHA = 0.12f
