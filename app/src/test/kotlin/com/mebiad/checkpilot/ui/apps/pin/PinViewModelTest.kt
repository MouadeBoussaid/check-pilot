package com.mebiad.checkpilot.ui.apps.pin

import app.cash.turbine.test
import com.mebiad.checkpilot.domain.pin.PinUsecaseFake
import com.mebiad.checkpilot.testutils.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PinViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val pin = PinUsecaseFake()

    @Test
    fun withoutAPinItAsksForANewOne() =
        runTest {
            // Given
            pin.pin = null

            // When
            val viewModel = shownViewModel()
            advanceUntilIdle()

            // Then
            assertEquals(PinStep.NewPin, viewModel.state.value.step)
        }

    @Test
    fun withAPinItAsksToEnterIt() =
        runTest {
            // Given
            pin.pin = "1234"

            // When
            val viewModel = shownViewModel()
            advanceUntilIdle()

            // Then
            assertEquals(PinStep.Enter, viewModel.state.value.step)
        }

    @Test
    fun digitsAreCountedAndBackspaceRemovesTheLast() =
        runTest {
            // Given
            val viewModel = shownViewModel()

            // When
            "123".forEach(viewModel::onDigit)
            viewModel.onBackspace()
            advanceUntilIdle()

            // Then
            assertEquals(2, viewModel.state.value.digitCount)
        }

    @Test
    fun aNewPinOfTheWrongLengthSaysSoAndStaysOnTheNewPin() =
        runTest {
            // Given
            val viewModel = shownViewModel()

            // When / Then: 3 digits, then 7 digits
            listOf("123", "1234567").forEach { digits ->
                digits.forEach(viewModel::onDigit)
                viewModel.onConfirm()
                advanceUntilIdle()
                val state = viewModel.state.value
                assertEquals(PinStep.NewPin, state.step)
                assertEquals(PinMessage.NotFourToSixDigits, state.message)
                assertEquals(0, state.digitCount)
            }
            assertEquals(null, pin.pin)
        }

    @Test
    fun aValidNewPinAsksForItAgain() =
        runTest {
            // Given
            val viewModel = shownViewModel()

            // When
            "1234".forEach(viewModel::onDigit)
            viewModel.onConfirm()
            advanceUntilIdle()

            // Then
            assertEquals(PinUiState(step = PinStep.Again, digitCount = 0, message = null), viewModel.state.value)
            assertEquals(null, pin.pin)
        }

    @Test
    fun aDifferentSecondPinSaysTheyDontMatchAndSetsNoPin() =
        runTest {
            // Given
            val viewModel = shownViewModel()
            viewModel.type("1234")
            advanceUntilIdle()

            // When
            viewModel.type("1235")
            advanceUntilIdle()

            // Then
            assertEquals(
                PinUiState(step = PinStep.Again, digitCount = 0, message = PinMessage.Mismatch),
                viewModel.state.value
            )
            assertEquals(null, pin.pin)
        }

    @Test
    fun aMatchingSecondPinSetsThePinAndUnlocks() =
        runTest {
            // Given
            val viewModel = shownViewModel()
            viewModel.type("1234")
            advanceUntilIdle()

            // When
            viewModel.type("1234")
            advanceUntilIdle()

            // Then
            assertEquals("1234", pin.pin)
            assertEquals(true, pin.unlocked.value)
        }

    @Test
    fun startingOverAsksForANewPinAgain() =
        runTest {
            // Given
            val viewModel = shownViewModel()
            viewModel.type("1234")
            viewModel.type("1235")
            advanceUntilIdle()

            // When
            viewModel.onStartOver()
            advanceUntilIdle()

            // Then
            assertEquals(PinUiState(step = PinStep.NewPin, digitCount = 0, message = null), viewModel.state.value)
        }

    @Test
    fun aWrongPinSaysSoAndStaysLocked() =
        runTest {
            // Given
            pin.pin = "1234"
            val viewModel = shownViewModel()
            advanceUntilIdle()

            // When
            viewModel.type("1235")
            advanceUntilIdle()

            // Then
            assertEquals(
                PinUiState(step = PinStep.Enter, digitCount = 0, message = PinMessage.Wrong),
                viewModel.state.value
            )
            assertEquals(listOf("1235"), pin.checks)
            assertEquals(false, pin.unlocked.value)
        }

    @Test
    fun theCorrectPinUnlocks() =
        runTest {
            // Given
            pin.pin = "1234"
            val viewModel = shownViewModel()
            advanceUntilIdle()

            // When
            viewModel.type("1234")
            advanceUntilIdle()

            // Then
            assertEquals(true, pin.unlocked.value)
            assertEquals(0, viewModel.state.value.digitCount)
        }

    @Test
    fun typingADigitClearsTheMessage() =
        runTest {
            // Given
            pin.pin = "1234"
            val viewModel = shownViewModel()
            viewModel.type("1235")
            advanceUntilIdle()

            // When
            viewModel.onDigit('1')
            advanceUntilIdle()

            // Then
            assertEquals(null, viewModel.state.value.message)
        }

    @Test
    fun aSecondConfirmWhileTheFirstIsCheckedChecksNothingMore() =
        runTest {
            // Given
            pin.pin = "1234"
            pin.hold = CompletableDeferred()
            val viewModel = shownViewModel()
            viewModel.type("1235")
            advanceUntilIdle()

            // When
            viewModel.type("9")
            viewModel.onConfirm()
            pin.hold?.complete(Unit)
            advanceUntilIdle()

            // Then
            assertEquals(listOf("1235"), pin.checks)
        }

    @Test
    fun changingAPinAsksForTheCurrentOneFirst() =
        runTest {
            // Given
            pin.pin = "1234"

            // When
            val viewModel = shownViewModel(mode = PinMode.Change)

            // Then
            assertEquals(PinUiState(mode = PinMode.Change, step = PinStep.Current), viewModel.state.value)
        }

    @Test
    fun aWrongCurrentPinSaysSoAndTheRightOneAsksForTheNewPin() =
        runTest {
            // Given
            pin.pin = "1234"
            val viewModel = shownViewModel(mode = PinMode.Change)

            // When / Then: a wrong current PIN
            viewModel.type("1235")
            advanceUntilIdle()
            assertEquals(
                PinUiState(mode = PinMode.Change, step = PinStep.Current, message = PinMessage.Wrong),
                viewModel.state.value
            )

            // When / Then: the right current PIN
            viewModel.type("1234")
            advanceUntilIdle()
            assertEquals(PinUiState(mode = PinMode.Change, step = PinStep.NewPin), viewModel.state.value)
            assertEquals(listOf("1235", "1234"), pin.verifications)
            assertEquals(false, pin.unlocked.value)
        }

    @Test
    fun theNewPinTypedTwiceChangesThePinAndEndsTheChange() =
        runTest {
            // Given
            pin.pin = "1234"
            val viewModel = shownViewModel(mode = PinMode.Change)
            viewModel.type("1234")
            advanceUntilIdle()

            viewModel.events.test {
                // When
                viewModel.type("975724")
                advanceUntilIdle()
                assertEquals(PinStep.Again, viewModel.state.value.step)
                viewModel.type("975724")
                advanceUntilIdle()

                // Then
                assertEquals(PinEvent.Changed, awaitItem())
            }
            assertEquals("975724", pin.pin)
            assertEquals(false, pin.unlocked.value)
        }

    @Test
    fun duringTheLockoutTheSecondsLeftShowAndTheKeypadDoesNothing() =
        runTest {
            // Given
            pin.pin = "1234"
            val viewModel = shownViewModel()
            "12".forEach(viewModel::onDigit)

            // When
            pin.secondsLeft.value = 30
            advanceUntilIdle()
            viewModel.onDigit('3')
            viewModel.onBackspace()
            viewModel.onConfirm()
            advanceUntilIdle()

            // Then
            assertEquals(
                PinUiState(step = PinStep.Enter, digitCount = 2, lockoutSecondsLeft = 30),
                viewModel.state.value
            )
            assertEquals(emptyList<String>(), pin.checks)
        }

    @Test
    fun forgotPinWarnsAndCancellingForgetsNothing() =
        runTest {
            // Given
            pin.pin = "1234"
            val viewModel = shownViewModel()

            // When / Then: Forgot PIN shows the warning
            viewModel.onForgotPin()
            advanceUntilIdle()
            assertEquals(true, viewModel.state.value.forgotPinWarning)

            // When / Then: Cancel hides it and forgets nothing
            viewModel.onCancelForgotPin()
            advanceUntilIdle()
            assertEquals(false, viewModel.state.value.forgotPinWarning)
            assertEquals(0, pin.forgetCount)
            assertEquals("1234", pin.pin)
        }

    @Test
    fun confirmingForgotPinForgetsThePinAndAsksForANewOne() =
        runTest {
            // Given
            pin.pin = "1234"
            pin.secondsLeft.value = 30
            val viewModel = shownViewModel()
            viewModel.onForgotPin()

            // When
            viewModel.onConfirmForgotPin()
            advanceUntilIdle()

            // Then
            assertEquals(1, pin.forgetCount)
            assertEquals(PinUiState(step = PinStep.NewPin), viewModel.state.value)
        }

    /** A [PinViewModel] whose state is collected, as the screen does, once it knows whether a PIN is set. */
    private fun TestScope.shownViewModel(mode: PinMode = PinMode.Unlock): PinViewModel {
        val viewModel = PinViewModel(pin = pin, mode = mode)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
        advanceUntilIdle()
        return viewModel
    }

    /** Types [digits] on the keypad and confirms them. */
    private fun PinViewModel.type(digits: String) {
        digits.forEach(::onDigit)
        onConfirm()
    }
}
