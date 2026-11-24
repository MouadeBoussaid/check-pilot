package com.mebiad.checkpilot.ui.apps.pin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mebiad.checkpilot.domain.pin.Pin
import com.mebiad.checkpilot.domain.pin.PinCheck
import com.mebiad.checkpilot.domain.pin.PinUsecase
import com.mebiad.checkpilot.domain.pin.SetPinResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PinStep { NewPin, Again, Enter }

enum class PinMessage { NotFourToSixDigits, Mismatch, Wrong }

data class PinUiState(
    /** Null until it's known whether a PIN is set. */
    val step: PinStep? = null,
    val digitCount: Int = 0,
    val message: PinMessage? = null,
)

private data class Entry(
    val digits: String = "",
    /** The new PIN typed the first time, once it has 4 to 6 digits. */
    val firstPin: Pin? = null,
    val message: PinMessage? = null,
)

class PinViewModel(
    private val pin: PinUsecase,
) : ViewModel() {
    private val entry = MutableStateFlow(Entry())

    val state: StateFlow<PinUiState> =
        combine(pin.isSet, entry) { isSet, entry ->
            PinUiState(
                step =
                    when {
                        isSet -> PinStep.Enter
                        entry.firstPin != null -> PinStep.Again
                        else -> PinStep.NewPin
                    },
                digitCount = entry.digits.length,
                message = entry.message
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), PinUiState())

    /** The PIN being set or checked; the keypad does nothing until it's done. */
    private var checking: Job? = null
    private val isChecking: Boolean get() = checking?.isActive == true

    fun onDigit(digit: Char) = edit { copy(digits = digits + digit, message = null) }

    fun onBackspace() = edit { copy(digits = digits.dropLast(1)) }

    fun onStartOver() = edit { Entry() }

    fun onConfirm() {
        if (isChecking) return
        val typed = entry.value
        when (state.value.step) {
            null -> {
                Unit
            }

            PinStep.NewPin -> {
                val newPin = Pin.parse(typed.digits)
                entry.value =
                    if (newPin == null) Entry(message = PinMessage.NotFourToSixDigits) else Entry(firstPin = newPin)
            }

            PinStep.Again -> {
                checking =
                    viewModelScope.launch {
                        val firstPin = checkNotNull(typed.firstPin)
                        entry.value =
                            when (pin.set(firstPin, again = typed.digits)) {
                                SetPinResult.Set -> Entry()
                                SetPinResult.Mismatch -> Entry(firstPin = firstPin, message = PinMessage.Mismatch)
                            }
                    }
            }

            PinStep.Enter -> {
                checking =
                    viewModelScope.launch {
                        entry.value =
                            when (pin.check(typed.digits)) {
                                PinCheck.Correct -> Entry()
                                PinCheck.Wrong -> Entry(message = PinMessage.Wrong)
                            }
                    }
            }
        }
    }

    private fun edit(change: Entry.() -> Entry) {
        if (!isChecking) entry.update(change)
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5000L
    }
}
