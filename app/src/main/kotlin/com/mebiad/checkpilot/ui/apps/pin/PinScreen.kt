package com.mebiad.checkpilot.ui.apps.pin

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mebiad.checkpilot.R
import com.mebiad.checkpilot.ui.generic.components.BackTopBar
import com.mebiad.checkpilot.ui.generic.components.SecondaryButton
import com.mebiad.checkpilot.ui.generic.theme.CheckPilotTheme

@Composable
fun PinScreen(
    viewModel: PinViewModel,
    onBack: () -> Unit,
    onPinChanged: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                PinEvent.Changed -> onPinChanged()
            }
        }
    }
    PinContent(
        state = state,
        onBack = onBack,
        onDigit = viewModel::onDigit,
        onBackspace = viewModel::onBackspace,
        onConfirm = viewModel::onConfirm,
        onStartOver = viewModel::onStartOver,
        onForgotPin = viewModel::onForgotPin
    )
    if (state.forgotPinWarning) {
        ForgotPinDialog(onDelete = viewModel::onConfirmForgotPin, onCancel = viewModel::onCancelForgotPin)
    }
}

@Composable
private fun PinContent(
    state: PinUiState,
    onBack: () -> Unit,
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    onConfirm: () -> Unit,
    onStartOver: () -> Unit,
    onForgotPin: () -> Unit,
) {
    Scaffold(topBar = { BackTopBar(title = "", onBack = onBack) }) { innerPadding ->
        val step = state.step ?: return@Scaffold
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_lock),
                contentDescription = null,
                modifier = Modifier.padding(top = 16.dp).size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            PinHeading(mode = state.mode, step = step)
            Spacer(modifier = Modifier.height(24.dp))
            PinDots(digitCount = state.digitCount, isError = state.message != null)
            Spacer(modifier = Modifier.height(24.dp))
            val secondsLeft = state.lockoutSecondsLeft
            if (secondsLeft == null) {
                PinMessages(step = step, message = state.message)
            } else {
                LockoutMessage(secondsLeft = secondsLeft)
            }
            Spacer(modifier = Modifier.weight(1f))
            Keypad(
                enabled = secondsLeft == null,
                onDigit = onDigit,
                onBackspace = onBackspace,
                onConfirm = onConfirm
            )
            Box(modifier = Modifier.heightIn(min = 72.dp), contentAlignment = Alignment.Center) {
                when {
                    step == PinStep.Again -> {
                        LinkButton(text = R.string.pin_start_over, onClick = onStartOver)
                    }

                    step == PinStep.Enter && state.mode == PinMode.Unlock -> {
                        LinkButton(text = R.string.pin_forgot, onClick = onForgotPin)
                    }
                }
            }
        }
    }
}

