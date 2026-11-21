package com.mebiad.checkpilot

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mebiad.checkpilot.testutils.AppHarness
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SmokeTest {
    private val harness = AppHarness()
    private val composeRule = createEmptyComposeRule()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(harness).around(composeRule)

    @Test
    fun theAppOpensOnTheHomeScreen() {
        // Given: a fresh install

        // When
        harness.launch()

        // Then
        composeRule.onNodeWithText("Check Pilot").assertIsDisplayed()
    }

    @Test
    fun theHomeScreenShowsAgainAfterARestart() {
        // Given
        harness.launch()

        // When
        harness.restart()

        // Then
        composeRule.onNodeWithText("Check Pilot").assertIsDisplayed()
    }
}
