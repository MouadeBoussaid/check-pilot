package com.mebiad.checkpilot.data.local.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.mebiad.checkpilot.domain.pin.PinHash
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class PinStoreImplTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val scopes = mutableListOf<CoroutineScope>()

    @After
    fun tearDown() = runBlocking { closeDataStores() }

    @Test
    fun theHashIsKeptAcrossARestart() =
        runTest {
            // Given
            val file = File(folder.root, "settings.preferences_pb")
            val before = PinStoreImpl(dataStore = openDataStore(file))
            assertNull(before.observeHash().first())

            // When
            before.writeHash(PinHash(hash = byteArrayOf(9, 8, 7), salt = byteArrayOf(1, 2), iterations = 120000))
            closeDataStores()
            val after = PinStoreImpl(dataStore = openDataStore(file))

            // Then
            val stored = checkNotNull(after.observeHash().first())
            assertArrayEquals(byteArrayOf(9, 8, 7), stored.hash)
            assertArrayEquals(byteArrayOf(1, 2), stored.salt)
            assertEquals(120000, stored.iterations)
        }

    @Test
    fun wrongAttemptsAddUpAcrossARestartUntilReset() =
        runTest {
            // Given
            val file = File(folder.root, "settings.preferences_pb")
            val before = PinStoreImpl(dataStore = openDataStore(file))
            assertEquals(0, before.wrongAttempts())

            // When / Then: two wrong attempts, then a restart
            before.recordWrongAttempt()
            before.recordWrongAttempt()
            closeDataStores()
            val after = PinStoreImpl(dataStore = openDataStore(file))
            assertEquals(2, after.wrongAttempts())

            // When / Then: a reset
            after.resetWrongAttempts()
            assertEquals(0, after.wrongAttempts())
        }

    private fun openDataStore(file: File): DataStore<Preferences> {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob()).also { scopes += it }
        return PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
    }

    private suspend fun closeDataStores() {
        scopes.forEach { it.coroutineContext[Job]?.cancelAndJoin() }
        scopes.clear()
    }
}
