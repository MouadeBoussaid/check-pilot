package com.mebiad.checkpilot.ui.generic.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.mebiad.checkpilot.R
import com.mebiad.checkpilot.domain.session.AirportCode

/** A route like "DOH → SLL" as one text, so it reads as one phrase, with the arrow dimmed. */
@Composable
fun RouteText(
    departure: AirportCode,
    destination: AirportCode,
    style: TextStyle,
    modifier: Modifier = Modifier,
) {
    val arrow = stringResource(R.string.route_arrow)
    val arrowColor = MaterialTheme.colorScheme.onSurfaceVariant
    Text(
        text =
            buildAnnotatedString {
                append(departure.letters)
                append(" ")
                withStyle(SpanStyle(color = arrowColor)) { append(arrow) }
                append(" ")
                append(destination.letters)
            },
        modifier = modifier,
        style = style
    )
}
