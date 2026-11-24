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
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mebiad.checkpilot.testutils.AppHarness
import com.mebiad.checkpilot.testutils.SessionSeeds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * US-7: Find and open a saved Session behind the PIN. Each test seeds Sessions, and Grades in their rows, then taps
 * Open Session on the home screen, sets or enters the PIN on the keypad, and works the Session list: search, the date
 * filter, Capture, Debrief, the ⋮ menu's Delete, back to the home screen and sending the app to the background.
 *
 * How the tests find things, so restyling doesn't break them:
 * - The PIN screens are told apart by their titles: "Set a PIN" with no PIN set, "Enter PIN" once one is. Setting a
 *   PIN asks for it a second time under a text containing "again".
 * - The keypad's digits are the clickable nodes whose text is the digit, such as "7"; its ✓ is the clickable node
 *   described "Confirm".
 * - The Session list is showing while the text "Sessions" stands alone outside pop-ups. Its search field is the one
 *   text field on it. Its date filter is the clickable node described "Filter by date"; once a date is picked, the
 *   clickable node described "Clear the date" clears it.
 * - Each Session's row is the band from its route, such as "DOH → SLL", down to the next route. In it: the date, each
 *   Staff Number, each Crew Member's Grade count as "<graded>/<total>" right of their Staff Number, the clickable
 *   "Capture" and "Debrief", and the ⋮ described "More options", whose menu holds a clickable "Delete".
 * - Deleting asks "Delete this Session?" in a dialog with a clickable "Delete" and "Cancel".
 * - The capture screen is showing while its "Generate Debrief" button is; the Debrief while its title "Debrief" stands
 *   alone, not clickable. Both show their Session's route.
 * - A PIN is set before a test through the PIN screens themselves, and the app is then restarted, so that the Session
 *   list starts locked whatever unlocks it in memory.
 *
 * The screen is tall so every row of the Session list is laid out at once, whatever list it uses.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h3000dp")
@OptIn(ExperimentalTestApi::class)
class US7FindAndOpenASavedSessionBehindThePinTest {
    private val harness = AppHarness()
    private val composeRule = createEmptyComposeRule()
    private val seeds = SessionSeeds(harness)

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(harness).around(composeRule)

    /**
     * 001-mvp/US-7/AS1: **Given** no PIN has been set, **when** the Assessor taps Open session, **then** the app asks
     * for a new 4–6 digit PIN, typed twice, and opens the Session list once both match.
     */
    @Test
    fun `001_mvp_US_7_AS1_withoutAPinOpenSessionSetsANewPinTypedTwiceThenOpensTheList`() {
        // Given
        seeds.session("old", departure = "DOH", destination = "LHR", createdAt = CREATED_AT)
        seeds.session("last", departure = "DOH", destination = "SLL", createdAt = CREATED_AT.plusSeconds(3600))
        harness.launch()

        // When
        tapOpenSession()

        // Then
        composeRule.waitUntilExactlyOneExists(hasText(SET_A_PIN), TIMEOUT_MS)
        enterPin(PIN)
        composeRule.waitUntilAtLeastOneExists(hasText(AGAIN, substring = true), TIMEOUT_MS)
        composeRule.onNode(sessionListTitle()).assertDoesNotExist()
        enterPin(PIN)
        waitForSessionList()
        composeRule.waitUntilExactlyOneExists(route("DOH", "LHR"), TIMEOUT_MS)
    }

    /**
     * 001-mvp/US-7/AS2: **Given** the two PIN entries differ, **when** the Assessor confirms, **then** a message says
     * they don't match and no PIN is set.
     */
    @Test
    fun `001_mvp_US_7_AS2_twoDifferentPinEntriesSayTheyDontMatchAndSetNoPin`() {
        // Given
        seeds.session("last", createdAt = CREATED_AT)
        harness.launch()
        tapOpenSession()
        composeRule.waitUntilExactlyOneExists(hasText(SET_A_PIN), TIMEOUT_MS)
        enterPin(PIN)
        composeRule.waitUntilAtLeastOneExists(hasText(AGAIN, substring = true), TIMEOUT_MS)

        // When
        enterPin(WRONG_PIN)

        // Then
        composeRule.waitUntilAtLeastOneExists(hasText(DONT_MATCH, substring = true, ignoreCase = true), TIMEOUT_MS)
        composeRule.onNode(sessionListTitle()).assertDoesNotExist()
        assertNoPinIsSet()
    }

