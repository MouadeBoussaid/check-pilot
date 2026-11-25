package com.mebiad.checkpilot.acceptance

import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mebiad.checkpilot.data.local.db.CheckPilotDatabase
import com.mebiad.checkpilot.testutils.AppHarness
import com.mebiad.checkpilot.testutils.SessionSeeds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.Duration
import java.time.Instant

/**
 * US-8: Change or reset the PIN. Each test sets a PIN through the PIN screens and restarts the app, then changes the
 * PIN from Settings, enters wrong PINs while time passes on the `ClockFake`, or uses Forgot PIN on the PIN screen.
 *
 * How the tests find things, so restyling doesn't break them:
 * - The home screen's gear is the clickable node described "Settings". Settings shows the title "Settings" and a
 *   clickable "Change PIN".
 * - Changing the PIN shows the keypad three times: under a text containing "current PIN", then one containing
 *   "new PIN", then one containing "again". Once the new PIN is confirmed, Settings shows again.
 * - The PIN screens are told apart by their titles: "Set a PIN" with no PIN set, "Enter PIN" once one is.
 * - The keypad's digits are the clickable nodes whose text is the digit, such as "7"; its ✓ is the clickable node
 *   described "Confirm". While the lockout lasts, no digit key and no ✓ is enabled, and the message says how many
 *   seconds are left, such as "Wait 30 seconds", in one text. The PIN screen reads the time again at least every
 *   second, so the keypad comes back once 30 seconds have passed.
 * - The PIN screen offers a clickable "Forgot PIN". Its warning is a dialog whose text says "all saved Sessions" will
 *   be deleted, with a clickable "Delete all Sessions" and "Cancel".
 * - The Session list is showing while the text "Sessions" stands alone outside pop-ups; each Session shows its route,
 *   such as "DOH → SLL".
 * - The capture screen shows each Competency's name.
 *
 * The PINs avoid the digits 0, 1 and 3, so a key never shares its text with a number of seconds left.
 * The screen is tall so every row of a list is laid out at once, whatever list it uses.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h3000dp")
@OptIn(ExperimentalTestApi::class)
class US8ChangeOrResetThePinTest {
    private val harness = AppHarness()
    private val composeRule = createEmptyComposeRule()
    private val seeds = SessionSeeds(harness)

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(harness).around(composeRule)

    /**
     * 001-mvp/US-8/AS1: **Given** a PIN is set, **when** the Assessor enters the old PIN and a new PIN twice in
     * Settings, **then** the Session list opens with the new PIN and no longer with the old one.
     */
    @Test
    fun `001_mvp_US_8_AS1_changingThePinInSettingsOpensTheListWithTheNewPinOnly`() {
        // Given
        seeds.session("old", departure = "DOH", destination = "LHR", createdAt = CREATED_AT)
        seeds.session("last", departure = "DOH", destination = "SLL", createdAt = CREATED_AT.plusSeconds(3600))
        givenPinIsSet(PIN)

        // When
        openChangePin()
        waitForKeypadUnder(CURRENT_PIN)
        enterPin(PIN)
        composeRule.waitUntilDoesNotExist(hasText(CURRENT_PIN, substring = true, ignoreCase = true), TIMEOUT_MS)
        waitForKeypadUnder(NEW_PIN)
        enterPin(NEW_PIN_DIGITS)
        waitForKeypadUnder(AGAIN)
        enterPin(NEW_PIN_DIGITS)
        waitForSettings()

        // Then
        backToHome()
        tapOpenSession()
        composeRule.waitUntilExactlyOneExists(hasText(ENTER_PIN), TIMEOUT_MS)
        enterPin(PIN)
        composeRule.waitUntilAtLeastOneExists(hasText(PIN_IS_WRONG, substring = true, ignoreCase = true), TIMEOUT_MS)
        composeRule.onNode(sessionListTitle()).assertDoesNotExist()
        enterPin(NEW_PIN_DIGITS)
        waitForSessionList()
        composeRule.waitUntilExactlyOneExists(route("DOH", "LHR"), TIMEOUT_MS)
    }

    /**
     * 001-mvp/US-8/AS2: **Given** a PIN is set, **when** the Assessor enters a wrong old PIN in Settings, **then** a
     * message says the PIN is wrong and the PIN stays unchanged.
     */
    @Test
    fun `001_mvp_US_8_AS2_aWrongOldPinInSettingsSaysSoAndKeepsThePin`() {
        // Given
        seeds.session("old", departure = "DOH", destination = "LHR", createdAt = CREATED_AT)
        seeds.session("last", departure = "DOH", destination = "SLL", createdAt = CREATED_AT.plusSeconds(3600))
        givenPinIsSet(PIN)
        openChangePin()
        waitForKeypadUnder(CURRENT_PIN)

        // When
        enterPin(WRONG_PIN)

        // Then
        composeRule.waitUntilAtLeastOneExists(hasText(PIN_IS_WRONG, substring = true, ignoreCase = true), TIMEOUT_MS)
        composeRule.onNode(hasText(CURRENT_PIN, substring = true, ignoreCase = true)).assertExists()
        composeRule.onNode(hasText(AGAIN, substring = true, ignoreCase = true)).assertDoesNotExist()
        backToHome()
        tapOpenSession()
        composeRule.waitUntilExactlyOneExists(hasText(ENTER_PIN), TIMEOUT_MS)
        enterPin(PIN)
        waitForSessionList()
        composeRule.waitUntilExactlyOneExists(route("DOH", "LHR"), TIMEOUT_MS)
    }

    /**
     * 001-mvp/US-8/AS3: **Given** 5 wrong PIN entries in a row, **when** the Assessor tries again, **then** a message
     * says to wait 30 seconds and no PIN can be entered until then.
     */
    @Test
    fun `001_mvp_US_8_AS3_fiveWrongPinsInARowMakeTheAssessorWait30Seconds`() {
        // Given
        seeds.session("old", departure = "DOH", destination = "LHR", createdAt = CREATED_AT)
        seeds.session("last", departure = "DOH", destination = "SLL", createdAt = CREATED_AT.plusSeconds(3600))
        givenPinIsSet(PIN)
        tapOpenSession()
        composeRule.waitUntilExactlyOneExists(hasText(ENTER_PIN), TIMEOUT_MS)
        repeat(5) {
            enterPin(WRONG_PIN)
            composeRule.waitUntilAtLeastOneExists(
                hasText(PIN_IS_WRONG, substring = true, ignoreCase = true) or
                    hasText(SECONDS, substring = true, ignoreCase = true),
                TIMEOUT_MS
            )
        }

        // When
        tryToEnterPin(PIN)

        // Then
        composeRule.waitUntilAtLeastOneExists(hasText(WAIT_30_SECONDS, substring = true, ignoreCase = true), TIMEOUT_MS)
        assertKeypadRefusesEntry()
        composeRule.onNode(sessionListTitle()).assertDoesNotExist()

        // When / Then: 29 seconds later, still no PIN can be entered
        harness.passTime(Duration.ofSeconds(29))
        tryToEnterPin(PIN)
        assertKeypadRefusesEntry()
        composeRule.onNode(sessionListTitle()).assertDoesNotExist()

        // When / Then: once 30 seconds have passed, the PIN opens the list
        harness.passTime(Duration.ofSeconds(1))
        composeRule.waitUntilExactlyOneExists(digitKey(PIN.first()) and isEnabled(), TIMEOUT_MS)
        enterPin(PIN)
        waitForSessionList()
        composeRule.waitUntilExactlyOneExists(route("DOH", "LHR"), TIMEOUT_MS)
    }

    /**
     * 001-mvp/US-8/AS4: **Given** the PIN screen, **when** the Assessor taps Forgot PIN, **then** a warning says all
     * saved Sessions will be deleted, and cancelling deletes nothing.
     */
    @Test
    fun `001_mvp_US_8_AS4_forgotPinWarnsThatAllSessionsGoAndCancellingDeletesNothing`() {
        // Given
        seeds.session("old", departure = "DOH", destination = "LHR", createdAt = CREATED_AT)
        seeds.session("last", departure = "DOH", destination = "SLL", createdAt = CREATED_AT.plusSeconds(3600))
        givenPinIsSet(PIN)
        tapOpenSession()
        composeRule.waitUntilExactlyOneExists(hasText(ENTER_PIN), TIMEOUT_MS)

        // When
        tapForgotPin()

        // Then
        composeRule.waitUntilExactlyOneExists(forgotPinWarning(), TIMEOUT_MS)

        // When / Then: Cancel deletes nothing
        composeRule.onNode(inPopUp() and hasText(CANCEL) and hasClickAction()).performClick()
        composeRule.waitUntilDoesNotExist(forgotPinWarning(), TIMEOUT_MS)
        composeRule.onNode(hasText(ENTER_PIN)).assertExists()
        enterPin(PIN)
        waitForSessionList()
        composeRule.waitUntil(TIMEOUT_MS) { routesShown().size == 2 }
        assertEquals(setOf("DOH → SLL", "DOH → LHR"), routesShown().toSet())
        backToHome()
        composeRule.waitUntilExactlyOneExists(hasText(CONTINUE, ignoreCase = true) and hasClickAction(), TIMEOUT_MS)
    }

    /**
     * 001-mvp/US-8/AS5: **Given** the Forgot PIN warning, **when** the Assessor confirms, **then** every Session is
     * gone, including the Last Session, Continue last Session is no longer shown, and the app asks for a new PIN.
     */
    @Test
    fun `001_mvp_US_8_AS5_confirmingForgotPinDeletesEverySessionAndAsksForANewPin`() {
        // Given
        seeds.session("old", departure = "DOH", destination = "LHR", createdAt = CREATED_AT)
        seeds.note("old", competencyId = 1, text = "Late gear", cmTag = "CM1")
        seeds.grade("old", competencyId = 1, crewMember = "CM1", value = "ADEQUATE")
        seeds.session("last", departure = "DOH", destination = "SLL", createdAt = CREATED_AT.plusSeconds(3600))
        givenPinIsSet(PIN)
        tapOpenSession()
        composeRule.waitUntilExactlyOneExists(hasText(ENTER_PIN), TIMEOUT_MS)
        tapForgotPin()
        composeRule.waitUntilExactlyOneExists(forgotPinWarning(), TIMEOUT_MS)

        // When
        composeRule.onNode(inPopUp() and hasText(DELETE_ALL_SESSIONS) and hasClickAction()).performClick()

        // Then
        composeRule.waitUntilExactlyOneExists(hasText(SET_A_PIN) and !inPopUp(), TIMEOUT_MS)
        composeRule.onNode(hasText(ENTER_PIN)).assertDoesNotExist()
        backToHome()
        composeRule.waitUntilDoesNotExist(hasText(CONTINUE, ignoreCase = true), TIMEOUT_MS)
        composeRule.onNode(route("DOH", "SLL")).assertDoesNotExist()
        tapOpenSession()
        composeRule.waitUntilExactlyOneExists(hasText(SET_A_PIN), TIMEOUT_MS)
        enterPin(NEW_PIN_DIGITS)
        composeRule.waitUntilAtLeastOneExists(hasText(AGAIN, substring = true, ignoreCase = true), TIMEOUT_MS)
        enterPin(NEW_PIN_DIGITS)
        waitForSessionList()
        assertEquals(emptyList<String>(), routesShown())
    }

    /**
     * 001-mvp/US-8/AS6: **Given** a Catalogue the Assessor has edited, **when** the Assessor confirms Forgot PIN,
     * **then** the Catalogue keeps its edits.
     *
     * The Catalogue editor comes with US-9, so the edits are written straight into the Catalogue's rows: Competency 1
     * renamed, and a tenth Competency added at the end. A Note of the deleted Session sits under the renamed one.
     */
    @Test
    fun `001_mvp_US_8_AS6_forgotPinKeepsTheEditedCatalogue`() {
        // Given
        seeds.session("last", departure = "DOH", destination = "SLL", createdAt = CREATED_AT)
        seeds.note("last", competencyId = 1, text = "Late gear")
        execSql("UPDATE competency SET name = ? WHERE id = 1", RENAMED_COMPETENCY)
        execSql("INSERT INTO competency (name, sortIndex) VALUES (?, 9)", ADDED_COMPETENCY)
        givenPinIsSet(PIN)
        tapOpenSession()
        composeRule.waitUntilExactlyOneExists(hasText(ENTER_PIN), TIMEOUT_MS)
        tapForgotPin()
        composeRule.waitUntilExactlyOneExists(forgotPinWarning(), TIMEOUT_MS)

        // When
        composeRule.onNode(inPopUp() and hasText(DELETE_ALL_SESSIONS) and hasClickAction()).performClick()
        composeRule.waitUntilExactlyOneExists(hasText(SET_A_PIN) and !inPopUp(), TIMEOUT_MS)

        // Then
        backToHome()
        startNewSession(departure = "MCT", destination = "DOH")
        composeRule.waitUntilExactlyOneExists(hasText(RENAMED_COMPETENCY), TIMEOUT_MS)
        composeRule.waitUntilExactlyOneExists(hasText(ADDED_COMPETENCY), TIMEOUT_MS)
        composeRule.onNode(hasText("Competency 1")).assertDoesNotExist()
        composeRule.onNode(hasText("Competency 2")).assertExists()
    }

    // Home and Settings

    private fun tapOpenSession() {
        composeRule.waitUntilAtLeastOneExists(hasText(OPEN_SESSION, ignoreCase = true) and hasClickAction(), TIMEOUT_MS)
        composeRule.onNode(hasText(OPEN_SESSION, ignoreCase = true) and hasClickAction()).performClick()
    }

    /** Presses back until the home screen shows. */
    private fun backToHome() {
        val home = hasText(OPEN_SESSION, ignoreCase = true) and hasClickAction()
        repeat(MAX_BACKS) {
            composeRule.waitForIdle()
            if (composeRule.onAllNodes(home).fetchSemanticsNodes().isNotEmpty()) return
            harness.pressBack()
        }
        composeRule.waitUntilExactlyOneExists(home, TIMEOUT_MS)
    }

    private fun openChangePin() {
        val gear = hasContentDescription(SETTINGS) and hasClickAction() and !inPopUp()
        composeRule.waitUntilExactlyOneExists(gear, TIMEOUT_MS)
        composeRule.onNode(gear).performClick()
        waitForSettings()
        composeRule.onNode(changePinEntry()).performClick()
    }

    private fun changePinEntry(): SemanticsMatcher = hasText(CHANGE_PIN) and hasClickAction() and !inPopUp()

    private fun waitForSettings() {
        composeRule.waitUntilExactlyOneExists(hasText(SETTINGS) and !hasClickAction() and !inPopUp(), TIMEOUT_MS)
        composeRule.waitUntilExactlyOneExists(changePinEntry(), TIMEOUT_MS)
        composeRule.waitUntilDoesNotExist(confirmKey(), TIMEOUT_MS)
    }

    /** Opens the setup screen from home and starts a Session with one Crew Member on the route given. */
    private fun startNewSession(
        departure: String,
        destination: String,
    ) {
        composeRule.waitUntilExactlyOneExists(hasText(NEW_SESSION, ignoreCase = true) and hasClickAction(), TIMEOUT_MS)
        composeRule.onNode(hasText(NEW_SESSION, ignoreCase = true) and hasClickAction()).performClick()
        composeRule.waitUntilAtLeastOneExists(hasText(START), TIMEOUT_MS)
        field("CM1").performTextInput("79125")
        field("From").performTextInput(departure)
        field("To").performTextInput(destination)
        composeRule.onNode(hasText(START) and hasClickAction()).performClick()
        composeRule.waitUntilExactlyOneExists(route(departure, destination), TIMEOUT_MS)
    }

    /** The setup screen's text field labelled [label]: "CM1", "CM2", "From" or "To". */
    private fun field(label: String): SemanticsNodeInteraction =
        composeRule.onNode(hasSetTextAction() and (hasText(label) or hasContentDescription(label)))

    // PIN

    private fun digitKey(digit: Char): SemanticsMatcher = hasText(digit.toString()) and hasClickAction() and !inPopUp()

    private fun confirmKey(): SemanticsMatcher = hasContentDescription(CONFIRM) and hasClickAction() and !inPopUp()

    private fun waitForKeypadUnder(text: String) {
        composeRule.waitUntilExactlyOneExists(confirmKey(), TIMEOUT_MS)
        composeRule.waitUntilAtLeastOneExists(hasText(text, substring = true, ignoreCase = true), TIMEOUT_MS)
    }

    /** Types [pin] on the keypad and taps its ✓. */
    private fun enterPin(pin: String) {
        pin.forEach { digit ->
            composeRule.waitUntilExactlyOneExists(digitKey(digit), TIMEOUT_MS)
            composeRule.onNode(digitKey(digit)).performClick()
        }
        composeRule.onNode(confirmKey()).performClick()
        composeRule.waitForIdle()
    }

    /** Taps [pin]'s digits and the ✓ wherever the keypad shows them, enabled or not. */
    private fun tryToEnterPin(pin: String) {
        pin.forEach { digit ->
            if (composeRule.onAllNodes(digitKey(digit)).fetchSemanticsNodes().isNotEmpty()) {
                composeRule.onNode(digitKey(digit)).performClick()
            }
        }
        if (composeRule.onAllNodes(confirmKey()).fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNode(confirmKey()).performClick()
        }
        composeRule.waitForIdle()
    }

    /** No digit key and no ✓ can be used. */
    private fun assertKeypadRefusesEntry() {
        val enabledKeys =
            ('0'..'9').flatMap { composeRule.onAllNodes(digitKey(it) and isEnabled()).fetchSemanticsNodes() } +
                composeRule.onAllNodes(confirmKey() and isEnabled()).fetchSemanticsNodes()
        assertTrue("No key may be used during the lockout, but ${enabledKeys.size} can", enabledKeys.isEmpty())
    }

    /** Sets [pin] through the PIN screens, then restarts the app on its home screen. */
    private fun givenPinIsSet(pin: String) {
        harness.launch()
        tapOpenSession()
        composeRule.waitUntilExactlyOneExists(hasText(SET_A_PIN), TIMEOUT_MS)
        enterPin(pin)
        composeRule.waitUntilAtLeastOneExists(hasText(AGAIN, substring = true, ignoreCase = true), TIMEOUT_MS)
        enterPin(pin)
        waitForSessionList()
        harness.restart()
    }

    private fun tapForgotPin() {
        val forgot = hasText(FORGOT_PIN) and hasClickAction() and !inPopUp()
        composeRule.waitUntilExactlyOneExists(forgot, TIMEOUT_MS)
        composeRule.onNode(forgot).performClick()
    }

    private fun forgotPinWarning(): SemanticsMatcher =
        inPopUp() and
            hasText(ALL_SAVED_SESSIONS, substring = true, ignoreCase = true) and
            hasText(DELETE, substring = true, ignoreCase = true)

    // Session list

    private fun sessionListTitle(): SemanticsMatcher = hasText(SESSIONS) and !inPopUp()

    private fun waitForSessionList() {
        composeRule.waitUntilExactlyOneExists(sessionListTitle(), TIMEOUT_MS)
    }

    private fun route(
        departure: String,
        destination: String,
    ): SemanticsMatcher = hasText("$departure → $destination") and !inPopUp()

    /** The routes of the listed Sessions, top to bottom. */
    private fun routesShown(): List<String> =
        composeRule
            .onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text) and !inPopUp(), useUnmergedTree = true)
            .fetchSemanticsNodes()
            .filter { node -> node.texts().any { ROUTE.matches(it) } }
            .sortedBy { it.positionInRoot.y }
            .map { node -> node.texts().first { ROUTE.matches(it) } }

    // Shared

    private fun inPopUp(): SemanticsMatcher = hasAnyAncestor(isDialog() or isPopup())

    private fun SemanticsNode.texts(): List<String> =
        config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text }

    private fun execSql(
        sql: String,
        vararg args: Any?,
    ) {
        harness
            .get<CheckPilotDatabase>()
            .openHelper.writableDatabase
            .execSQL(sql, args)
    }

    private companion object {
        const val TIMEOUT_MS = 5000L
        const val MAX_BACKS = 5
        const val PIN = "2468"
        const val WRONG_PIN = "2469"
        const val NEW_PIN_DIGITS = "975724"
        const val OPEN_SESSION = "Open Session"
        const val NEW_SESSION = "New Session"
        const val START = "Start"
        const val CONTINUE = "Continue last Session"
        const val SETTINGS = "Settings"
        const val CHANGE_PIN = "Change PIN"
        const val CURRENT_PIN = "current PIN"
        const val NEW_PIN = "new PIN"
        const val SET_A_PIN = "Set a PIN"
        const val ENTER_PIN = "Enter PIN"
        const val AGAIN = "again"
        const val CONFIRM = "Confirm"
        const val PIN_IS_WRONG = "PIN is wrong"
        const val SECONDS = "seconds"
        const val WAIT_30_SECONDS = "Wait 30 seconds"
        const val FORGOT_PIN = "Forgot PIN"
        const val ALL_SAVED_SESSIONS = "all saved Sessions"
        const val DELETE = "delete"
        const val DELETE_ALL_SESSIONS = "Delete all Sessions"
        const val CANCEL = "Cancel"
        const val SESSIONS = "Sessions"
        const val RENAMED_COMPETENCY = "Application of Procedures"
        const val ADDED_COMPETENCY = "Workload Management"
        val CREATED_AT: Instant = Instant.parse("2026-11-01T08:00:00Z")
        val ROUTE = Regex("""[A-Z]{3} → [A-Z]{3}""")
    }
}