@Composable
private fun LinkButton(
    @StringRes text: Int,
    onClick: () -> Unit,
) {
    TextButton(onClick = onClick) {
        Text(text = stringResource(text), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun PinHeading(
    mode: PinMode,
    step: PinStep,
) {
    val title =
        when {
            mode == PinMode.Change -> R.string.pin_change_title
            step == PinStep.Enter -> R.string.pin_enter_title
            else -> R.string.pin_set_title
        }
    Text(text = stringResource(title), style = MaterialTheme.typography.headlineLarge)
    val subtitle =
        when (step) {
            PinStep.NewPin -> R.string.pin_new_subtitle
            PinStep.Again -> R.string.pin_again_subtitle
            PinStep.Current -> R.string.pin_current_subtitle
            PinStep.Enter -> null
        }
    subtitle?.let {
        Text(
            text = stringResource(it),
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
    if (step != PinStep.Enter) {
        Row(
            modifier = Modifier.padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StepChip(
                text = stringResource(R.string.pin_step_new),
                current = step == PinStep.NewPin,
                done =
                    step == PinStep.Again
            )
            StepChip(text = stringResource(R.string.pin_step_confirm), current = step == PinStep.Again, done = false)
        }
    }
}

@Composable
private fun StepChip(
    text: String,
    current: Boolean,
    done: Boolean,
) {
    Surface(
        shape = RoundedCornerShape(50),
        color =
            if (current) {
                MaterialTheme.colorScheme.surfaceContainerHighest
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (done) {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = if (current) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** One dot per digit typed, over at least [MIN_SLOTS] slots; the digits themselves never show. */
@Composable
private fun PinDots(
    digitCount: Int,
    isError: Boolean,
) {
    val description = pluralStringResource(R.plurals.pin_digit_count, digitCount, digitCount)
    val filled = MaterialTheme.colorScheme.primary
    val empty = if (isError) MaterialTheme.colorScheme.error else CheckPilotTheme.colors.hint
    Row(
        modifier = Modifier.clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        repeat(maxOf(MIN_SLOTS, digitCount)) { index ->
            PinDot(typed = index < digitCount, filled = filled, empty = empty)
        }
    }
}

@Composable
private fun PinDot(
    typed: Boolean,
    filled: Color,
    empty: Color,
) {
    Canvas(modifier = Modifier.size(16.dp)) {
        if (typed) {
            drawCircle(color = filled)
        } else {
            val width = 2.dp.toPx()
            drawCircle(
                color = empty,
                radius = (size.minDimension - width) / 2,
                style = Stroke(width = width, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f)))
            )
        }
    }
}

@Composable
private fun PinMessages(
    step: PinStep,
    message: PinMessage?,
) {
    Column(
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val error =
            when (message) {
                PinMessage.Mismatch -> R.string.pin_mismatch
                PinMessage.Wrong -> R.string.pin_wrong
                PinMessage.NotFourToSixDigits, null -> null
            }
        error?.let { MessageText(text = stringResource(it), color = MaterialTheme.colorScheme.error) }
        if (step == PinStep.NewPin || step == PinStep.Again) {
            val hintColor =
                if (message == PinMessage.NotFourToSixDigits) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            MessageText(text = stringResource(R.string.pin_hint_4_to_6_digits), color = hintColor)
        }
    }
}

/** The lockout's reason and countdown, in one text so it's read out whole. */
@Composable
private fun LockoutMessage(secondsLeft: Int) {
    val reason = stringResource(R.string.pin_lockout_reason)
    val wait = pluralStringResource(R.plurals.pin_lockout_wait, secondsLeft, secondsLeft)
    val error = MaterialTheme.colorScheme.error
    Surface(
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_schedule),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = error
            )
            Text(
                text =
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = error)) { append(reason) }
                        append("\n")
                        append(wait)
                    },
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun ForgotPinDialog(
    onDelete: () -> Unit,
    onCancel: () -> Unit,
) {
    Dialog(onDismissRequest = onCancel) {
        Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainer) {
            Column(modifier = Modifier.padding(24.dp)) {
                Icon(
                    painter = painterResource(R.drawable.ic_warning),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    text = stringResource(R.string.pin_forgot_title),
                    modifier = Modifier.padding(top = 16.dp),
                    style = MaterialTheme.typography.headlineSmall
                )
                val body = stringResource(R.string.pin_forgot_body)
                val bold = stringResource(R.string.pin_forgot_body_bold)
                Text(
                    text =
                        buildAnnotatedString {
                            append(body)
                            val start = body.indexOf(bold)
                            if (start >= 0) {
                                addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, start + bold.length)
                            }
                        },
                    modifier = Modifier.padding(top = 12.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = stringResource(R.string.pin_forgot_hint),
                    modifier = Modifier.padding(top = 12.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = onDelete,
                    modifier = Modifier.padding(top = 24.dp).fillMaxWidth().height(56.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                ) {
                    Text(text = stringResource(R.string.pin_forgot_delete), style = MaterialTheme.typography.labelLarge)
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

@Composable
private fun MessageText(
    text: String,
    color: Color,
) {
    Text(text = text, style = MaterialTheme.typography.bodyLarge, color = color, textAlign = TextAlign.Center)
}

@Composable
private fun Keypad(
    enabled: Boolean,
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    onConfirm: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        listOf("123", "456", "789").forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { digit ->
                    DigitKey(digit = digit, enabled = enabled, onDigit = onDigit, modifier = Modifier.weight(1f))
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconKey(
                icon = R.drawable.ic_backspace,
                description = stringResource(R.string.pin_remove_last_digit),
                onClick = onBackspace,
                enabled = enabled,
                container = Color.Transparent,
                content = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            DigitKey(digit = '0', enabled = enabled, onDigit = onDigit, modifier = Modifier.weight(1f))
            IconKey(
                icon = R.drawable.ic_check,
                description = stringResource(R.string.pin_confirm),
                onClick = onConfirm,
                enabled = enabled,
                container = MaterialTheme.colorScheme.primary,
                content = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DigitKey(
    digit: Char,
    enabled: Boolean,
    onDigit: (Char) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = { onDigit(digit) },
        modifier = modifier.height(KEY_HEIGHT).alpha(if (enabled) 1f else DISABLED_ALPHA),
        enabled = enabled,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = digit.toString(), style = MaterialTheme.typography.headlineLarge)
        }
    }
}

@Composable
private fun IconKey(
    @DrawableRes icon: Int,
    description: String,
    onClick: () -> Unit,
    enabled: Boolean,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(KEY_HEIGHT).alpha(if (enabled) 1f else DISABLED_ALPHA),
        enabled = enabled,
        shape = CircleShape,
        color = container,
        contentColor = content
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(painter = painterResource(icon), contentDescription = description, modifier = Modifier.size(28.dp))
        }
    }
}

private const val MIN_SLOTS = 6
private val KEY_HEIGHT = 64.dp
private const val DISABLED_ALPHA = 0.38f