    /**
     * 001-mvp/US-7/AS3: **Given** a new PIN of 3 or of 7 digits, **when** the Assessor confirms, **then** a message
     * says a PIN has 4 to 6 digits and no PIN is set.
     */
    @Test
    fun `001_mvp_US_7_AS3_aNewPinOf3Or7DigitsSaysAPinHas4To6DigitsAndSetsNoPin`() {
        // Given
        seeds.session("last", createdAt = CREATED_AT)
        harness.launch()

        // When / Then: a PIN of 3 digits
        tapOpenSession()
        composeRule.waitUntilExactlyOneExists(hasText(SET_A_PIN), TIMEOUT_MS)
        enterPin("123")
        composeRule.waitUntilAtLeastOneExists(hasText(FOUR_TO_SIX_DIGITS, substring = true), TIMEOUT_MS)
        composeRule.onNode(hasText(AGAIN, substring = true)).assertDoesNotExist()
        composeRule.onNode(sessionListTitle()).assertDoesNotExist()
        assertNoPinIsSet()

        // When / Then: a PIN of 7 digits
        enterPin("1234567")
        composeRule.waitUntilAtLeastOneExists(hasText(FOUR_TO_SIX_DIGITS, substring = true), TIMEOUT_MS)
        composeRule.onNode(hasText(AGAIN, substring = true)).assertDoesNotExist()
        composeRule.onNode(sessionListTitle()).assertDoesNotExist()
        assertNoPinIsSet()
    }

    /**
     * 001-mvp/US-7/AS4: **Given** a PIN is set, **when** the Assessor taps Open session and enters it correctly,
     * **then** the Session list opens.
     */
    @Test
    fun `001_mvp_US_7_AS4_theCorrectPinOpensTheSessionList`() {
        // Given
        seeds.session("old", departure = "DOH", destination = "LHR", createdAt = CREATED_AT)
        seeds.session("last", departure = "DOH", destination = "SLL", createdAt = CREATED_AT.plusSeconds(3600))
        givenPinIsSet(PIN)

        // When
        tapOpenSession()
        composeRule.waitUntilExactlyOneExists(hasText(ENTER_PIN), TIMEOUT_MS)
        enterPin(PIN)

        // Then
        waitForSessionList()
        composeRule.waitUntilExactlyOneExists(route("DOH", "LHR"), TIMEOUT_MS)
    }

    /**
     * 001-mvp/US-7/AS5: **Given** a PIN is set, **when** the Assessor enters a wrong PIN, **then** a message says the
     * PIN is wrong and the Session list stays closed.
     */
    @Test
    fun `001_mvp_US_7_AS5_aWrongPinSaysSoAndKeepsTheListClosed`() {
        // Given
        seeds.session("old", departure = "DOH", destination = "LHR", createdAt = CREATED_AT)
        seeds.session("last", departure = "DOH", destination = "SLL", createdAt = CREATED_AT.plusSeconds(3600))
        givenPinIsSet(PIN)
        tapOpenSession()
        composeRule.waitUntilExactlyOneExists(hasText(ENTER_PIN), TIMEOUT_MS)

        // When
        enterPin(WRONG_PIN)

        // Then
        composeRule.waitUntilAtLeastOneExists(hasText(PIN_IS_WRONG, substring = true, ignoreCase = true), TIMEOUT_MS)
        composeRule.onNode(sessionListTitle()).assertDoesNotExist()
        composeRule.onNode(route("DOH", "LHR")).assertDoesNotExist()
        composeRule.onNode(hasText(ENTER_PIN)).assertExists()
    }

    /**
     * 001-mvp/US-7/AS6: **Given** the Session list was unlocked, **when** the Assessor opens a Session and goes back to
     * the list, **then** the list shows without asking for the PIN.
     */
    @Test
    fun `001_mvp_US_7_AS6_backFromAnOpenedSessionShowsTheListWithoutThePin`() {
        // Given
        seeds.session("old", departure = "DOH", destination = "LHR", createdAt = CREATED_AT)
        seeds.session("last", departure = "DOH", destination = "SLL", createdAt = CREATED_AT.plusSeconds(3600))
        harness.launch()
        unlockSessionListWithANewPin()

        // When
        tapInRow(route("DOH", "LHR"), CAPTURE)
        waitForCapture(route("DOH", "LHR"))
        harness.pressBack()

        // Then
        waitForSessionList()
        composeRule.waitUntilExactlyOneExists(route("DOH", "LHR"), TIMEOUT_MS)
        composeRule.onNode(hasText(ENTER_PIN)).assertDoesNotExist()
    }

