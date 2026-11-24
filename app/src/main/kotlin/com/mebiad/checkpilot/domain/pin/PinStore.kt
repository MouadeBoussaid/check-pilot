package com.mebiad.checkpilot.domain.pin

import kotlinx.coroutines.flow.Flow

/** The App PIN's hash, with the salt and the iteration count it was made with. */
class PinHash(
    val hash: ByteArray,
    val salt: ByteArray,
    val iterations: Int,
)

/** Keeps the App PIN's hash and the count of wrong PINs entered in a row, across restarts. */
interface PinStore {
    /** Emits the stored hash, or null while no PIN is set. */
    fun observeHash(): Flow<PinHash?>

    suspend fun writeHash(pinHash: PinHash)

    suspend fun wrongAttempts(): Int

    suspend fun recordWrongAttempt()

    suspend fun resetWrongAttempts()
}
