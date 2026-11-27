package com.mebiad.checkpilot.domain.pin

import com.mebiad.checkpilot.core.clock.Clock
import com.mebiad.checkpilot.domain.session.DeleteSessionUsecase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformLatest
import java.security.MessageDigest
import java.time.Duration

class PinUsecaseImpl(
    private val store: PinStore,
    private val hasher: PinHasher,
    private val clock: Clock,
    private val sessions: DeleteSessionUsecase,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : PinUsecase {
    // Hashing is slow on purpose, and a PIN being saved finishes even if the screen asking for it goes away.
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val unlockedState = MutableStateFlow(false)

    override val isSet: Flow<Boolean> = store.observeHash().map { it != null }.distinctUntilChanged()

    @OptIn(ExperimentalCoroutinesApi::class)
    override val lockoutSecondsLeft: Flow<Int?> =
        store
            .observeLockedUntil()
            .transformLatest { until ->
                var millisLeft = until?.let { Duration.between(clock.now(), it).toMillis() } ?: 0
                while (millisLeft > 0) {
                    emit(((millisLeft + MILLIS_PER_SECOND - 1) / MILLIS_PER_SECOND).toInt())
                    delay((millisLeft - 1) % MILLIS_PER_SECOND + 1)
                    millisLeft = Duration.between(clock.now(), until).toMillis()
                }
                emit(null)
            }.distinctUntilChanged()

    override val unlocked: StateFlow<Boolean> = unlockedState.asStateFlow()

    override suspend fun set(
        pin: Pin,
        again: String,
    ): SetPinResult {
        if (again != pin.digits) return SetPinResult.Mismatch
        return scope
            .async {
                write(pin)
                unlockedState.value = true
                SetPinResult.Set
            }.await()
    }

    override suspend fun check(entry: String): PinCheck =
        scope
            .async {
                attempt(entry).also { if (it == PinCheck.Correct) unlockedState.value = true }
            }.await()

    override suspend fun verify(entry: String): PinCheck = scope.async { attempt(entry) }.await()

    override suspend fun change(
        current: String,
        newPin: Pin,
        again: String,
    ): ChangePinResult {
        if (again != newPin.digits) return ChangePinResult.Mismatch
        return scope
            .async {
                val check = if (store.observeHash().first() == null) PinCheck.Correct else attempt(current)
                when (check) {
                    PinCheck.Correct -> {
                        write(newPin)
                        ChangePinResult.Changed
                    }

                    PinCheck.Wrong -> {
                        ChangePinResult.Wrong
                    }

                    PinCheck.LockedOut -> {
                        ChangePinResult.LockedOut
                    }
                }
            }.await()
    }

    override suspend fun forget() {
        scope
            .async {
                // Sessions first: if the app dies halfway, the PIN still guards whatever is left.
                sessions.deleteAll()
                store.clear()
                lock()
            }.await()
    }

    override fun lock() {
        unlockedState.value = false
    }

    private suspend fun write(pin: Pin) {
        val salt = hasher.newSalt()
        store.writeHash(PinHash(hasher.hash(pin, salt, hasher.iterations), salt, hasher.iterations))
    }

    /** Checks [entry], counting a wrong PIN and starting the lockout on the fifth in a row. */
    private suspend fun attempt(entry: String): PinCheck =
        when {
            isLockedOut() -> {
                PinCheck.LockedOut
            }

            matches(entry) -> {
                store.resetWrongAttempts()
                PinCheck.Correct
            }

            else -> {
                store.recordWrongAttempt()
                if (store.wrongAttempts() < MAX_WRONG_ATTEMPTS) {
                    PinCheck.Wrong
                } else {
                    store.lockOut(until = clock.now().plus(LOCKOUT))
                    PinCheck.LockedOut
                }
            }
        }

    private suspend fun isLockedOut(): Boolean = store.observeLockedUntil().first()?.let { clock.now() < it } ?: false

    private suspend fun matches(entry: String): Boolean {
        val pin = Pin.parse(entry) ?: return false
        val stored = store.observeHash().first() ?: return false
        return MessageDigest.isEqual(stored.hash, hasher.hash(pin, stored.salt, stored.iterations))
    }

    private companion object {
        const val MAX_WRONG_ATTEMPTS = 5
        const val MILLIS_PER_SECOND = 1000L
        val LOCKOUT: Duration = Duration.ofSeconds(30)
    }
}