    /**
     * 001-mvp/US-7/AS7: **Given** the Session list was unlocked, **when** the Assessor goes back to the home screen and
     * taps Open session, **then** the PIN is asked again.
     */
    @Test
    fun `001_mvp_US_7_AS7_backToHomeLocksTheListAgain`() {
        // Given
        seeds.session("last", createdAt = CREATED_AT)
        harness.launch()
        unlockSessionListWithANewPin()

        // When
        harness.pressBack()
        tapOpenSession()

        // Then
        composeRule.waitUntilExactlyOneExists(hasText(ENTER_PIN), TIMEOUT_MS)
        composeRule.onNode(sessionListTitle()).assertDoesNotExist()
    }

    /**
     * 001-mvp/US-7/AS8: **Given** the Session list was unlocked, **when** the Assessor switches to another app and comes
     * back, **then** the PIN is asked again before the list shows.
     */
    @Test
    fun `001_mvp_US_7_AS8_comingBackFromAnotherAppAsksThePinBeforeTheList`() {
        // Given
        seeds.session("old", departure = "DOH", destination = "LHR", createdAt = CREATED_AT)
        seeds.session("last", departure = "DOH", destination = "SLL", createdAt = CREATED_AT.plusSeconds(3600))
        harness.launch()
        unlockSessionListWithANewPin()

        // When
        harness.sendToBackground()
        harness.bringToForeground()

        // Then
        composeRule.waitUntilExactlyOneExists(hasText(ENTER_PIN), TIMEOUT_MS)
        composeRule.onNode(sessionListTitle()).assertDoesNotExist()
        composeRule.onNode(route("DOH", "LHR")).assertDoesNotExist()
        enterPin(PIN)
        waitForSessionList()
        composeRule.waitUntilExactlyOneExists(route("DOH", "LHR"), TIMEOUT_MS)
    }

    /**
     * 001-mvp/US-7/AS9: **Given** several Sessions, **when** the Session list opens, **then** they are listed newest
     * first, each row showing route, date, Staff Numbers, and how many Competencies have a Grade for each Crew Member.
     *
     * The Sessions are seeded oldest first, and each one is both dated and created after the one before.
     */
    @Test
    fun `001_mvp_US_7_AS9_theListShowsSessionsNewestFirstWithRouteDateStaffNumbersAndGradeCounts`() {
        // Given
        seeds.session(
            "oldest",
            cm1 = null,
            cm2 = "66021",
            departure = "MCT",
            destination = "DOH",
            date = LocalDate.of(2026, 10, 19),
            createdAt = CREATED_AT
        )
        (1L..7L).forEach { seeds.grade("oldest", competencyId = it, crewMember = "CM2", value = "EFFECTIVE") }
        seeds.session(
            "middle",
            cm1 = "81530",
            cm2 = null,
            departure = "DOH",
            destination = "LHR",
            date = LocalDate.of(2026, 10, 28),
            createdAt = CREATED_AT.plusSeconds(3600)
        )
        seeds.session(
            "newest",
            cm1 = "79125",
            cm2 = "04512",
            departure = "DOH",
            destination = "SLL",
            date = LocalDate.of(2026, 11, 1),
            createdAt = CREATED_AT.plusSeconds(7200)
        )
        (1L..3L).forEach { seeds.grade("newest", competencyId = it, crewMember = "CM1", value = "ADEQUATE") }
        (1L..9L).forEach { seeds.grade("newest", competencyId = it, crewMember = "CM2", value = "NOT_OBSERVED") }
        harness.launch()

        // When
        unlockSessionListWithANewPin()

        // Then
        composeRule.waitUntil(TIMEOUT_MS) { routesShown().size == 3 }
        assertEquals(listOf("DOH → SLL", "DOH → LHR", "MCT → DOH"), routesShown())

        val newest = rowTexts(route("DOH", "SLL"))
        assertTrue("the newest row shows its date: $newest", newest.any { "01 Nov 2026" in it })
        assertEquals("3/9", gradeCountRightOf(route("DOH", "SLL"), "79125"))
        assertEquals("9/9", gradeCountRightOf(route("DOH", "SLL"), "04512"))

        val middle = rowTexts(route("DOH", "LHR"))
        assertTrue("the middle row shows its date: $middle", middle.any { "28 Oct 2026" in it })
        assertEquals("0/9", gradeCountRightOf(route("DOH", "LHR"), "81530"))

        val oldest = rowTexts(route("MCT", "DOH"))
        assertTrue("the oldest row shows its date: $oldest", oldest.any { "19 Oct 2026" in it })
        assertEquals("7/9", gradeCountRightOf(route("MCT", "DOH"), "66021"))
    }

