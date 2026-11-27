package com.mebiad.checkpilot.acceptance

import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mebiad.checkpilot.testutils.AppHarness
import com.mebiad.checkpilot.testutils.SessionListDriver
import com.mebiad.checkpilot.testutils.SessionSeeds
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * US-11: Change Staff Numbers after the start. Each test seeds a Session DOH → SLL with Notes and Grades in their
 * database rows, opens it with Continue last Session, and adds, corrects or removes a Staff Number on the capture
 * screen.
 *
 * How the tests find things, so restyling doesn't break them:
 * - The capture screen is showing while its "Generate debrief" button is.
 * - Each Staff Number shows on a clickable chip outside pop-ups, whose text contains the Staff Number, such as
 *   "04512". Tapping it opens the Edit Staff Number pop-up. With only one Crew Member, a clickable node outside pop-ups
 *   whose text contains "Add CM2" opens the same kind of pop-up for the empty slot.
 * - In that pop-up, the Staff Number is its editable field, "Save" keeps the change, "Cancel" closes it, and "Remove
 *   CM1" or "Remove CM2" asks to remove the Crew Member. A refusal shows in a pop-up, as the design draws it.
 * - Removing a Crew Member with Grades asks for confirmation in a pop-up that says "CM2's Grades will be deleted"
 *   (the design adds the count: "CM2's 4 Grades will be deleted"), with "Remove CM2 and Grades" and "Cancel".
 * - The CM Tag selector is the selectable nodes labelled CM1, CM2 and BOTH; the chips also say CM1 and CM2, but
 *   aren't selectable. Note fields carry the test tag `note-field-<competencyId>`.
 * - Generate debriefing asks which Crew Member with two of them: each choice is the clickable pop-up node whose text
 *   contains CM1 or CM2. The Debrief is showing while the text "Debrief" stands alone outside pop-ups. A Competency
 *   with Notes about the Crew Member has an eye icon described "Show Notes" that opens their Notes in a pop-up; the
 *   summary header shows the Grade count as "Grades 4/9" or "4 of 9 Grades set". The back arrow is described "Back",
 *   and leaving with ungraded Competencies asks to "Leave".
 * - The Session list, its rows and their texts as [SessionListDriver] describes them.
 *
 * The screen is tall so every row of the Session list and every Competency is laid out at once.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h3000dp")
@OptIn(ExperimentalTestApi::class)
class US11ChangeStaffNumbersAfterTheStartTest {
    private val harness = AppHarness()
    private val composeRule = createEmptyComposeRule()
    private val seeds = SessionSeeds(harness)
    private val list = SessionListDriver(composeRule)

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(harness).around(composeRule)

    /**
     * 001-mvp/US-11/AS1: **Given** a Session with only CM1 and a Note tagged BOTH, **when** the Assessor adds a CM2
     * Staff Number on the capture screen, **then** focusing a Note shows the CM Tag selector, and the BOTH Note appears
     * in the CM2 Debrief.
     */
    @Test
    fun `001_mvp_US_11_AS1_addingCm2ShowsTheCmTagSelectorAndTheBothNoteInTheCm2Debrief`() {
        // Given
        seeds.session(SESSION, cm1 = "79125", cm2 = null)
        seeds.note(SESSION, COMPETENCY_1, BOTH_NOTE, cmTag = "BOTH")
        harness.launch()
        openLastSession()

        // When
        addCm2("04512")

        // Then
        focus(noteField(COMPETENCY_1, index = 0))
        composeRule.waitUntilExactlyOneExists(cmTagOption(BOTH), TIMEOUT_MS)
        composeRule.onNode(cmTagOption(CM1)).assertExists()
        composeRule.onNode(cmTagOption(CM2)).assertExists()

        openDebrief(CM2)
        openFirstEyePopUp()
        composeRule.waitUntilExactlyOneExists(list.inPopUp() and hasText(BOTH_NOTE), TIMEOUT_MS)
    }

