package com.mebiad.checkpilot.ui.generic.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import com.mebiad.checkpilot.ui.generic.theme.CheckPilotTheme

/** Fields sit on the card colour, with an amber border while focused. */
@Composable
fun checkPilotFieldColors(): TextFieldColors {
    val scheme = MaterialTheme.colorScheme
    return OutlinedTextFieldDefaults.colors(
        focusedContainerColor = scheme.surfaceContainerLow,
        unfocusedContainerColor = scheme.surfaceContainerLow,
        errorContainerColor = scheme.surfaceContainerLow,
        focusedBorderColor = scheme.primary,
        unfocusedBorderColor = scheme.outlineVariant,
        focusedLabelColor = scheme.onSurfaceVariant,
        unfocusedLabelColor = scheme.onSurfaceVariant,
        focusedPlaceholderColor = CheckPilotTheme.colors.hint,
        unfocusedPlaceholderColor = CheckPilotTheme.colors.hint
    )
}