    /**
     * 001-mvp/US-7/AS10: **Given** a Session started with Staff Number 04512, **when** the Session list shows it,
     * **then** the row shows "04512".
     */
    @Test
    fun `001_mvp_US_7_AS10_aStaffNumberWithALeadingZeroShowsAsTyped`() {
        // Given
        seeds.session("old", cm1 = "04512", cm2 = null, departure = "DOH", destination = "LHR", createdAt = CREATED_AT)
        seeds.session(
            "last",
            cm1 = "79125",
            cm2 = null,
            departure = "DOH",
            destination = "SLL",
            createdAt = CREATED_AT.plusSeconds(3600)
        )
        harness.launch()

        // When
        unlockSessionListWithANewPin()

        // Then
        composeRule.waitUntilExactlyOneExists(route("DOH", "LHR"), TIMEOUT_MS)
        val row = rowTexts(route("DOH", "LHR"))
        assertTrue("the row shows 04512: $row", row.any { Regex("""(^|\D)04512(\D|$)""").containsMatchIn(it) })
    }

    /**
     * 001-mvp/US-7/AS11: **Given** Sessions with different Staff Numbers, **when** the Assessor searches for 79125,
     * **then** only Sessions with Staff Number 79125 are listed.
     *
     * 79125 is CM1 in one Session and CM2 in another.
     */
    @Test
    fun `001_mvp_US_7_AS11_searchingAStaffNumberListsOnlyItsSessions`() {
        // Given
        seeds.session(
            "cm1",
            cm1 = "79125",
            cm2 = "04512",
            departure = "DOH",
            destination = "SLL",
            createdAt = CREATED_AT
        )
        seeds.session(
            "cm2",
            cm1 = "66021",
            cm2 = "79125",
            departure = "MCT",
            destination = "DOH",
            createdAt = CREATED_AT.plusSeconds(3600)
        )
        seeds.session(
            "other",
            cm1 = "81530",
            cm2 = "04512",
            departure = "DOH",
            destination = "LHR",
            createdAt = CREATED_AT.plusSeconds(7200)
        )
        harness.launch()
        unlockSessionListWithANewPin()
        composeRule.waitUntil(TIMEOUT_MS) { routesShown().size == 3 }

        // When
        searchField().performTextInput("79125")

        // Then
        composeRule.waitUntil(TIMEOUT_MS) { routesShown().size == 2 }
        assertEquals(setOf("DOH → SLL", "MCT → DOH"), routesShown().toSet())
    }

    /**
     * 001-mvp/US-7/AS12: **Given** Sessions on different routes, **when** the Assessor searches for SLL, **then** only
     * Sessions departing from or arriving at SLL are listed.
     */
    @Test
    fun `001_mvp_US_7_AS12_searchingAnAirportListsOnlySessionsFromOrToIt`() {
        // Given
        seeds.session("to", departure = "DOH", destination = "SLL", createdAt = CREATED_AT)
        seeds.session("from", departure = "SLL", destination = "DOH", createdAt = CREATED_AT.plusSeconds(3600))
        seeds.session("neither", departure = "DOH", destination = "LHR", createdAt = CREATED_AT.plusSeconds(7200))
        seeds.session("nor", departure = "MCT", destination = "DOH", createdAt = CREATED_AT.plusSeconds(10800))
        harness.launch()
        unlockSessionListWithANewPin()
        composeRule.waitUntil(TIMEOUT_MS) { routesShown().size == 4 }

        // When
        searchField().performTextInput("SLL")

        // Then
        composeRule.waitUntil(TIMEOUT_MS) { routesShown().size == 2 }
        assertEquals(setOf("DOH → SLL", "SLL → DOH"), routesShown().toSet())
    }

