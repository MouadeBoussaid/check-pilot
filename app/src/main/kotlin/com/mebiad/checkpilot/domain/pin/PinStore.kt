package com.mebiad.checkpilot.domain.pin

import kotlinx.coroutines.flow.Flow
import java.time.Instant

/** The App PIN's hash, with the salt and the iteration count it was made with. */
class PinHash(
    val hash: ByteArray,
    val salt: ByteArray,
    val iterations: Int,
)

/** Keeps the App PIN's hash, the count of wrong PINs entered in a row and the lockout, across restarts. */
interface PinStore {
    /** Emits the stored hash, or null while no PIN is set. */
    fun observeHash(): Flow<PinHash?>

    suspend fun writeHash(pinHash: PinHash)

    suspend fun wrongAttempts(): Int

    suspend fun recordWrongAttempt()

    suspend fun resetWrongAttempts()

    /** Emits the end of the latest lockout, past or not, or null while there was none. */
    fun observeLockedUntil(): Flow<Instant?>

    /** Starts a lockout that ends at [until], and resets the wrong attempts. */
    suspend fun lockOut(until: Instant)

    /** Forgets the hash, the wrong attempts and the lockout. */
    suspend fun clear()
}
