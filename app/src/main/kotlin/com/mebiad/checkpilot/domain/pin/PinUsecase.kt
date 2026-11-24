package com.mebiad.checkpilot.domain.pin

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

enum class SetPinResult { Set, Mismatch }

enum class PinCheck { Correct, Wrong }

/**
 * The App PIN in front of the Session list. The unlock lives in memory only: it ends with [lock], and never survives a
 * restart.
 */
interface PinUsecase {
    /** Emits whether a PIN has been set. */
    val isSet: Flow<Boolean>

    /** Whether the Session list is unlocked. */
    val unlocked: StateFlow<Boolean>

    /** Sets [pin] as the App PIN if [again], its second typing, matches; setting it unlocks the Session list. */
    suspend fun set(
        pin: Pin,
        again: String,
    ): SetPinResult

    /** Checks [entry] against the App PIN; the correct one unlocks the Session list. */
    suspend fun check(entry: String): PinCheck

    fun lock()
}
