package com.mebiad.checkpilot.acceptance

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mebiad.checkpilot.data.local.db.CheckPilotDatabase
import com.mebiad.checkpilot.domain.session.AirportCode
import com.mebiad.checkpilot.domain.session.Session
import com.mebiad.checkpilot.domain.session.SessionRepository
import com.mebiad.checkpilot.domain.session.StaffNumber
import com.mebiad.checkpilot.testutils.AppHarness
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

/**
 * US-5: Continue the Last Session from the home screen. Each test seeds zero or more Sessions with a set creation time,
 * or deletes the Last Session, then opens the home screen and checks its buttons and what Continue last Session opens.
 */
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class US5ContinueTheLastSessionTest {
    private val harness = AppHarness()
    private val composeRule = createEmptyComposeRule()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(harness).around(composeRule)

    /**
     * 001-mvp/US-5/AS1: **Given** no Session exists, **when** the home screen opens, **then** it shows New session,
     * Open session and Settings, and no Continue last Session.
     */
    @Test
    fun `001_mvp_US_5_AS1_withoutSessionsHomeShowsNewOpenAndSettingsButNoContinue`() {
        // Given
        // A fresh install: no Session has been created.

        // When
        harness.launch()

        // Then
        composeRule.waitUntilAtLeastOneExists(hasText("New Session", ignoreCase = true), TIMEOUT_MS)
        composeRule.onNodeWithText("New Session", ignoreCase = true).assertHasClickAction()
        composeRule.onNodeWithText("Open Session", ignoreCase = true).assertHasClickAction()
        composeRule.onNodeWithContentDescription("Settings").assertHasClickAction()
        composeRule.onNodeWithText(CONTINUE, ignoreCase = true).assertDoesNotExist()
    }

    /**
     * 001-mvp/US-5/AS2: **Given** a Session was created, **when** the Assessor taps Continue last Session, **then**
     * that Session opens in the capture screen without asking for the PIN.
     */
    @Test
    fun `001_mvp_US_5_AS2_continueOpensTheLastSessionWithoutThePin`() {
        // Given
        seed(session(id = "older", departure = "AMS", destination = "LHR", createdAt = CREATED_AT))
        seed(
            session(
                id = "last",
                departure = "DOH",
                destination = "SLL",
                date = LocalDate.of(2026, 11, 1),
                createdAt = CREATED_AT.plus(Duration.ofHours(1))
            )
        )
        harness.launch()

        // When
        composeRule.waitUntilAtLeastOneExists(hasText(CONTINUE, ignoreCase = true), TIMEOUT_MS)
        composeRule.onNodeWithText(CONTINUE, ignoreCase = true).performClick()

        // Then
        composeRule.waitUntilExactlyOneExists(hasText("DOH → SLL"), TIMEOUT_MS)
        composeRule.onNodeWithText("01 Nov 2026").assertExists()
        composeRule.onNodeWithText("79125").assertExists()
        composeRule.onNodeWithText("AMS → LHR").assertDoesNotExist()
        composeRule.onNodeWithText(CONTINUE, ignoreCase = true).assertDoesNotExist()
    }

    /**
     * 001-mvp/US-5/AS3: **Given** the Last Session has been deleted, **when** the home screen opens, **then** Continue
     * last Session is not shown until the Assessor creates a new Session.
     */
    @Test
    fun `001_mvp_US_5_AS3_afterTheLastSessionIsDeletedContinueWaitsForANewSession`() {
        // Given
        seed(session(id = "deleted", departure = "AMS", destination = "LHR", createdAt = CREATED_AT))
        deleteSessionRow(id = "deleted")
        harness.clock.instant = CREATED_AT.plus(Duration.ofHours(1))

        // When
        harness.launch()

        // Then
        composeRule.waitUntilAtLeastOneExists(hasText("New Session", ignoreCase = true), TIMEOUT_MS)
        composeRule.onNodeWithText(CONTINUE, ignoreCase = true).assertDoesNotExist()

        // When / Then: the Assessor creates a new Session and goes back to the home screen
        startSessionThroughSetup(cm1 = "04512", departure = "DOH", destination = "SLL")
        harness.pressBack()
        composeRule.waitUntilAtLeastOneExists(hasText("New Session", ignoreCase = true), TIMEOUT_MS)
        composeRule.waitUntilAtLeastOneExists(hasText(CONTINUE, ignoreCase = true), TIMEOUT_MS)
        composeRule.onNodeWithText(CONTINUE, ignoreCase = true).performClick()
        composeRule.waitUntilExactlyOneExists(hasText("DOH → SLL"), TIMEOUT_MS)
        composeRule.onNodeWithText("AMS → LHR").assertDoesNotExist()
    }

    private fun startSessionThroughSetup(
        cm1: String,
        departure: String,
        destination: String,
    ) {
        composeRule.onNodeWithText("New Session", ignoreCase = true).performClick()
        composeRule.waitUntilAtLeastOneExists(hasText("Start"), TIMEOUT_MS)
        field("CM1").performTextInput(cm1)
        field("From").performTextInput(departure)
        field("To").performTextInput(destination)
        composeRule.onNodeWithText("Start").performClick()
        composeRule.waitUntilExactlyOneExists(hasText("$departure → $destination"), TIMEOUT_MS)
    }

    /** The text field labelled [label] on the setup screen: "CM1", "CM2", "From" or "To". */
    private fun field(label: String): SemanticsNodeInteraction =
        composeRule.onNode(hasSetTextAction() and (hasText(label) or hasContentDescription(label)))

    private fun seed(session: Session) {
        runBlocking { harness.get<SessionRepository>().insert(session) }
    }

    /** Deleting a Session comes with US-7, so the Given removes its row straight from the database. */
    private fun deleteSessionRow(id: String) {
        harness
            .get<CheckPilotDatabase>()
            .openHelper.writableDatabase
            .execSQL("DELETE FROM session WHERE id = ?", arrayOf(id))
    }

    private fun session(
        id: String,
        departure: String,
        destination: String,
        createdAt: Instant,
        date: LocalDate = LocalDate.of(2026, 10, 30),
    ): Session =
        Session(
            id = id,
            createdAt = createdAt,
            date = date,
            cm1StaffNumber = StaffNumber.parse("79125"),
            cm2StaffNumber = null,
            departure = checkNotNull(AirportCode.parse(departure)),
            destination = checkNotNull(AirportCode.parse(destination))
        )

    private companion object {
        const val TIMEOUT_MS = 5000L
        const val CONTINUE = "Continue last Session"
        val CREATED_AT: Instant = Instant.parse("2026-11-01T08:00:00Z")
    }
}
