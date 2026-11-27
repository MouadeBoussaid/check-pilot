package com.mebiad.checkpilot.domain.pin

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

/** Holds the PIN in plain digits; [pin] null means none is set. */
class PinUsecaseFake : PinUsecase {
    private val pinState = MutableStateFlow<String?>(null)
    private val unlockedState = MutableStateFlow(false)

    var pin: String?
        get() = pinState.value
        set(value) {
            pinState.value = value
        }

    /** Each entry checked, in order. */
    val checks = mutableListOf<String>()

    /** While set and not completed, every set and check waits on it, as a slow hash would. */
    var hold: CompletableDeferred<Unit>? = null

    override val isSet: Flow<Boolean> = pinState.map { it != null }

    override val unlocked: StateFlow<Boolean> = unlockedState

    val secondsLeft = MutableStateFlow<Int?>(null)

    override val lockoutSecondsLeft: Flow<Int?> = secondsLeft

    override suspend fun set(
        pin: Pin,
        again: String,
    ): SetPinResult {
        hold?.await()
        if (again != pin.digits) return SetPinResult.Mismatch
        this.pin = pin.digits
        unlockedState.value = true
        return SetPinResult.Set
    }

    override suspend fun check(entry: String): PinCheck {
        checks += entry
        hold?.await()
        if (entry != pin) return PinCheck.Wrong
        unlockedState.value = true
        return PinCheck.Correct
    }

    /** Each entry verified, in order. */
    val verifications = mutableListOf<String>()

    override suspend fun verify(entry: String): PinCheck {
        verifications += entry
        hold?.await()
        if (secondsLeft.value != null) return PinCheck.LockedOut
        return if (entry == pin) PinCheck.Correct else PinCheck.Wrong
    }

    override suspend fun change(
        current: String,
        newPin: Pin,
        again: String,
    ): ChangePinResult {
        hold?.await()
        if (again != newPin.digits) return ChangePinResult.Mismatch
        if (pin != null && current != pin) return ChangePinResult.Wrong
        pin = newPin.digits
        return ChangePinResult.Changed
    }

    /** How many times the PIN was forgotten. */
    var forgetCount: Int = 0

    override suspend fun forget() {
        forgetCount++
        hold?.await()
        pin = null
        secondsLeft.value = null
        lock()
    }

    override fun lock() {
        unlockedState.value = false
    }

    fun reset() {
        pin = null
        unlockedState.value = false
        checks.clear()
        verifications.clear()
        forgetCount = 0
        secondsLeft.value = null
        hold = null
    }
}
