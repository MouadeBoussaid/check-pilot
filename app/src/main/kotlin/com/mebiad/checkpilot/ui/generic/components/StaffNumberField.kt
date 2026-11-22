package com.mebiad.checkpilot.ui.generic.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mebiad.checkpilot.R
import com.mebiad.checkpilot.ui.generic.theme.CheckPilotTheme

/**
 * A Crew Member's Staff Number. It keeps digits only, and sets no maximum length: the domain says how many digits a
 * Staff Number has, and refuses one that is too long with a message instead of silently cutting it.
 */
@Composable
fun StaffNumberField(
    slot: CrewMemberSlot,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val mono = CheckPilotTheme.monoTypography.medium
    OutlinedTextField(
        value = value,
        onValueChange = { text -> onValueChange(text.filter(Char::isDigit)) },
        modifier = modifier.fillMaxWidth(),
        textStyle = mono,
        placeholder = { Text(text = stringResource(R.string.setup_staff_number_placeholder), style = mono) },
        leadingIcon = {
            Row(
                modifier = Modifier.padding(start = 16.dp, end = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CrewMemberDot(slot = slot)
                Text(text = stringResource(slot.label), style = MaterialTheme.typography.titleSmall)
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
        shape = MaterialTheme.shapes.medium,
        colors = checkPilotFieldColors()
    )
}