    /**
     * 001-mvp/US-7/AS13: **Given** Sessions on different dates, **when** the Assessor picks 08 Nov 2026 in the list's
     * date picker, **then** only Sessions dated 08 Nov 2026 are listed, and clearing the date lists all of them again.
     */
    @Test
    fun `001_mvp_US_7_AS13_pickingADateListsOnlyThatDateAndClearingItListsAllAgain`() {
        // Given
        harness.clock.setToday(LocalDate.of(2026, 11, 21))
        seeds.session(
            "first",
            departure = "DOH",
            destination = "SLL",
            date = LocalDate.of(2026, 11, 1),
            createdAt = CREATED_AT
        )
        seeds.session(
            "eighth",
            departure = "SLL",
            destination = "DOH",
            date = LocalDate.of(2026, 11, 8),
            createdAt = CREATED_AT.plusSeconds(3600)
        )
        seeds.session(
            "eighth-too",
            departure = "MCT",
            destination = "DOH",
            date = LocalDate.of(2026, 11, 8),
            createdAt = CREATED_AT.plusSeconds(7200)
        )
        seeds.session(
            "ninth",
            departure = "DOH",
            destination = "LHR",
            date = LocalDate.of(2026, 11, 9),
            createdAt = CREATED_AT.plusSeconds(10800)
        )
        harness.launch()
        unlockSessionListWithANewPin()
        composeRule.waitUntil(TIMEOUT_MS) { routesShown().size == 4 }

        // When
        composeRule.onNode(hasContentDescription(FILTER_BY_DATE) and hasClickAction() and !inPopUp()).performClick()
        pickInDatePicker(LocalDate.of(2026, 11, 8))

        // Then
        composeRule.waitUntil(TIMEOUT_MS) { routesShown().size == 2 }
        assertEquals(setOf("SLL → DOH", "MCT → DOH"), routesShown().toSet())

        // When / Then: the date is cleared
        composeRule.waitUntilExactlyOneExists(clearDate(), TIMEOUT_MS)
        composeRule.onNode(clearDate()).performClick()
        composeRule.waitUntil(TIMEOUT_MS) { routesShown().size == 4 }
    }

    /**
     * 001-mvp/US-7/AS14: **Given** a Session in the list, **when** the Assessor opens it for capture or for the
     * Debrief, **then** the capture screen or the Debrief of that Session opens.
     *
     * The Session opened isn't the Last Session, which Continue last Session would open.
     */
    @Test
    fun `001_mvp_US_7_AS14_aSessionOpensFromTheListForCaptureOrForTheDebrief`() {
        // Given
        seeds.session("old", cm2 = null, departure = "DOH", destination = "LHR", createdAt = CREATED_AT)
        seeds.session("last", departure = "DOH", destination = "SLL", createdAt = CREATED_AT.plusSeconds(3600))
        harness.launch()
        unlockSessionListWithANewPin()

        // When / Then: Capture
        tapInRow(route("DOH", "LHR"), CAPTURE)
        waitForCapture(route("DOH", "LHR"))
        composeRule.onNode(route("DOH", "SLL")).assertDoesNotExist()

        // When / Then: Debrief
        harness.pressBack()
        waitForSessionList()
        tapInRow(route("DOH", "LHR"), DEBRIEF)
        composeRule.waitUntilExactlyOneExists(debriefTitle(), TIMEOUT_MS)
        composeRule.waitUntilExactlyOneExists(route("DOH", "LHR"), TIMEOUT_MS)
        composeRule.onNode(route("DOH", "SLL")).assertDoesNotExist()
        composeRule.onNode(sessionListTitle()).assertDoesNotExist()
    }

