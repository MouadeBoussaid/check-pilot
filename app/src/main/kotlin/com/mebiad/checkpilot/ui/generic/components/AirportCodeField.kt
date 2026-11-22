package com.mebiad.checkpilot.ui.generic.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.mebiad.checkpilot.ui.generic.theme.CheckPilotTheme

/**
 * An airport code, typed as is. The keyboard offers capitals, but the domain does the uppercasing and says whether the
 * code is valid, so the field neither filters nor cuts what the Assessor types.
 */
@Composable
fun AirportCodeField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    imeAction: ImeAction = ImeAction.Next,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        textStyle = CheckPilotTheme.monoTypography.large,
        label = { Text(text = label) },
        singleLine = true,
        keyboardOptions =
            KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Ascii,
                imeAction = imeAction
            ),
        shape = MaterialTheme.shapes.medium,
        colors = checkPilotFieldColors()
    )
}
