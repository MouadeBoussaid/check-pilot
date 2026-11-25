package com.mebiad.checkpilot.domain.pin

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class PinStoreFake : PinStore {
    private val hashState = MutableStateFlow<PinHash?>(null)

    val pinHash: PinHash?
        get() = hashState.value

    var attempts: Int = 0

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

    fun reset() {
        hashState.value = null
        attempts = 0
    }
}
