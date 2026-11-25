package com.mebiad.checkpilot.domain.pin

import app.cash.turbine.test
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PinUsecaseImplTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = PinStoreFake()
    private val usecase = PinUsecaseImpl(store = store, hasher = PinHasherFake(), dispatcher = dispatcher)

    @Test
    fun aNewPinTypedTheSameTwiceIsSetAndUnlocksTheSessionList() =
        runTest(dispatcher) {
            // Given
            val pin = checkNotNull(Pin.parse("1234"))

            usecase.isSet.test {
                assertFalse(awaitItem())

                // When
                val result = usecase.set(pin = pin, again = "1234")

                // Then
                assertEquals(SetPinResult.Set, result)
                assertTrue(awaitItem())
                assertTrue(usecase.unlocked.value)
            }
        }

    @Test
    fun aSecondTypingThatDiffersSetsNoPin() =
        runTest(dispatcher) {
            // Given
            val pin = checkNotNull(Pin.parse("1234"))

            // When
            val result = usecase.set(pin = pin, again = "1235")

            // Then
            assertEquals(SetPinResult.Mismatch, result)
            assertNull(store.pinHash)
            assertFalse(usecase.unlocked.value)
        }

    @Test
    fun theCorrectPinUnlocksTheSessionListAndClearsTheWrongAttempts() =
        runTest(dispatcher) {
            // Given
            usecase.set(pin = checkNotNull(Pin.parse("1234")), again = "1234")
            usecase.lock()
            store.attempts = 2

            // When
            val result = usecase.check("1234")

            // Then
            assertEquals(PinCheck.Correct, result)
            assertTrue(usecase.unlocked.value)
            assertEquals(0, store.attempts)
        }

    @Test
    fun aWrongPinKeepsTheSessionListLockedAndCountsTheAttempt() =
        runTest(dispatcher) {
            // Given
            usecase.set(pin = checkNotNull(Pin.parse("1234")), again = "1234")
            usecase.lock()

            // When
            val results = listOf("1235", "123", "12345").map { usecase.check(it) }

            // Then
            assertEquals(List(3) { PinCheck.Wrong }, results)
            assertFalse(usecase.unlocked.value)
            assertEquals(3, store.attempts)
        }

    @Test
    fun lockingEndsTheUnlock() =
        runTest(dispatcher) {
            // Given
            usecase.set(pin = checkNotNull(Pin.parse("1234")), again = "1234")

            // When
            usecase.lock()

            // Then
            assertFalse(usecase.unlocked.value)
        }
}
