package com.mebiad.checkpilot.ui.generic.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** The screen's main action: full width, 64 dp high, amber. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(64.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}