    /**
     * 001-mvp/US-11/AS2: **Given** a Session with Staff Number 79152, **when** the Assessor corrects it to 79125,
     * **then** the Session list shows 79125.
     */
    @Test
    fun `001_mvp_US_11_AS2_aCorrectedStaffNumberShowsInTheSessionList`() {
        // Given
        seeds.session(SESSION, cm1 = "79152", cm2 = "04512")
        harness.launch()
        openLastSession()

        // When
        openStaffNumber("79152")
        val field = list.inPopUp() and hasSetTextAction()
        composeRule.waitUntilExactlyOneExists(field, TIMEOUT_MS)
        composeRule.onNode(field).performTextReplacement("79125")
        tapInPopUp(SAVE)
        composeRule.waitUntilExactlyOneExists(staffNumberChip("79125"), TIMEOUT_MS)

        // Then
        harness.pressBack()
        list.unlockWithANewPin()
        val row = list.route("DOH", "SLL")
        composeRule.waitUntil(TIMEOUT_MS) { list.rowTexts(row).any { "79125" in it } }
        val texts = list.rowTexts(row)
        assertTrue("the row shows 79125: $texts", texts.any { "79125" in it })
        assertTrue("the row no longer shows 79152: $texts", texts.none { "79152" in it })
    }

    /**
     * 001-mvp/US-11/AS3: **Given** a Session with a Note tagged CM2, **when** the Assessor removes the CM2 Staff
     * Number, **then** the removal is refused and a message asks to retag the Notes tagged CM2 first.
     */
    @Test
    fun `001_mvp_US_11_AS3_removingCm2WithNotesTaggedCm2IsRefused`() {
        // Given
        seeds.session(SESSION, cm1 = "79125", cm2 = "04512")
        seeds.note(SESSION, COMPETENCY_1, "Checklist read silently", cmTag = "CM2")
        harness.launch()
        openLastSession()

        // When
        openStaffNumber("04512")
        tapInPopUp(REMOVE_CM2)

        // Then
        composeRule.waitUntilExactlyOneExists(
            list.inPopUp() and hasText(RETAG_MESSAGE, substring = true, ignoreCase = true),
            TIMEOUT_MS
        )
        composeRule.onNode(gradesDeletedMessage()).assertDoesNotExist()
        closePopUps()
        composeRule.onNode(staffNumberChip("04512")).assertExists()
    }

    /**
     * 001-mvp/US-11/AS4: **Given** a Session where no Note is tagged CM2 and CM2 has no Grade, **when** the Assessor
     * removes the CM2 Staff Number, **then** it is removed and focusing a Note no longer shows the CM Tag selector.
     */
    @Test
    fun `001_mvp_US_11_AS4_removingCm2WithoutCm2NotesOrGradesHidesTheCmTagSelector`() {
        // Given
        seeds.session(SESSION, cm1 = "79125", cm2 = "04512")
        seeds.note(SESSION, COMPETENCY_1, BOTH_NOTE, cmTag = "BOTH")
        seeds.note(SESSION, COMPETENCY_2, "Missed the callout", cmTag = "CM1")
        seeds.grade(SESSION, COMPETENCY_1, crewMember = "CM1", value = "ADEQUATE")
        harness.launch()
        openLastSession()

        // When
        openStaffNumber("04512")
        tapInPopUp(REMOVE_CM2)

        // Then
        composeRule.waitUntilDoesNotExist(staffNumberChip("04512"), TIMEOUT_MS)
        composeRule.onNode(staffNumberChip("79125")).assertExists()
        focus(noteField(COMPETENCY_1, index = 0))
        composeRule.onNode(cmTagOption(CM1)).assertDoesNotExist()
        composeRule.onNode(cmTagOption(CM2)).assertDoesNotExist()
        composeRule.onNode(cmTagOption(BOTH)).assertDoesNotExist()
    }

    /**
     * 001-mvp/US-11/AS5: **Given** a Session with only one Staff Number, **when** the Assessor tries to remove it,
     * **then** the removal is refused and a message says at least one Staff Number is required.
     */
    @Test
    fun `001_mvp_US_11_AS5_removingTheOnlyStaffNumberIsRefused`() {
        // Given
        seeds.session(SESSION, cm1 = "79125", cm2 = null)
        harness.launch()
        openLastSession()

        // When
        openStaffNumber("79125")
        tapInPopUp(REMOVE_CM1)

        // Then
        composeRule.waitUntilExactlyOneExists(
            list.inPopUp() and hasText(AT_LEAST_ONE_MESSAGE, substring = true),
            TIMEOUT_MS
        )
        closePopUps()
        composeRule.onNode(staffNumberChip("79125")).assertExists()
    }