    /**
     * 001-mvp/US-7/AS15: **Given** a Session in the list, **when** the Assessor deletes it, **then** a confirmation asks
     * first; confirming removes it from the list, cancelling keeps it.
     */
    @Test
    fun `001_mvp_US_7_AS15_deletingAsksFirstConfirmingRemovesCancellingKeeps`() {
        // Given
        seeds.session("old", departure = "DOH", destination = "LHR", createdAt = CREATED_AT)
        seeds.session("last", departure = "DOH", destination = "SLL", createdAt = CREATED_AT.plusSeconds(3600))
        harness.launch()
        unlockSessionListWithANewPin()
        composeRule.waitUntil(TIMEOUT_MS) { routesShown().size == 2 }

        // When / Then: Delete, then Cancel
        tapDeleteInMenuOf(route("DOH", "LHR"))
        composeRule.waitUntilExactlyOneExists(inPopUp() and hasText(DELETE_THIS_SESSION), TIMEOUT_MS)
        assertEquals(setOf("DOH → SLL", "DOH → LHR"), routesShown().toSet())
        composeRule.onNode(inPopUp() and hasText(CANCEL) and hasClickAction()).performClick()
        composeRule.waitUntilDoesNotExist(inPopUp() and hasText(DELETE_THIS_SESSION), TIMEOUT_MS)
        assertEquals(setOf("DOH → SLL", "DOH → LHR"), routesShown().toSet())

        // When / Then: Delete, then confirm
        tapDeleteInMenuOf(route("DOH", "LHR"))
        composeRule.waitUntilExactlyOneExists(inPopUp() and hasText(DELETE_THIS_SESSION), TIMEOUT_MS)
        composeRule.onNode(inPopUp() and hasText(DELETE) and hasClickAction()).performClick()
        composeRule.waitUntilDoesNotExist(inPopUp() and hasText(DELETE_THIS_SESSION), TIMEOUT_MS)
        composeRule.waitUntil(TIMEOUT_MS) { routesShown() == listOf("DOH → SLL") }
    }

    /**
     * 001-mvp/US-7/AS16: **Given** Session A was created before Session B, **when** the Assessor edits A through the
     * Session list and then taps Continue last Session on the home screen, **then** Session B opens.
     */
    @Test
    fun `001_mvp_US_7_AS16_editingAnOlderSessionLeavesTheLastSessionAsItWas`() {
        // Given
        seeds.session("a", departure = "DOH", destination = "LHR", createdAt = CREATED_AT)
        seeds.session("b", departure = "DOH", destination = "SLL", createdAt = CREATED_AT.plusSeconds(3600))
        harness.launch()
        unlockSessionListWithANewPin()

        // When
        tapInRow(route("DOH", "LHR"), CAPTURE)
        waitForCapture(route("DOH", "LHR"))
        val field = composeRule.onAllNodesWithTag("note-field-1")[0]
        field.performScrollTo().performClick()
        field.performTextInput("Late gear")
        composeRule.waitUntilAtLeastOneExists(hasTestTag("note-field-1") and hasText("Late gear"), TIMEOUT_MS)
        harness.pressBack()
        waitForSessionList()
        harness.pressBack()
        composeRule.waitUntilAtLeastOneExists(hasText(CONTINUE, ignoreCase = true), TIMEOUT_MS)
        composeRule.onNodeWithText(CONTINUE, ignoreCase = true).performClick()

        // Then
        waitForCapture(route("DOH", "SLL"))
        composeRule.onNode(route("DOH", "LHR")).assertDoesNotExist()
    }

    // Home and PIN

    private fun tapOpenSession() {
        composeRule.waitUntilAtLeastOneExists(hasText(OPEN_SESSION, ignoreCase = true) and hasClickAction(), TIMEOUT_MS)
        composeRule.onNode(hasText(OPEN_SESSION, ignoreCase = true) and hasClickAction()).performClick()
    }

    /** Types [pin] on the keypad and taps its ✓. */
    private fun enterPin(pin: String) {
        pin.forEach { digit ->
            val key = hasText(digit.toString()) and hasClickAction() and !inPopUp()
            composeRule.waitUntilExactlyOneExists(key, TIMEOUT_MS)
            composeRule.onNode(key).performClick()
        }
        composeRule.onNode(hasContentDescription(CONFIRM) and hasClickAction() and !inPopUp()).performClick()
        composeRule.waitForIdle()
    }

