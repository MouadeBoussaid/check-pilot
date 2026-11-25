package com.mebiad.checkpilot.domain.pin

import app.cash.turbine.test
import com.mebiad.checkpilot.core.clock.ClockFake
import com.mebiad.checkpilot.domain.session.DeleteSessionUsecaseFake
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class PinUsecaseImplTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = PinStoreFake()
    private val clock = ClockFake(instant = NOW)
    private val sessions = DeleteSessionUsecaseFake()
    private val usecase =
        PinUsecaseImpl(
            store = store,
            hasher = PinHasherFake(),
            clock = clock,
            sessions = sessions,
            dispatcher = dispatcher
        )

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

    @Test
    fun theFifthWrongPinInARowLocksTheEntryOutFor30Seconds() =
        runTest(dispatcher) {
            // Given
            givenPin("1234")
            store.attempts = 4

            // When
            val result = usecase.check("1235")

            // Then
            assertEquals(PinCheck.LockedOut, result)
            assertEquals(NOW.plusSeconds(30), store.lockedUntil)
            assertEquals(0, store.attempts)
            assertFalse(usecase.unlocked.value)
        }

    @Test
    fun noPinIsCheckedUntilTheLockoutEnds() =
        runTest(dispatcher) {
            // Given
            givenPin("1234")
            store.lockOut(until = NOW.plusSeconds(30))

            // When / Then: 29 seconds later, the correct PIN is refused and nothing is counted
            clock.advanceBy(Duration.ofSeconds(29))
            assertEquals(PinCheck.LockedOut, usecase.check("1234"))
            assertEquals(PinCheck.LockedOut, usecase.check("1235"))
            assertFalse(usecase.unlocked.value)
            assertEquals(0, store.attempts)

            // When / Then: 30 seconds later, the correct PIN unlocks
            clock.advanceBy(Duration.ofSeconds(1))
            assertEquals(PinCheck.Correct, usecase.check("1234"))
            assertTrue(usecase.unlocked.value)
        }

    @Test
    fun theSecondsLeftCountDownEverySecondUntilTheLockoutEnds() =
        runTest(dispatcher) {
            // Given
            store.lockOut(until = NOW.plusSeconds(30))

            usecase.lockoutSecondsLeft.test {
                // Then
                assertEquals(30, awaitItem())

                // When / Then: 29 seconds later, 1 second is left
                passTime(Duration.ofSeconds(29))
                assertEquals(1, awaitItem())

                // When / Then: 30 seconds later, the lockout is over
                passTime(Duration.ofSeconds(1))
                assertNull(awaitItem())
            }
        }

    @Test
    fun withoutALockoutNoSecondsAreLeft() =
        runTest(dispatcher) {
            // Given
            store.lockedUntil = null

            usecase.lockoutSecondsLeft.test {
                // When
                val secondsLeft = awaitItem()

                // Then
                assertNull(secondsLeft)
            }
        }

    @Test
    fun verifyingTheCurrentPinCountsWrongOnesAndNeverUnlocks() =
        runTest(dispatcher) {
            // Given
            givenPin("1234")

            // When / Then: a wrong PIN is counted
            assertEquals(PinCheck.Wrong, usecase.verify("1235"))
            assertEquals(1, store.attempts)

            // When / Then: the correct PIN resets the count and leaves the Session list locked
            assertEquals(PinCheck.Correct, usecase.verify("1234"))
            assertEquals(0, store.attempts)
            assertFalse(usecase.unlocked.value)

            // When / Then: the fifth wrong PIN in a row locks out
            store.attempts = 4
            assertEquals(PinCheck.LockedOut, usecase.verify("1235"))
            assertEquals(PinCheck.LockedOut, usecase.verify("1234"))
        }

    @Test
    fun theCurrentPinAndANewOneTypedTwiceChangeThePinWithoutUnlocking() =
        runTest(dispatcher) {
            // Given
            givenPin("1234")

            // When
            val result = usecase.change(current = "1234", newPin = checkNotNull(Pin.parse("975724")), again = "975724")

            // Then
            assertEquals(ChangePinResult.Changed, result)
            assertFalse(usecase.unlocked.value)
            assertEquals(PinCheck.Wrong, usecase.verify("1234"))
            assertEquals(PinCheck.Correct, usecase.verify("975724"))
        }

    @Test
    fun aWrongCurrentPinOrADifferentSecondTypingKeepsThePin() =
        runTest(dispatcher) {
            // Given
            givenPin("1234")
            val newPin = checkNotNull(Pin.parse("975724"))

            // When
            val wrong = usecase.change(current = "1235", newPin = newPin, again = "975724")
            val mismatch = usecase.change(current = "1234", newPin = newPin, again = "975725")

            // Then
            assertEquals(ChangePinResult.Wrong, wrong)
            assertEquals(ChangePinResult.Mismatch, mismatch)
            assertEquals(PinCheck.Correct, usecase.verify("1234"))
        }

    @Test
    fun withNoPinSetANewOneIsSetWithoutTheCurrentPinOrUnlocking() =
        runTest(dispatcher) {
            // Given
            store.reset()

            // When
            val result = usecase.change(current = "", newPin = checkNotNull(Pin.parse("975724")), again = "975724")

            // Then
            assertEquals(ChangePinResult.Changed, result)
            assertFalse(usecase.unlocked.value)
            assertEquals(PinCheck.Correct, usecase.verify("975724"))
        }

    @Test
    fun forgettingThePinDeletesEverySessionAndForgetsThePinAndItsLockout() =
        runTest(dispatcher) {
            // Given
            usecase.set(pin = checkNotNull(Pin.parse("1234")), again = "1234")
            store.lockOut(until = NOW.plusSeconds(30))
            store.attempts = 2

            usecase.isSet.test {
                assertTrue(awaitItem())

                // When
                usecase.forget()

                // Then
                assertFalse(awaitItem())
            }
            assertEquals(1, sessions.deleteAllCount)
            assertNull(store.lockedUntil)
            assertEquals(0, store.attempts)
            assertFalse(usecase.unlocked.value)
        }

    /** Moves the clock and the test scheduler on together. */
    private fun TestScope.passTime(duration: Duration) {
        clock.advanceBy(duration)
        advanceTimeBy(duration.toMillis())
        runCurrent()
    }

    private suspend fun givenPin(digits: String) {
        usecase.set(pin = checkNotNull(Pin.parse(digits)), again = digits)
        usecase.lock()
    }

    private companion object {
        val NOW: Instant = Instant.parse("2026-11-01T09:00:00Z")
    }
}