    /**
     * 001-mvp/US-11/AS6: **Given** a Session where no Note is tagged CM2 and CM2 has Grades, **when** the Assessor
     * removes the CM2 Staff Number, **then** a confirmation says CM2's Grades will be deleted; confirming removes CM2
     * and their Grades, and cancelling keeps both.
     *
     * CM2 has 4 Grades. That they are deleted shows once a CM2 is added again: their Debrief has no Grade.
     */
    @Test
    fun `001_mvp_US_11_AS6_removingCm2WithGradesAsksFirstAndDeletesTheGradesOnlyOnConfirm`() {
        // Given
        seeds.session(SESSION, cm1 = "79125", cm2 = "04512")
        seeds.note(SESSION, COMPETENCY_1, BOTH_NOTE, cmTag = "BOTH")
        seeds.note(SESSION, COMPETENCY_2, "Missed the callout", cmTag = "CM1")
        (1L..4L).forEach { seeds.grade(SESSION, competencyId = it, crewMember = "CM2", value = "EFFECTIVE") }
        harness.launch()
        openLastSession()

        // When / Then: cancelling keeps CM2 and their Grades
        openStaffNumber("04512")
        tapInPopUp(REMOVE_CM2)
        composeRule.waitUntilExactlyOneExists(gradesDeletedMessage(), TIMEOUT_MS)
        tapInPopUp(CANCEL)
        closePopUps()
        composeRule.onNode(staffNumberChip("04512")).assertExists()
        openDebrief(CM2)
        composeRule.waitUntilExactlyOneExists(gradesSummary(graded = 4, total = 9), TIMEOUT_MS)
        leaveDebrief()

        // When / Then: confirming removes CM2 and their Grades
        openStaffNumber("04512")
        tapInPopUp(REMOVE_CM2)
        composeRule.waitUntilExactlyOneExists(gradesDeletedMessage(), TIMEOUT_MS)
        tapInPopUp(REMOVE_CM2_AND_GRADES)
        composeRule.waitUntilDoesNotExist(staffNumberChip("04512"), TIMEOUT_MS)
        composeRule.onNode(staffNumberChip("79125")).assertExists()
        addCm2("81530")
        openDebrief(CM2)
        composeRule.waitUntilExactlyOneExists(gradesSummary(graded = 0, total = 9), TIMEOUT_MS)
    }

    // Capture screen

    private fun openLastSession() {
        composeRule.waitUntilAtLeastOneExists(hasText(CONTINUE, ignoreCase = true), TIMEOUT_MS)
        composeRule.onNodeWithText(CONTINUE, ignoreCase = true).performClick()
        composeRule.waitUntilExactlyOneExists(generateButton(), TIMEOUT_MS)
    }

    private fun generateButton(): SemanticsMatcher =
        hasText(GENERATE, substring = true, ignoreCase = true) and hasClickAction() and !list.inPopUp()

    private fun staffNumberChip(staffNumber: String): SemanticsMatcher =
        hasText(staffNumber, substring = true) and hasClickAction() and !list.inPopUp()

    private fun openStaffNumber(staffNumber: String) {
        composeRule.waitUntilExactlyOneExists(staffNumberChip(staffNumber), TIMEOUT_MS)
        composeRule.onNode(staffNumberChip(staffNumber)).performScrollTo().performClick()
    }

    /** Adds [staffNumber] as CM2 through "Add CM2", its pop-up's field and Save. */
    private fun addCm2(staffNumber: String) {
        val add = hasText(ADD_CM2, substring = true, ignoreCase = true) and hasClickAction() and !list.inPopUp()
        composeRule.waitUntilExactlyOneExists(add, TIMEOUT_MS)
        composeRule.onNode(add).performScrollTo().performClick()
        val field = list.inPopUp() and hasSetTextAction()
        composeRule.waitUntilExactlyOneExists(field, TIMEOUT_MS)
        composeRule.onNode(field).performTextReplacement(staffNumber)
        tapInPopUp(SAVE)
        composeRule.waitUntilExactlyOneExists(staffNumberChip(staffNumber), TIMEOUT_MS)
    }

    private fun tapInPopUp(label: String) {
        val button = list.inPopUp() and hasText(label) and hasClickAction()
        composeRule.waitUntilExactlyOneExists(button, TIMEOUT_MS)
        composeRule.onNode(button).performClick()
    }

    /** Taps Cancel in each pop-up still open, until none is. */
    private fun closePopUps() {
        val cancel = list.inPopUp() and hasText(CANCEL) and hasClickAction()
        repeat(MAX_POP_UPS) {
            composeRule.waitForIdle()
            val open = composeRule.onAllNodes(cancel).fetchSemanticsNodes()
            if (open.isEmpty()) return
            composeRule.onAllNodes(cancel)[open.lastIndex].performClick()
        }
        composeRule.waitUntilDoesNotExist(cancel, TIMEOUT_MS)
    }

