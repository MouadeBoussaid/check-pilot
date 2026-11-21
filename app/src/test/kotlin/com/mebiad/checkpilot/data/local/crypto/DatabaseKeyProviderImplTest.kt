package com.mebiad.checkpilot.data.local.crypto

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DatabaseKeyProviderImplTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val cipher = PassphraseCipherFake()

    @Test
    fun theFirstCallCreatesA32BytePassphrase() {
        // Given
        val provider = DatabaseKeyProviderImpl(directory = folder.root, cipher = cipher)

        // When
        val passphrase = provider.passphrase()

        // Then
        assertEquals(32, passphrase.size)
    }

    @Test
    fun laterCallsReturnTheSamePassphrase() {
        // Given
        val provider = DatabaseKeyProviderImpl(directory = folder.root, cipher = cipher)
        val first = provider.passphrase()

        // When
        val second = provider.passphrase()

        // Then
        assertArrayEquals(first, second)
    }

    @Test
    fun aNewProviderOverTheSameFolderReturnsTheSamePassphrase() {
        // Given
        val first = DatabaseKeyProviderImpl(directory = folder.root, cipher = cipher).passphrase()

        // When
        val second = DatabaseKeyProviderImpl(directory = folder.root, cipher = cipher).passphrase()

        // Then
        assertArrayEquals(first, second)
    }

    @Test
    fun theKeyFolderNeverHoldsThePlainPassphrase() {
        // Given
        val provider = DatabaseKeyProviderImpl(directory = folder.root, cipher = cipher)

        // When
        val passphrase = provider.passphrase()

        // Then
        val stored =
            folder.root
                .walk()
                .filter { it.isFile }
                .map { it.readBytes() }
                .toList()
        assertEquals(1, stored.size)
        assertFalse(stored.single().containsRun(passphrase))
    }

    private fun ByteArray.containsRun(run: ByteArray): Boolean =
        (0..size - run.size).any { start -> run.indices.all { this[start + it] == run[it] } }
}
