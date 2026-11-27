package com.mebiad.checkpilot.domain.pin

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.Instant

class PinStoreFake : PinStore {
    private val hashState = MutableStateFlow<PinHash?>(null)

    val pinHash: PinHash?
        get() = hashState.value

    var attempts: Int = 0

    private val lockedUntilState = MutableStateFlow<Instant?>(null)

    var lockedUntil: Instant?
        get() = lockedUntilState.value
        set(value) {
            lockedUntilState.value = value
        }

    override fun observeHash(): Flow<PinHash?> = hashState

    override suspend fun writeHash(pinHash: PinHash) {
        hashState.value = pinHash
    }

    override suspend fun wrongAttempts(): Int = attempts

    override suspend fun recordWrongAttempt() {
        attempts++
    }

    override suspend fun resetWrongAttempts() {
        attempts = 0
    }

    override fun observeLockedUntil(): Flow<Instant?> = lockedUntilState

    override suspend fun lockOut(until: Instant) {
        lockedUntil = until
        attempts = 0
    }

    override suspend fun clear() {
        reset()
    }

    fun reset() {
        hashState.value = null
        attempts = 0
        lockedUntil = null
    }
}
