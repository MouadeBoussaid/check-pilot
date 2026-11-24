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

    override fun lock() {
        unlockedState.value = false
    }

    fun reset() {
        pin = null
        unlockedState.value = false
        checks.clear()
        hold = null
    }
}
