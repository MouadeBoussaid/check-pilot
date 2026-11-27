package com.mebiad.checkpilot.data.local.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mebiad.checkpilot.domain.pin.PinHash
import com.mebiad.checkpilot.domain.pin.PinStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.Base64

/** Keeps the App PIN in the app's settings DataStore, the hash and salt as Base64, the lockout's end as epoch ms. */
class PinStoreImpl(
    private val dataStore: DataStore<Preferences>,
) : PinStore {
    override fun observeHash(): Flow<PinHash?> = dataStore.data.map { it.toPinHash() }

    override suspend fun writeHash(pinHash: PinHash) {
        dataStore.edit { settings ->
            settings[HASH] = Base64.getEncoder().encodeToString(pinHash.hash)
            settings[SALT] = Base64.getEncoder().encodeToString(pinHash.salt)
            settings[ITERATIONS] = pinHash.iterations
        }
    }

    override suspend fun wrongAttempts(): Int = dataStore.data.first()[WRONG_ATTEMPTS] ?: 0

    override suspend fun recordWrongAttempt() {
        dataStore.edit { it[WRONG_ATTEMPTS] = (it[WRONG_ATTEMPTS] ?: 0) + 1 }
    }

    override suspend fun resetWrongAttempts() {
        dataStore.edit { it.remove(WRONG_ATTEMPTS) }
    }

    override fun observeLockedUntil(): Flow<Instant?> =
        dataStore.data.map { settings -> settings[LOCKED_UNTIL]?.let(Instant::ofEpochMilli) }.distinctUntilChanged()

    override suspend fun lockOut(until: Instant) {
        dataStore.edit { settings ->
            settings[LOCKED_UNTIL] = until.toEpochMilli()
            settings.remove(WRONG_ATTEMPTS)
        }
    }

    override suspend fun clear() {
        dataStore.edit { settings ->
            settings.remove(HASH)
            settings.remove(SALT)
            settings.remove(ITERATIONS)
            settings.remove(WRONG_ATTEMPTS)
            settings.remove(LOCKED_UNTIL)
        }
    }

    private fun Preferences.toPinHash(): PinHash? {
        val hash = this[HASH] ?: return null
        val salt = this[SALT] ?: return null
        val iterations = this[ITERATIONS] ?: return null
        return PinHash(
            hash = Base64.getDecoder().decode(hash),
            salt = Base64.getDecoder().decode(salt),
            iterations = iterations
        )
    }

    private companion object {
        val HASH = stringPreferencesKey("pin_hash")
        val SALT = stringPreferencesKey("pin_salt")
        val ITERATIONS = intPreferencesKey("pin_iterations")
        val WRONG_ATTEMPTS = intPreferencesKey("pin_wrong_attempts")
        val LOCKED_UNTIL = longPreferencesKey("pin_locked_until")
    }
}
