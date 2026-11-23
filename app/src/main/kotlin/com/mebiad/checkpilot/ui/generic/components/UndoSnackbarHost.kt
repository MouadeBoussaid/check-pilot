package com.mebiad.checkpilot.ui.generic.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** The light snackbar at the bottom of a screen that offers to undo the last change. */
@Composable
fun UndoSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    SnackbarHost(hostState = hostState, modifier = modifier) { data ->
        Snackbar(snackbarData = data, shape = MaterialTheme.shapes.large)
    }
}

/** Shows [message] with Undo, and returns true if the Assessor tapped Undo before it went away. */
suspend fun SnackbarHostState.showUndo(
    message: String,
    undoLabel: String,
): Boolean =
    showSnackbar(message = message, actionLabel = undoLabel, duration = SnackbarDuration.Long) ==
        SnackbarResult.ActionPerformed
