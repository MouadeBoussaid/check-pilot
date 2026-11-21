package com.mebiad.checkpilot.testutils

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppHarnessTest {
    private val harness = AppHarness()
    private val composeRule = createEmptyComposeRule()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(harness).around(composeRule)

    @Test
    fun sendingTheAppToTheBackgroundStopsTheProcessLifecycle() {
        // Given
        harness.launch()

        // When
        harness.sendToBackground()

        // Then
        assertEquals(Lifecycle.State.CREATED, ProcessLifecycleOwner.get().lifecycle.currentState)
    }

    @Test
    fun bringingTheAppBackResumesTheProcessLifecycle() {
        // Given
        harness.launch()
        harness.sendToBackground()

        // When
        harness.bringToForeground()

        // Then
        assertEquals(Lifecycle.State.RESUMED, ProcessLifecycleOwner.get().lifecycle.currentState)
    }
}