    private fun gradesDeletedMessage(): SemanticsMatcher =
        list.inPopUp() and
            SemanticsMatcher("says CM2's Grades will be deleted") { node ->
                node.texts().any { GRADES_DELETED.containsMatchIn(it) }
            }

    private fun cmTagOption(label: String): SemanticsMatcher = hasText(label) and isSelectable() and !list.inPopUp()

    private fun noteField(
        competencyId: Long,
        index: Int,
    ): SemanticsNodeInteraction {
        val tag = "note-field-$competencyId"
        composeRule.waitUntil(TIMEOUT_MS) { composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().size > index }
        return composeRule.onAllNodesWithTag(tag)[index]
    }

    private fun focus(field: SemanticsNodeInteraction) {
        field.performScrollTo().performClick()
        field.assertIsFocused()
    }

    // Debrief

    /** Taps Generate debriefing and picks [crewMember] in the pop-up that asks which Crew Member. */
    private fun openDebrief(crewMember: String) {
        composeRule.onNode(generateButton()).performScrollTo().performClick()
        val choice = list.inPopUp() and hasClickAction() and hasText(crewMember, substring = true)
        composeRule.waitUntilExactlyOneExists(choice, TIMEOUT_MS)
        composeRule.onNode(choice).performClick()
        composeRule.waitUntilExactlyOneExists(hasText(DEBRIEF) and !list.inPopUp(), TIMEOUT_MS)
        composeRule.waitUntilDoesNotExist(generateButton(), TIMEOUT_MS)
    }

    /** Leaves the Debrief with its back arrow, through Leave if the ungraded-Competencies message shows. */
    private fun leaveDebrief() {
        composeRule.onNode(hasContentDescription(BACK) and hasClickAction() and !list.inPopUp()).performClick()
        val leave = list.inPopUp() and hasText(LEAVE) and hasClickAction()
        composeRule.waitUntil(TIMEOUT_MS) { exists(generateButton()) || exists(leave) }
        if (exists(leave)) composeRule.onNode(leave).performClick()
        composeRule.waitUntilExactlyOneExists(generateButton(), TIMEOUT_MS)
    }

    /** Opens the eye pop-up of the topmost Competency that has one. */
    private fun openFirstEyePopUp() {
        val eye = hasContentDescription(SHOW_NOTES) and hasClickAction() and !list.inPopUp()
        composeRule.waitUntilAtLeastOneExists(eye, TIMEOUT_MS)
        composeRule.onAllNodes(eye)[0].performScrollTo().performClick()
    }

    /** Matches the Grade count in the summary header, as the spec writes it or as the design draws it. */
    private fun gradesSummary(
        graded: Int,
        total: Int,
    ): SemanticsMatcher {
        val pattern = Regex("""\s*(Grades $graded/$total|$graded of $total Grades? set)\s*""")
        return SemanticsMatcher("shows $graded of $total Grades") { node ->
            node.texts().any { pattern.matches(it) }
        } and !list.inPopUp()
    }

    private fun exists(matcher: SemanticsMatcher): Boolean =
        composeRule.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty()

    private fun SemanticsNode.texts(): List<String> =
        config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text }

    private companion object {
        const val TIMEOUT_MS = 5000L
        const val MAX_POP_UPS = 3
        const val SESSION = "session"
        const val COMPETENCY_1 = 1L
        const val COMPETENCY_2 = 2L
        const val BOTH_NOTE = "Readback of altitude clearance incomplete"
        const val CONTINUE = "Continue last Session"
        const val GENERATE = "Generate debrief"
        const val DEBRIEF = "Debrief"
        const val BACK = "Back"
        const val LEAVE = "Leave"
        const val SHOW_NOTES = "Show Notes"
        const val ADD_CM2 = "Add CM2"
        const val SAVE = "Save"
        const val CANCEL = "Cancel"
        const val REMOVE_CM1 = "Remove CM1"
        const val REMOVE_CM2 = "Remove CM2"
        const val REMOVE_CM2_AND_GRADES = "Remove CM2 and Grades"
        const val RETAG_MESSAGE = "Retag the Notes tagged CM2 first"
        const val AT_LEAST_ONE_MESSAGE = "At least one Staff Number is required."
        const val CM1 = "CM1"
        const val CM2 = "CM2"
        const val BOTH = "BOTH"
        val GRADES_DELETED = Regex("""CM2's (\d+ )?Grades? will be deleted""")
    }
}
