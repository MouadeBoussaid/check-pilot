package com.mebiad.checkpilot.ui.apps.pin

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

    /** A [PinViewModel] whose state is collected, as the screen does, once it knows whether a PIN is set. */
    private fun TestScope.shownViewModel(): PinViewModel {
        val viewModel = PinViewModel(pin = pin)
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
