package com.mebiad.checkpilot.domain.pin

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest

class PinUsecaseImpl(
    private val store: PinStore,
    private val hasher: PinHasher,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : PinUsecase {
    // Hashing is slow on purpose, and a PIN being saved finishes even if the screen asking for it goes away.
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val unlockedState = MutableStateFlow(false)

    override val isSet: Flow<Boolean> = store.observeHash().map { it != null }.distinctUntilChanged()

    override val unlocked: StateFlow<Boolean> = unlockedState.asStateFlow()

    override suspend fun set(
        pin: Pin,
        again: String,
    ): SetPinResult {
        if (again != pin.digits) return SetPinResult.Mismatch
        return scope
            .async {
                val salt = hasher.newSalt()
                store.writeHash(PinHash(hasher.hash(pin, salt, hasher.iterations), salt, hasher.iterations))
                unlockedState.value = true
                SetPinResult.Set
            }.await()
    }

    override suspend fun check(entry: String): PinCheck =
        scope
            .async {
                if (matches(entry)) {
                    store.resetWrongAttempts()
                    unlockedState.value = true
                    PinCheck.Correct
                } else {
                    store.recordWrongAttempt()
                    PinCheck.Wrong
                }
            }.await()

    override fun lock() {
        unlockedState.value = false
    }

    private suspend fun matches(entry: String): Boolean {
        val pin = Pin.parse(entry) ?: return false
        val stored = store.observeHash().first() ?: return false
        return MessageDigest.isEqual(stored.hash, hasher.hash(pin, stored.salt, stored.iterations))
    }
}