    /** From the home screen with no PIN set: Open Session, the new PIN typed twice, and the Session list. */
    private fun unlockSessionListWithANewPin() {
        tapOpenSession()
        composeRule.waitUntilExactlyOneExists(hasText(SET_A_PIN), TIMEOUT_MS)
        enterPin(PIN)
        composeRule.waitUntilAtLeastOneExists(hasText(AGAIN, substring = true), TIMEOUT_MS)
        enterPin(PIN)
        waitForSessionList()
    }

    /** Sets [pin] through the PIN screens, then restarts the app on its home screen. */
    private fun givenPinIsSet(pin: String) {
        harness.launch()
        tapOpenSession()
        composeRule.waitUntilExactlyOneExists(hasText(SET_A_PIN), TIMEOUT_MS)
        enterPin(pin)
        composeRule.waitUntilAtLeastOneExists(hasText(AGAIN, substring = true), TIMEOUT_MS)
        enterPin(pin)
        waitForSessionList()
        harness.restart()
    }

    /** Goes back to the home screen, taps Open Session, and checks the app asks to set a PIN, not to enter one. */
    private fun assertNoPinIsSet() {
        harness.pressBack()
        tapOpenSession()
        composeRule.waitUntilExactlyOneExists(hasText(SET_A_PIN), TIMEOUT_MS)
        composeRule.onNode(hasText(ENTER_PIN)).assertDoesNotExist()
    }

    // Session list

    private fun sessionListTitle(): SemanticsMatcher = hasText(SESSIONS) and !inPopUp()

    private fun waitForSessionList() {
        composeRule.waitUntilExactlyOneExists(sessionListTitle(), TIMEOUT_MS)
    }

    private fun searchField(): SemanticsNodeInteraction = composeRule.onNode(hasSetTextAction() and !inPopUp())

    private fun clearDate(): SemanticsMatcher = hasContentDescription(CLEAR_DATE) and hasClickAction() and !inPopUp()

    private fun route(
        departure: String,
        destination: String,
    ): SemanticsMatcher = hasText("$departure → $destination") and !inPopUp()

    /** The routes of the listed Sessions, top to bottom. */
    private fun routesShown(): List<String> = routeNodes().map { node -> node.texts().first { ROUTE.matches(it) } }

