package com.mebiad.checkpilot.domain.pin

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

enum class SetPinResult { Set, Mismatch }

enum class PinCheck { Correct, Wrong, LockedOut }

enum class ChangePinResult { Changed, Wrong, Mismatch, LockedOut }

/**
 * The App PIN in front of the Session list. The unlock lives in memory only: it ends with [lock], and never survives a
 * restart.
 */
interface PinUsecase {
    /** Emits whether a PIN has been set. */
    val isSet: Flow<Boolean>

    /**
     * Emits the seconds left in the lockout, rounded up, as they change, and null while there is no lockout. It reads
     * the time on the collector's dispatcher, at least every second.
     */
    val lockoutSecondsLeft: Flow<Int?>

    /** Whether the Session list is unlocked. */
    val unlocked: StateFlow<Boolean>

    /** Sets [pin] as the App PIN if [again], its second typing, matches; setting it unlocks the Session list. */
    suspend fun set(
        pin: Pin,
        again: String,
    ): SetPinResult

    /**
     * Checks [entry] against the App PIN; the correct one unlocks the Session list. The fifth wrong PIN in a row starts
     * a lockout of 30 seconds, during which no entry is checked.
     */
    suspend fun check(entry: String): PinCheck

    /** Checks [entry] against the App PIN as [check] does, wrong PINs and lockout included, but never unlocks. */
    suspend fun verify(entry: String): PinCheck

    /**
     * Replaces the App PIN with [newPin] if [again], its second typing, matches and [current] passes [verify]. With no
     * PIN set, [current] is ignored. It never unlocks the Session list.
     */
    suspend fun change(
        current: String,
        newPin: Pin,
        again: String,
    ): ChangePinResult

    /**
     * Forgot PIN: deletes every Session, then forgets the App PIN, its wrong attempts and its lockout, and locks the
     * Session list. The Catalogue is kept.
     */
    suspend fun forget()

    fun lock()
}
