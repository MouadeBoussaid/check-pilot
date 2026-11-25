package com.mebiad.checkpilot.ui.apps.pin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mebiad.checkpilot.domain.pin.ChangePinResult
import com.mebiad.checkpilot.domain.pin.Pin
import com.mebiad.checkpilot.domain.pin.PinCheck
import com.mebiad.checkpilot.domain.pin.PinUsecase
import com.mebiad.checkpilot.domain.pin.SetPinResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Unlock opens the Session list; Change replaces the PIN from Settings. */
enum class PinMode { Unlock, Change }

enum class PinStep { Current, NewPin, Again, Enter }

enum class PinMessage { NotFourToSixDigits, Mismatch, Wrong }

sealed interface PinEvent {
    /** The PIN was changed from Settings. */
    data object Changed : PinEvent
}

data class PinUiState(
    val mode: PinMode = PinMode.Unlock,
    /** Null until it's known whether a PIN is set. */
    val step: PinStep? = null,
    val digitCount: Int = 0,
    val message: PinMessage? = null,
    /** The seconds left before a PIN can be entered again; null while it can. */
    val lockoutSecondsLeft: Int? = null,
    /** The Forgot PIN warning shows, saying that every Session will be deleted. */
    val forgotPinWarning: Boolean = false,
)

private data class Entry(
    val digits: String = "",
    /** The current PIN, once [PinUsecase.verify] accepted it. */
    val current: String? = null,
    /** The new PIN typed the first time, once it has 4 to 6 digits. */
    val firstPin: Pin? = null,
    val message: PinMessage? = null,
)

class PinViewModel(
    private val pin: PinUsecase,
    private val mode: PinMode,
) : ViewModel() {
    private val entry = MutableStateFlow(Entry())
    private val forgotPinWarning = MutableStateFlow(false)

    private val mutableEvents = MutableSharedFlow<PinEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<PinEvent> = mutableEvents.asSharedFlow()

    val state: StateFlow<PinUiState> =
        combine(pin.isSet, entry, pin.lockoutSecondsLeft, forgotPinWarning) { isSet, entry, secondsLeft, warning ->
            val step =
                when {
                    isSet && mode == PinMode.Unlock -> PinStep.Enter
                    isSet && entry.current == null -> PinStep.Current
                    entry.firstPin != null -> PinStep.Again
                    else -> PinStep.NewPin
                }
            PinUiState(
                mode = mode,
                step = step,
                digitCount = entry.digits.length,
                message = entry.message,
                lockoutSecondsLeft = secondsLeft.takeIf { step == PinStep.Enter || step == PinStep.Current },
                forgotPinWarning = warning
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), PinUiState(mode = mode))

    /** The PIN being set or checked; the keypad does nothing until it's done. */
    private var checking: Job? = null
    private val isChecking: Boolean get() = checking?.isActive == true

    private val isLockedOut: Boolean get() = state.value.lockoutSecondsLeft != null

    fun onDigit(digit: Char) = edit { copy(digits = digits + digit, message = null) }

    fun onBackspace() = edit { copy(digits = digits.dropLast(1)) }

    fun onStartOver() = edit { Entry() }

    fun onForgotPin() {
        forgotPinWarning.value = true
    }

    fun onConfirmForgotPin() {
        forgotPinWarning.value = false
        entry.value = Entry()
        checking = viewModelScope.launch { pin.forget() }
    }

    fun onCancelForgotPin() {
        forgotPinWarning.value = false
    }

    fun onConfirm() {
        if (isChecking || isLockedOut) return
        val typed = entry.value
        when (state.value.step) {
            null -> {
                Unit
            }

            PinStep.Current -> {
                checking =
                    viewModelScope.launch {
                        entry.value =
                            when (pin.verify(typed.digits)) {
                                PinCheck.Correct -> Entry(current = typed.digits)
                                PinCheck.Wrong -> Entry(message = PinMessage.Wrong)
                                PinCheck.LockedOut -> Entry()
                            }
                    }
            }

            PinStep.NewPin -> {
                val newPin = Pin.parse(typed.digits)
                entry.value =
                    if (newPin == null) {
                        Entry(current = typed.current, message = PinMessage.NotFourToSixDigits)
                    } else {
                        Entry(current = typed.current, firstPin = newPin)
                    }
            }

            PinStep.Again -> {
                checking = viewModelScope.launch { if (mode == PinMode.Unlock) setPin(typed) else changePin(typed) }
            }

            PinStep.Enter -> {
                checking =
                    viewModelScope.launch {
                        entry.value =
                            when (pin.check(typed.digits)) {
                                PinCheck.Correct -> Entry()
                                PinCheck.Wrong -> Entry(message = PinMessage.Wrong)
                                PinCheck.LockedOut -> Entry()
                            }
                    }
            }
        }
    }

    private suspend fun setPin(typed: Entry) {
        val firstPin = checkNotNull(typed.firstPin)
        entry.value =
            when (pin.set(firstPin, again = typed.digits)) {
                SetPinResult.Set -> Entry()
                SetPinResult.Mismatch -> Entry(firstPin = firstPin, message = PinMessage.Mismatch)
            }
    }

    private suspend fun changePin(typed: Entry) {
        val firstPin = checkNotNull(typed.firstPin)
        when (pin.change(current = typed.current.orEmpty(), newPin = firstPin, again = typed.digits)) {
            ChangePinResult.Changed -> {
                entry.value = typed.copy(digits = "")
                mutableEvents.emit(PinEvent.Changed)
            }

            ChangePinResult.Mismatch -> {
                entry.value = typed.copy(digits = "", message = PinMessage.Mismatch)
            }

            ChangePinResult.Wrong, ChangePinResult.LockedOut -> {
                entry.value = Entry(message = PinMessage.Wrong)
            }
        }
    }

    private fun edit(change: Entry.() -> Entry) {
        if (!isChecking && !isLockedOut) entry.update(change)
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5000L
    }
}