    private fun routeNodes(): List<SemanticsNode> =
        composeRule
            .onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text) and !inPopUp(), useUnmergedTree = true)
            .fetchSemanticsNodes()
            .filter { node -> node.texts().any { ROUTE.matches(it) } }
            .sortedBy { it.top }

    /** The vertical band of the list that belongs to the row with [route]: from its route to the next route. */
    private fun bandOf(route: SemanticsMatcher): ClosedFloatingPointRange<Float> {
        composeRule.waitUntilExactlyOneExists(route, TIMEOUT_MS)
        val routes = routeNodes()
        val index = routes.indexOfFirst { route.matches(it) }
        check(index >= 0) { "No row with $route; the list shows ${routesShown()}" }
        val top = routes[index].top
        val bottom = routes.getOrNull(index + 1)?.top ?: Float.MAX_VALUE
        return top..bottom
    }

    /** The texts in the row with [route], each text node on its own. */
    private fun rowTexts(route: SemanticsMatcher): List<String> = textNodesInRow(route).flatMap { it.texts() }

    private fun textNodesInRow(route: SemanticsMatcher): List<SemanticsNode> {
        val band = bandOf(route)
        return composeRule
            .onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text) and !inPopUp(), useUnmergedTree = true)
            .fetchSemanticsNodes()
            .filter { it.top >= band.start && it.top < band.endInclusive }
    }

    /**
     * The Grade count shown for the Crew Member with [staffNumber] in the row with [route]: the first "<n>/<total>" in
     * the Staff Number's own text after it, or else in the nearest text right of it on the same line.
     */
    private fun gradeCountRightOf(
        route: SemanticsMatcher,
        staffNumber: String,
    ): String? {
        val nodes = textNodesInRow(route)
        val own = nodes.firstOrNull { node -> node.texts().any { staffNumber in it } }
        checkNotNull(own) { "No $staffNumber in the row; it shows ${nodes.flatMap { it.texts() }}" }
        own
            .texts()
            .firstNotNullOfOrNull { text ->
                GRADE_COUNT.find(text.substringAfter(staffNumber, missingDelimiterValue = ""))
            }?.let { return it.value }
        val right = own.positionInRoot.x + own.size.width
        val ownTop = own.top
        val ownBottom = own.top + own.size.height
        return nodes
            .filter { it !== own && it.positionInRoot.x >= right }
            .filter { it.top < ownBottom && it.top + it.size.height > ownTop }
            .sortedBy { it.positionInRoot.x }
            .firstOrNull()
            ?.texts()
            ?.firstNotNullOfOrNull { GRADE_COUNT.find(it)?.value }
    }

    /** Taps the clickable [label], such as "Capture", in the row with [route]. */
    private fun tapInRow(
        route: SemanticsMatcher,
        label: String,
    ) {
        tapInBand(route, hasText(label) and hasClickAction())
    }

    private fun tapDeleteInMenuOf(route: SemanticsMatcher) {
        tapInBand(route, hasContentDescription(MORE_OPTIONS) and hasClickAction())
        val delete = inPopUp() and hasText(DELETE) and hasClickAction()
        composeRule.waitUntilExactlyOneExists(delete, TIMEOUT_MS)
        composeRule.onNode(delete).performClick()
    }

    private fun tapInBand(
        route: SemanticsMatcher,
        matcher: SemanticsMatcher,
    ) {
        waitForSessionList()
        val band = bandOf(route)
        val candidates = composeRule.onAllNodes(matcher and !inPopUp()).fetchSemanticsNodes()
        val index = candidates.indexOfFirst { it.top >= band.start && it.top < band.endInclusive }
        check(index >= 0) { "Nothing matching $matcher in the row with $route" }
        composeRule.onAllNodes(matcher and !inPopUp())[index].performClick()
    }

    /** Taps [date]'s day in the open Material 3 date picker, which describes each day like "Sunday, November 1, 2026". */
    private fun pickInDatePicker(date: LocalDate) {
        val description = date.format(DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.US))
        composeRule.waitUntilAtLeastOneExists(hasText(description, substring = true), TIMEOUT_MS)
        composeRule.onNode(hasText(description, substring = true)).performClick()
        composeRule.onNodeWithText("OK").performClick()
    }

    // Capture and Debrief

    private fun waitForCapture(route: SemanticsMatcher) {
        composeRule.waitUntilExactlyOneExists(
            hasText(GENERATE, substring = true, ignoreCase = true) and hasClickAction() and !inPopUp(),
            TIMEOUT_MS
        )
        composeRule.waitUntilExactlyOneExists(route, TIMEOUT_MS)
    }

    private fun debriefTitle(): SemanticsMatcher = hasText(DEBRIEF) and !hasClickAction() and !inPopUp()

    // Shared

    private fun inPopUp(): SemanticsMatcher = hasAnyAncestor(isDialog() or isPopup())

    private fun SemanticsNode.texts(): List<String> =
        config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text }

    // Unclipped, unlike boundsInRoot, so rows scrolled out of view still have their real place.
    private val SemanticsNode.top: Float get() = positionInRoot.y

    private companion object {
        const val TIMEOUT_MS = 5000L
        const val PIN = "1234"
        const val WRONG_PIN = "1235"
        const val OPEN_SESSION = "Open Session"
        const val CONTINUE = "Continue last Session"
        const val SET_A_PIN = "Set a PIN"
        const val ENTER_PIN = "Enter PIN"
        const val AGAIN = "again"
        const val CONFIRM = "Confirm"
        const val DONT_MATCH = "don't match"
        const val FOUR_TO_SIX_DIGITS = "4 to 6 digits"
        const val PIN_IS_WRONG = "PIN is wrong"
        const val SESSIONS = "Sessions"
        const val FILTER_BY_DATE = "Filter by date"
        const val CLEAR_DATE = "Clear the date"
        const val CAPTURE = "Capture"
        const val DEBRIEF = "Debrief"
        const val MORE_OPTIONS = "More options"
        const val DELETE = "Delete"
        const val CANCEL = "Cancel"
        const val DELETE_THIS_SESSION = "Delete this Session?"
        const val GENERATE = "Generate debrief"
        val CREATED_AT: Instant = Instant.parse("2026-11-01T08:00:00Z")
        val ROUTE = Regex("""[A-Z]{3} → [A-Z]{3}""")
        val GRADE_COUNT = Regex("""\d+/\d+""")
    }
}
