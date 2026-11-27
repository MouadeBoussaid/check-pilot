package com.mebiad.checkpilot.acceptance

import android.view.KeyEvent
import android.view.View
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.isSelectable
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
import org.junit.Assert.assertNull
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
 * US-4: Debrief a Crew Member. Each test seeds a Session with Notes and Grades in their database rows, opens it with
 * Continue last Session, taps Generate debriefing on the capture screen, and works the Debrief: the CM1 / CM2 toggle,
 * eye icons, Grade selectors, the back arrow, the system back and the date.
 *
 * How the tests find things, so restyling doesn't break them:
 * - Generate debriefing is the clickable node whose text contains "Generate debrief", ignoring case, outside pop-ups.
 * - A pop-up is anything in a dialog or popup window: the CM pop-up, the eye pop-up, the Grade menu, the leave message.
 *   In the CM pop-up, each Crew Member's choice is the clickable node whose text contains CM1 or CM2.
 * - The Debrief is showing while the text "Debrief" stands alone outside pop-ups and the capture screen is gone.
 * - Each Competency's place is its title, such as "Competency 2". Everything between that title and the next one
 *   belongs to it: its Grade selector, the one clickable node with text there, and its eye icon, described "Show
 *   Notes". A Grade selector has no Grade while none of its texts is a Grade's name.
 * - The CM1 / CM2 toggle is the selectable nodes labelled CM1 and CM2 on the Debrief.
 * - The back arrow is described "Back". The eye pop-up closes with the clickable node described "Close".
 * - The summary header shows "<n> Notes" and "<n> OBS BEH", and the Grade count either as the spec writes it,
 *   "Grades 6/9", or as the design draws it, "6 of 9 Grades set".
 *
 * The screen is tall so every Competency of the Debrief is laid out at once, whatever list it uses.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h3000dp")
@OptIn(ExperimentalTestApi::class)
class US4DebriefACrewMemberTest {
    private val harness = AppHarness()
    private val composeRule = createEmptyComposeRule()
    private val seeds = SessionSeeds(harness)

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(harness).around(composeRule)

    /**
     * 001-mvp/US-4/AS1: **Given** a Session with two Crew Members, **when** the Assessor taps Generate debriefing,
     * **then** a pop-up asks for the CM 1 or the CM 2 debriefing.
     */
    @Test
    fun `001_mvp_US_4_AS1_generatingWithTwoCrewMembersAsksWhichCrewMember`() {
        // Given
        seeds.session(SESSION)
        harness.launch()
        openLastSession()

        // When
        tapGenerate()

        // Then
        composeRule.waitUntilExactlyOneExists(cmChoice(CM1), TIMEOUT_MS)
        composeRule.onNode(cmChoice(CM2)).assertExists()
        composeRule.onNode(debriefTitle()).assertDoesNotExist()
    }

    /**
     * 001-mvp/US-4/AS2: **Given** a Session with one Crew Member, **when** the Assessor taps Generate debriefing,
     * **then** that Crew Member's Debrief opens directly, with no pop-up.
     *
     * The one Crew Member sits in the CM2 slot, and only a Note tagged CM2 lifts Competency 7 to the top, so the
     * order shows whose Debrief opened.
     */
    @Test
    fun `001_mvp_US_4_AS2_generatingWithOneCrewMemberOpensTheirDebriefDirectly`() {
        // Given
        seeds.session(SESSION, cm1 = null, cm2 = "04512")
        seeds.note(SESSION, competencyId = 7, text = "Late flaps", cmTag = "CM2")
        harness.launch()
        openLastSession()

        // When
        tapGenerate()

        // Then
        waitForDebrief()
        assertEquals(0, composeRule.onAllNodes(isDialog()).fetchSemanticsNodes().size)
        waitForOrder("the Debrief of the CM2 slot") { it.first() == 7L }
    }

    /**
     * 001-mvp/US-4/AS3: **Given** a Note tagged CM1 and a Note tagged BOTH under Competency 2, **when** the Assessor
     * opens the eye pop-up of Competency 2 in each Debrief, **then** CM1's lists both Notes and CM2's lists only the
     * BOTH Note.
     */
    @Test
    fun `001_mvp_US_4_AS3_theEyePopUpListsTheCrewMembersOwnAndBothNotes`() {
        // Given
        seeds.session(SESSION)
        seeds.note(SESSION, competencyId = 2, text = "Missed ATC frequency change", cmTag = "CM1")
        seeds.note(SESSION, competencyId = 2, text = "Readback incomplete", cmTag = "BOTH")
        harness.launch()
        openLastSession()

        // When / Then: CM1's Debrief
        openDebrief(CM1)
        openEyePopUp(competencyId = 2)
        composeRule.waitUntilExactlyOneExists(inPopUp() and hasText("Readback incomplete"), TIMEOUT_MS)
        composeRule.onNode(inPopUp() and hasText("Missed ATC frequency change")).assertExists()
        closeEyePopUp()

        // When / Then: CM2's Debrief
        selectOnToggle(CM2)
        openEyePopUp(competencyId = 2)
        composeRule.waitUntilExactlyOneExists(inPopUp() and hasText("Readback incomplete"), TIMEOUT_MS)
        composeRule.onNode(inPopUp() and hasText("Missed ATC frequency change")).assertDoesNotExist()
    }

    /**
     * 001-mvp/US-4/AS4: **Given** Competency 4 has three Notes for CM1 and Competency 1 has one, **when** the CM1
     * Debrief opens, **then** Competency 4 is listed above Competency 1.
     *
     * Competency 1 also has three Notes tagged CM2, which aren't about CM1.
     */
    @Test
    fun `001_mvp_US_4_AS4_moreNotesForTheCrewMemberListsACompetencyHigher`() {
        // Given
        seeds.session(SESSION)
        seeds.note(SESSION, competencyId = 4, text = "Late gear", cmTag = "CM1")
        seeds.note(SESSION, competencyId = 4, text = "Late flaps", cmTag = "BOTH")
        seeds.note(SESSION, competencyId = 4, text = "Late spoilers", cmTag = "CM1")
        seeds.note(SESSION, competencyId = 1, text = "Checklist read silently", cmTag = "CM1")
        repeat(3) { seeds.note(SESSION, competencyId = 1, text = "Not about CM1 $it", cmTag = "CM2") }
        harness.launch()
        openLastSession()

        // When
        openDebrief(CM1)

        // Then
        waitForOrder("Competency 4 above Competency 1") { it.indexOf(4L) < it.indexOf(1L) }
    }

    /**
     * 001-mvp/US-4/AS5: **Given** Competencies 5 and 6 both have two Notes for CM1, with two different OBS BEH under 6
     * and the same OBS BEH twice under 5, **when** the CM1 Debrief opens, **then** Competency 6 is listed above
     * Competency 5.
     */
    @Test
    fun `001_mvp_US_4_AS5_moreDistinctObsBehBreaksATieInNotes`() {
        // Given
        seeds.session(SESSION)
        seeds.note(SESSION, competencyId = 5, text = "Late gear", cmTag = "CM1", observableBehaviour = "5a")
        seeds.note(SESSION, competencyId = 5, text = "Late flaps", cmTag = "CM1", observableBehaviour = "5a")
        seeds.note(SESSION, competencyId = 6, text = "Late spoilers", cmTag = "CM1", observableBehaviour = "6a")
        seeds.note(SESSION, competencyId = 6, text = "Late lights", cmTag = "CM1", observableBehaviour = "6b")
        harness.launch()
        openLastSession()

        // When
        openDebrief(CM1)

        // Then
        waitForOrder("Competency 6 above Competency 5") { it.indexOf(6L) < it.indexOf(5L) }
    }

    /**
     * 001-mvp/US-4/AS6: **Given** two Competencies with the same number of Notes and distinct OBS BEH for CM1, **when**
     * the CM1 Debrief opens, **then** they are listed in Catalogue order.
     *
     * Competency 7 also has two Notes tagged CM2, which aren't about CM1.
     */
    @Test
    fun `001_mvp_US_4_AS6_aFullTieKeepsCatalogueOrder`() {
        // Given
        seeds.session(SESSION)
        seeds.note(SESSION, competencyId = 7, text = "Late gear", cmTag = "CM1", observableBehaviour = "7a")
        seeds.note(SESSION, competencyId = 3, text = "Late flaps", cmTag = "BOTH", observableBehaviour = "3a")
        seeds.note(SESSION, competencyId = 7, text = "Not about CM1", cmTag = "CM2", observableBehaviour = "7b")
        seeds.note(SESSION, competencyId = 7, text = "Not about CM1 either", cmTag = "CM2", observableBehaviour = "7c")
        harness.launch()
        openLastSession()

        // When
        openDebrief(CM1)

        // Then
        waitForOrder("Competency 3 then Competency 7 at the top") { it.take(2) == listOf(3L, 7L) }
    }

    /**
     * 001-mvp/US-4/AS7: **Given** Competencies with no Notes for CM1, **when** the CM1 Debrief opens, **then** they are
     * listed at the bottom, in Catalogue order, each with a Grade selector.
     *
     * Competency 5's only Note is tagged CM2, so it has no Notes for CM1.
     */
    @Test
    fun `001_mvp_US_4_AS7_competenciesWithoutNotesComeLastInCatalogueOrderWithAGradeSelector`() {
        // Given
        seeds.session(SESSION)
        seeds.note(SESSION, competencyId = 8, text = "Late gear", cmTag = "CM1")
        seeds.note(SESSION, competencyId = 2, text = "Late flaps", cmTag = "BOTH")
        seeds.note(SESSION, competencyId = 5, text = "Not about CM1", cmTag = "CM2")
        harness.launch()
        openLastSession()

        // When
        openDebrief(CM1)

        // Then
        waitForOrder("Competencies without Notes last, in Catalogue order") {
            it == listOf(2L, 8L, 1L, 3L, 4L, 5L, 6L, 7L, 9L)
        }
        listOf(1L, 3L, 4L, 5L, 6L, 7L, 9L).forEach { competencyId ->
            assertEquals("Grade selectors of Competency $competencyId", 1, gradeSelectorIndices(competencyId).size)
        }
    }

    /**
     * 001-mvp/US-4/AS8: **Given** a Debrief opened for the first time, **when** the Assessor looks at the Grade
     * selectors, **then** none has a Grade, and each offers Not Observed, Exemplary, Effective, Adequate, Minimum
     * Acceptable Level and Not Yet Competent, in that order.
     */
    @Test
    fun `001_mvp_US_4_AS8_aNewDebriefHasNoGradesAndEachSelectorOffersTheScaleInOrder`() {
        // Given
        seeds.session(SESSION, cm2 = null)
        seeds.note(SESSION, competencyId = 1, text = "Checklist read silently")
        harness.launch()
        openLastSession()
        openDebrief(crewMember = null)

        // When / Then
        (1L..9L).forEach { competencyId ->
            assertNull("Grade of Competency $competencyId", gradeOf(competencyId))
            assertEquals("Grades offered for Competency $competencyId", GRADES, gradeMenuOf(competencyId))
            dismissGradeMenu()
        }
    }

    /**
     * 001-mvp/US-4/AS9: **Given** a Competency with no Notes, **when** the Assessor sets its Grade to Effective,
     * **then** the Grade is kept.
     *
     * Kept means it is still there once the Assessor has left the Debrief and opened it again.
     */
    @Test
    fun `001_mvp_US_4_AS9_aGradeSetWithoutNotesIsKept`() {
        // Given
        seeds.session(SESSION, cm2 = null)
        seeds.note(SESSION, competencyId = 1, text = "Checklist read silently")
        harness.launch()
        openLastSession()
        openDebrief(crewMember = null)

        // When
        setGrade(competencyId = 9, grade = EFFECTIVE)

        // Then
        waitForGrade(competencyId = 9, expected = EFFECTIVE)
        leaveDebrief()
        openDebrief(crewMember = null)
        waitForGrade(competencyId = 9, expected = EFFECTIVE)
    }

    /**
     * 001-mvp/US-4/AS10: **Given** CM1's Grade for Competency 2 set to Adequate, **when** the Assessor taps CM2 on the
     * Debrief's toggle and then CM1, **then** CM2's Competency 2 has no Grade and CM1's still shows Adequate.
     */
    @Test
    fun `001_mvp_US_4_AS10_gradesBelongToOneCrewMember`() {
        // Given
        seeds.session(SESSION)
        seeds.grade(SESSION, competencyId = 2, crewMember = CM1, value = "ADEQUATE")
        harness.launch()
        openLastSession()
        openDebrief(CM1)
        waitForGrade(competencyId = 2, expected = ADEQUATE)

        // When / Then: CM2 tapped
        selectOnToggle(CM2)
        waitForGrade(competencyId = 2, expected = null)

        // When / Then: CM1 tapped
        selectOnToggle(CM1)
        waitForGrade(competencyId = 2, expected = ADEQUATE)
    }

    /**
     * 001-mvp/US-4/AS11: **Given** a Competency with Notes, **when** the Assessor taps its eye icon, **then** a pop-up
     * lists that Crew Member's Notes for the Competency, each with its OBS BEH, and closing it returns to the Debrief.
     */
    @Test
    fun `001_mvp_US_4_AS11_theEyePopUpListsTheNotesWithTheirObsBehAndCloses`() {
        // Given
        seeds.session(SESSION)
        seeds.note(
            SESSION,
            competencyId = 2,
            text = "Missed ATC frequency change",
            cmTag = "CM1",
            observableBehaviour = "2c"
        )
        seeds.note(SESSION, competencyId = 2, text = "Readback incomplete", cmTag = "BOTH", observableBehaviour = "2d")
        seeds.note(SESSION, competencyId = 2, text = "Not about CM1", cmTag = "CM2", observableBehaviour = "2e")
        harness.launch()
        openLastSession()
        openDebrief(CM1)

        // When
        openEyePopUp(competencyId = 2)

        // Then
        composeRule.waitUntilExactlyOneExists(inPopUp() and hasText("Readback incomplete"), TIMEOUT_MS)
        composeRule.onNode(inPopUp() and hasText("Not about CM1")).assertDoesNotExist()
        val obsBehUnderNote = obsBehUnderEachNoteInPopUp(listOf("Missed ATC frequency change", "Readback incomplete"))
        assertTrue(
            "2c under its Note: $obsBehUnderNote",
            "2c" in obsBehUnderNote.getValue("Missed ATC frequency change")
        )
        assertTrue("2d under its Note: $obsBehUnderNote", "2d" in obsBehUnderNote.getValue("Readback incomplete"))

        // When / Then: the pop-up closed
        closeEyePopUp()
        composeRule.onNode(debriefTitle()).assertExists()
    }

    /**
     * 001-mvp/US-4/AS12: **Given** a Debrief, **when** it opens, **then** a summary header shows the number of Notes,
     * the number of OBS BEH, and how many Competencies have a Grade, such as "Grades 6/9".
     *
     * CM1 has four Notes (two tagged CM1, two BOTH) linking three OBS BEH, and Grades for six Competencies. CM2's own
     * Note, OBS BEH and Grades don't count.
     */
    @Test
    fun `001_mvp_US_4_AS12_theSummaryHeaderCountsNotesObsBehAndGrades`() {
        // Given
        seeds.session(SESSION)
        seeds.note(
            SESSION,
            competencyId = 2,
            text = "Missed ATC frequency change",
            cmTag = "CM1",
            observableBehaviour = "2c"
        )
        seeds.note(SESSION, competencyId = 2, text = "Readback incomplete", cmTag = "BOTH", observableBehaviour = "2d")
        seeds.note(SESSION, competencyId = 2, text = "Not about CM1", cmTag = "CM2", observableBehaviour = "2e")
        seeds.note(SESSION, competencyId = 3, text = "Late flaps", cmTag = "CM1")
        seeds.note(SESSION, competencyId = 3, text = "Late gear", cmTag = "BOTH", observableBehaviour = "3a")
        (1L..6L).forEach { seeds.grade(SESSION, competencyId = it, crewMember = CM1, value = "EFFECTIVE") }
        (7L..8L).forEach { seeds.grade(SESSION, competencyId = it, crewMember = CM2, value = "EFFECTIVE") }
        harness.launch()
        openLastSession()

        // When
        openDebrief(CM1)

        // Then
        composeRule.waitUntilAtLeastOneExists(gradesSummary(graded = 6, total = 9), TIMEOUT_MS)
        composeRule.onAllNodes(hasText("4 Notes") and !inPopUp()).fetchSemanticsNodes().let { assertEquals(1, it.size) }
        composeRule
            .onAllNodes(
                hasText("3 OBS BEH") and !inPopUp()
            ).fetchSemanticsNodes()
            .let { assertEquals(1, it.size) }
    }

    /**
     * 001-mvp/US-4/AS13: **Given** a Debrief with 3 Competencies without a Grade, **when** the Assessor leaves it with
     * the system back or the Debrief's back arrow, **then** a message says "3 Competencies have no Grade" with Leave
     * and Stay; Stay keeps the Debrief open and Leave closes it.
     */
    @Test
    fun `001_mvp_US_4_AS13_leavingWithUngradedCompetenciesAsksToLeaveOrStay`() {
        // Given
        seeds.session(SESSION, cm2 = null)
        (1L..6L).forEach { seeds.grade(SESSION, competencyId = it, crewMember = CM1, value = "EFFECTIVE") }
        harness.launch()
        openLastSession()
        openDebrief(crewMember = null)

        // When / Then: the system back, then Stay
        harness.pressBack()
        waitForLeaveMessage()
        composeRule.onNode(inPopUp() and hasText(STAY) and hasClickAction()).performClick()
        composeRule.waitUntilDoesNotExist(leaveMessage(), TIMEOUT_MS)
        composeRule.onNode(debriefTitle()).assertExists()
        composeRule.onNode(generateButton()).assertDoesNotExist()

        // When / Then: the back arrow, then Leave
        composeRule.onNode(backArrow()).performClick()
        waitForLeaveMessage()
        composeRule.onNode(inPopUp() and hasText(LEAVE) and hasClickAction()).performClick()
        composeRule.waitUntilExactlyOneExists(generateButton(), TIMEOUT_MS)
        composeRule.onNode(debriefTitle()).assertDoesNotExist()
        composeRule.onNode(leaveMessage()).assertDoesNotExist()
    }

    /**
     * 001-mvp/US-4/AS14: **Given** a Debrief where every Competency has a Grade, **when** the Assessor leaves it with the
     * system back or the Debrief's back arrow, **then** no message appears.
     */
    @Test
    fun `001_mvp_US_4_AS14_leavingAFullyGradedDebriefShowsNoMessage`() {
        // Given
        seeds.session(SESSION, cm2 = null)
        (1L..9L).forEach { seeds.grade(SESSION, competencyId = it, crewMember = CM1, value = "EFFECTIVE") }
        harness.launch()
        openLastSession()
        openDebrief(crewMember = null)
        composeRule.waitUntilAtLeastOneExists(gradesSummary(graded = 9, total = 9), TIMEOUT_MS)

        // When / Then: the system back
        harness.pressBack()
        composeRule.waitUntilExactlyOneExists(generateButton(), TIMEOUT_MS)
        composeRule.onNode(leaveMessage(), useUnmergedTree = true).assertDoesNotExist()
        composeRule.onNode(debriefTitle()).assertDoesNotExist()

        // When / Then: the back arrow
        openDebrief(crewMember = null)
        composeRule.waitUntilAtLeastOneExists(gradesSummary(graded = 9, total = 9), TIMEOUT_MS)
        composeRule.onNode(backArrow()).performClick()
        composeRule.waitUntilExactlyOneExists(generateButton(), TIMEOUT_MS)
        composeRule.onNode(leaveMessage(), useUnmergedTree = true).assertDoesNotExist()
        composeRule.onNode(debriefTitle()).assertDoesNotExist()
    }

    /**
     * 001-mvp/US-4/AS15: **Given** an opened Debrief, **when** the Assessor goes back to capture, adds two Notes for CM1
     * under the lowest-listed Competency, and opens the CM1 Debrief again, **then** that Competency has moved up.
     */
    @Test
    fun `001_mvp_US_4_AS15_notesAddedAfterADebriefMoveTheCompetencyUp`() {
        // Given
        seeds.session(SESSION)
        seeds.note(SESSION, competencyId = 1, text = "Checklist read silently", cmTag = "CM1")
        harness.launch()
        openLastSession()
        openDebrief(CM1)
        val before = waitForOrder("all nine Competencies listed") { it.size == 9 }
        val lowest = before.last()

        // When
        leaveDebrief()
        addNoteForCm1(competencyId = lowest, fieldIndex = 0, text = "Late gear")
        addNoteForCm1(competencyId = lowest, fieldIndex = 1, text = "Late flaps")
        openDebrief(CM1)

        // Then
        waitForOrder("Competency $lowest moved up from ${before.indexOf(lowest)}") {
            it.indexOf(lowest) in 0 until before.indexOf(lowest)
        }
    }

    /**
     * 001-mvp/US-4/AS16: **Given** a Debrief, **when** the Assessor changes the date on it, **then** the capture screen
     * shows the new date too.
     */
    @Test
    fun `001_mvp_US_4_AS16_aDateChangedOnTheDebriefShowsOnTheCaptureScreen`() {
        // Given
        harness.clock.setToday(LocalDate.of(2026, 11, 21))
        seeds.session(SESSION, cm2 = null, date = LocalDate.of(2026, 11, 1))
        harness.launch()
        openLastSession()
        openDebrief(crewMember = null)

        // When
        composeRule.onNode(hasText("01 Nov 2026", substring = true) and hasClickAction() and !inPopUp()).performClick()
        pickInDatePicker(LocalDate.of(2026, 11, 5))

        // Then
        leaveDebrief()
        composeRule.waitUntilAtLeastOneExists(hasText("05 Nov 2026", substring = true), TIMEOUT_MS)
        composeRule.onNode(hasText("01 Nov 2026", substring = true)).assertDoesNotExist()
    }

    /**
     * 001-mvp/US-4/AS17: **Given** a Debrief, **when** it opens, **then** the CM1 / CM2 toggle is shown with two Crew
     * Members, with the opened Crew Member selected, and is not shown with one.
     *
     * Only the Last Session can be opened before the Session list exists (US-7), so the Session with one Crew Member is
     * created later, after the Session with two was checked, and becomes the Last Session.
     */
    @Test
    fun `001_mvp_US_4_AS17_theToggleShowsWithTwoCrewMembersOnlyWithTheOpenedOneSelected`() {
        // Given
        seeds.session(SESSION)
        harness.launch()
        openLastSession()

        // When / Then: CM2's Debrief opened in a Session with two Crew Members
        openDebrief(CM2)
        composeRule.waitUntilExactlyOneExists(toggleOption(CM2), TIMEOUT_MS)
        composeRule.onNode(toggleOption(CM2)).assertIsSelected()
        composeRule.onNode(toggleOption(CM1)).assertIsNotSelected()

        // When / Then: the Debrief opened in a Session with one Crew Member
        leaveDebrief()
        composeRule.onNodeWithText(DONE).performScrollTo().performClick()
        composeRule.waitUntilAtLeastOneExists(hasText(CONTINUE, ignoreCase = true), TIMEOUT_MS)
        seeds.session(
            SESSION_ONE_CM,
            cm2 = null,
            departure = "SLL",
            destination = "DOH",
            createdAt = Instant.parse("2026-11-01T09:00:00Z")
        )
        composeRule.waitUntilExactlyOneExists(hasText(ROUTE_ONE_CM), TIMEOUT_MS)
        openLastSession()
        openDebrief(crewMember = null)
        composeRule.onNode(toggleOption(CM1)).assertDoesNotExist()
        composeRule.onNode(toggleOption(CM2)).assertDoesNotExist()
    }

    /**
     * 001-mvp/US-4/AS18: **Given** the CM1 Debrief with Competencies without a Grade, **when** the Assessor taps CM2 on
     * the toggle, **then** the CM2 Debrief shows and no ungraded-Competencies message appears.
     *
     * Only a Note tagged CM2 lifts Competency 5 to the top, so the order shows CM2's Debrief.
     */
    @Test
    fun `001_mvp_US_4_AS18_switchingCrewMemberOnTheToggleIsNotLeaving`() {
        // Given
        seeds.session(SESSION)
        seeds.note(SESSION, competencyId = 1, text = "Checklist read silently", cmTag = "CM1")
        seeds.note(SESSION, competencyId = 5, text = "Late gear", cmTag = "CM2")
        harness.launch()
        openLastSession()
        openDebrief(CM1)
        waitForOrder("CM1's Debrief") { it.first() == 1L }

        // When
        composeRule.onNode(toggleOption(CM2)).performClick()

        // Then
        waitForOrder("CM2's Debrief") { it.first() == 5L }
        composeRule.onNode(toggleOption(CM2)).assertIsSelected()
        composeRule.onNode(debriefTitle()).assertExists()
        composeRule.onNode(leaveMessage(), useUnmergedTree = true).assertDoesNotExist()
    }

    // Capture screen

    private fun openLastSession() {
        composeRule.waitUntilAtLeastOneExists(hasText(CONTINUE, ignoreCase = true), TIMEOUT_MS)
        composeRule.onNodeWithText(CONTINUE, ignoreCase = true).performClick()
        composeRule.waitUntilExactlyOneExists(generateButton(), TIMEOUT_MS)
    }

    private fun generateButton(): SemanticsMatcher =
        hasText(GENERATE, substring = true, ignoreCase = true) and hasClickAction() and !inPopUp()

    private fun tapGenerate() {
        composeRule.onNode(generateButton()).performScrollTo().performClick()
    }

    /** Types [text] into the Note field at [fieldIndex] of the Competency and tags it CM1. */
    private fun addNoteForCm1(
        competencyId: Long,
        fieldIndex: Int,
        text: String,
    ) {
        val tag = "note-field-$competencyId"
        composeRule.waitUntil(TIMEOUT_MS) { composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().size > fieldIndex }
        val field = composeRule.onAllNodesWithTag(tag)[fieldIndex]
        field.performScrollTo().performClick()
        field.performTextInput(text)
        val cm1Tag = hasText(CM1) and isSelectable()
        composeRule.waitUntilExactlyOneExists(cm1Tag, TIMEOUT_MS)
        composeRule.onNode(cm1Tag).performClick()
        composeRule.onNode(cm1Tag).assertIsSelected()
    }

    // Debrief

    /** Taps Generate debriefing and, with two Crew Members, picks [crewMember] in the pop-up. */
    private fun openDebrief(crewMember: String?) {
        tapGenerate()
        if (crewMember != null) {
            composeRule.waitUntilExactlyOneExists(cmChoice(crewMember), TIMEOUT_MS)
            composeRule.onNode(cmChoice(crewMember)).performClick()
        }
        waitForDebrief()
    }

    private fun waitForDebrief() {
        composeRule.waitUntilExactlyOneExists(debriefTitle(), TIMEOUT_MS)
        composeRule.waitUntilDoesNotExist(generateButton(), TIMEOUT_MS)
    }

    /** Leaves the Debrief with its back arrow, through Leave if the ungraded-Competencies message shows. */
    private fun leaveDebrief() {
        composeRule.onNode(backArrow()).performClick()
        composeRule.waitUntil(TIMEOUT_MS) { exists(generateButton()) || exists(leaveButton()) }
        if (exists(leaveButton())) composeRule.onNode(leaveButton()).performClick()
        composeRule.waitUntilExactlyOneExists(generateButton(), TIMEOUT_MS)
    }

    private fun cmChoice(crewMember: String): SemanticsMatcher =
        inPopUp() and hasClickAction() and hasText(crewMember, substring = true)

    private fun debriefTitle(): SemanticsMatcher = hasText(DEBRIEF) and !inPopUp()

    private fun backArrow(): SemanticsMatcher = hasContentDescription(BACK) and hasClickAction() and !inPopUp()

    private fun toggleOption(crewMember: String): SemanticsMatcher =
        hasText(crewMember) and isSelectable() and !inPopUp()

    private fun selectOnToggle(crewMember: String) {
        composeRule.waitUntilExactlyOneExists(toggleOption(crewMember), TIMEOUT_MS)
        composeRule.onNode(toggleOption(crewMember)).performClick()
        composeRule.onNode(toggleOption(crewMember)).assertIsSelected()
    }

    private fun leaveMessage(): SemanticsMatcher = hasText(NO_GRADE_MESSAGE, substring = true)

    private fun leaveButton(): SemanticsMatcher = inPopUp() and hasText(LEAVE) and hasClickAction()

    private fun waitForLeaveMessage() {
        composeRule.waitUntilExactlyOneExists(inPopUp() and hasText("3 Competencies have no Grade"), TIMEOUT_MS)
        composeRule.onNode(inPopUp() and hasText(LEAVE) and hasClickAction()).assertExists()
        composeRule.onNode(inPopUp() and hasText(STAY) and hasClickAction()).assertExists()
    }

    /** Matches the Grade count in the summary header, as the spec writes it or as the design draws it. */
    private fun gradesSummary(
        graded: Int,
        total: Int,
    ): SemanticsMatcher {
        val pattern = Regex("""\s*(Grades $graded/$total|$graded of $total Grades set)\s*""")
        return SemanticsMatcher("shows $graded of $total Grades") { node ->
            !node.inPopUp() && node.texts().any { pattern.matches(it) }
        }
    }

    /** The Competencies' ids, top to bottom, as the Debrief lists them. */
    private fun competencyOrder(): List<Long> = competencyTitles().map { it.first }

    private fun competencyTitles(): List<Pair<Long, SemanticsNode>> =
        composeRule
            .onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text) and !inPopUp(), useUnmergedTree = true)
            .fetchSemanticsNodes()
            .mapNotNull { node ->
                node
                    .texts()
                    .firstNotNullOfOrNull { COMPETENCY_TITLE.matchEntire(it) }
                    ?.let { it.groupValues[1].toLong() to node }
            }.sortedBy { (_, node) -> node.top }

    /** Waits until the Debrief's order satisfies [condition], fails with the order it shows, and returns that order. */
    private fun waitForOrder(
        what: String,
        condition: (List<Long>) -> Boolean,
    ): List<Long> {
        val reached = runCatching { composeRule.waitUntil(TIMEOUT_MS) { condition(competencyOrder()) } }
        val order = competencyOrder()
        assertTrue("$what, but the Debrief lists $order", reached.isSuccess && condition(order))
        return order
    }

    /** The vertical band of the Debrief that belongs to the Competency: from its title to the next title. */
    private fun bandOf(competencyId: Long): ClosedFloatingPointRange<Float> {
        val titles = competencyTitles()
        val index = titles.indexOfFirst { it.first == competencyId }
        check(index >= 0) { "Competency $competencyId isn't listed; the Debrief lists ${titles.map { it.first }}" }
        val top = titles[index].second.top
        val bottom = titles.getOrNull(index + 1)?.second?.top ?: Float.MAX_VALUE
        return top..bottom
    }

    /** The indices, among [matcher]'s nodes outside pop-ups, of those lying in the Competency's band. */
    private fun indicesInBand(
        competencyId: Long,
        matcher: SemanticsMatcher,
    ): List<Int> {
        val band = bandOf(competencyId)
        return composeRule
            .onAllNodes(matcher and !inPopUp())
            .fetchSemanticsNodes()
            .withIndex()
            .filter { (_, node) -> node.top in band && node.top < band.endInclusive }
            .map { it.index }
    }

    private fun gradeSelectorMatcher(): SemanticsMatcher =
        hasClickAction() and
            SemanticsMatcher("has text") {
                it.config.getOrNull(SemanticsProperties.Text) != null ||
                    it.config.getOrNull(SemanticsProperties.EditableText) != null
            }

    private fun gradeSelectorIndices(competencyId: Long): List<Int> =
        indicesInBand(competencyId, gradeSelectorMatcher())

    private fun gradeSelector(competencyId: Long): SemanticsNodeInteraction {
        val indices = gradeSelectorIndices(competencyId)
        check(indices.size == 1) { "Competency $competencyId has ${indices.size} Grade selectors" }
        return composeRule.onAllNodes(gradeSelectorMatcher() and !inPopUp())[indices.single()]
    }

    /** The Grade the Competency's selector shows, or null when it shows none. */
    private fun gradeOf(competencyId: Long): String? {
        val node = gradeSelector(competencyId).fetchSemanticsNode()
        val shown =
            node.texts() + listOfNotNull(node.config.getOrNull(SemanticsProperties.EditableText)?.text)
        return GRADES.firstOrNull { grade -> shown.any { it.trim() == grade } }
    }

    private fun waitForGrade(
        competencyId: Long,
        expected: String?,
    ) {
        val reached = runCatching { composeRule.waitUntil(TIMEOUT_MS) { gradeOf(competencyId) == expected } }
        if (reached.isFailure) assertEquals("Grade of Competency $competencyId", expected, gradeOf(competencyId))
    }

    private fun gradeMenuItem(): SemanticsMatcher = inPopUp() and hasClickAction()

    /** Opens the Competency's Grade selector and returns the Grades it offers, top to bottom. */
    private fun gradeMenuOf(competencyId: Long): List<String> {
        gradeSelector(competencyId).performScrollTo().performClick()
        composeRule.waitUntilAtLeastOneExists(gradeMenuItem(), TIMEOUT_MS)
        return composeRule
            .onAllNodes(gradeMenuItem())
            .fetchSemanticsNodes()
            .sortedBy { it.top }
            .map { it.texts().joinToString(" ") }
    }

    private fun setGrade(
        competencyId: Long,
        grade: String,
    ) {
        gradeSelector(competencyId).performScrollTo().performClick()
        composeRule.waitUntilExactlyOneExists(gradeMenuItem() and hasText(grade), TIMEOUT_MS)
        composeRule.onNode(gradeMenuItem() and hasText(grade)).performClick()
    }

    /** Closes the open Grade menu the way the system back closes the window on top. */
    private fun dismissGradeMenu() {
        pressBackOnTopWindow()
        composeRule.waitUntilDoesNotExist(gradeMenuItem(), TIMEOUT_MS)
    }

    private fun openEyePopUp(competencyId: Long) {
        val eye = hasContentDescription(SHOW_NOTES) and hasClickAction()
        composeRule.waitUntil(TIMEOUT_MS) { runCatching { indicesInBand(competencyId, eye) }.getOrNull()?.size == 1 }
        val index = indicesInBand(competencyId, eye).single()
        composeRule.onAllNodes(eye and !inPopUp())[index].performScrollTo().performClick()
    }

    private fun closeEyePopUp() {
        val close = inPopUp() and hasContentDescription(CLOSE) and hasClickAction()
        composeRule.waitUntilExactlyOneExists(close, TIMEOUT_MS)
        composeRule.onNode(close).performClick()
        composeRule.waitUntilDoesNotExist(close, TIMEOUT_MS)
    }

    /** For each Note text in the open pop-up, the words shown below it and above the next Note. */
    private fun obsBehUnderEachNoteInPopUp(noteTexts: List<String>): Map<String, List<String>> {
        val popUpTexts =
            composeRule
                .onAllNodes(
                    inPopUp() and SemanticsMatcher.keyIsDefined(SemanticsProperties.Text),
                    useUnmergedTree = true
                ).fetchSemanticsNodes()
        val notes =
            noteTexts
                .map { text -> text to popUpTexts.first { node -> text in node.texts() } }
                .sortedBy { (_, node) -> node.top }
        return notes.withIndex().associate { (index, note) ->
            val (text, node) = note
            val bottom = notes.getOrNull(index + 1)?.second?.top ?: Float.MAX_VALUE
            text to
                popUpTexts
                    .filter { it.top > node.top && it.top < bottom && it !== node }
                    .flatMap { other -> other.texts().flatMap { it.split(Regex("\\s+")) } }
        }
    }

    /** Taps [date]'s day in the open Material 3 date picker, which describes each day like "Sunday, November 1, 2026". */
    private fun pickInDatePicker(date: LocalDate) {
        val description = date.format(DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.US))
        composeRule.waitUntilAtLeastOneExists(hasText(description, substring = true), TIMEOUT_MS)
        composeRule.onNode(hasText(description, substring = true)).performClick()
        composeRule.onNodeWithText("OK").performClick()
    }

    /**
     * Sends the back key to the window on top, as the system does: a popup, such as a dropdown menu, closes. Compose
     * test input reaches only the composition, not the popup's window, and Espresso isn't on the test classpath.
     */
    private fun pressBackOnTopWindow() {
        val global = Class.forName("android.view.WindowManagerGlobal")
        val instance = global.getMethod("getInstance").invoke(null)
        val windows = global.getDeclaredField("mViews").apply { isAccessible = true }.get(instance) as List<*>
        val top = windows.last() as View
        top.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK))
        top.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK))
        composeRule.waitForIdle()
    }

    // Shared

    private fun inPopUp(): SemanticsMatcher = hasAnyAncestor(isDialog() or isPopup())

    private fun SemanticsNode.inPopUp(): Boolean {
        var parent = this.parent
        while (parent != null) {
            if (SemanticsProperties.IsDialog in parent.config || SemanticsProperties.IsPopup in parent.config) {
                return true
            }
            parent = parent.parent
        }
        return false
    }

    private fun exists(matcher: SemanticsMatcher): Boolean =
        composeRule.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty()

    private fun SemanticsNode.texts(): List<String> =
        config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text }

    // Unclipped, unlike boundsInRoot, so rows scrolled out of view still have their real place.
    private val SemanticsNode.top: Float get() = positionInRoot.y

    private companion object {
        const val TIMEOUT_MS = 5000L
        const val SESSION = "session"
        const val SESSION_ONE_CM = "session-one-cm"
        const val ROUTE_ONE_CM = "SLL → DOH"
        const val CONTINUE = "Continue last Session"
        const val DONE = "Done"
        const val GENERATE = "Generate debrief"
        const val DEBRIEF = "Debrief"
        const val BACK = "Back"
        const val SHOW_NOTES = "Show Notes"
        const val CLOSE = "Close"
        const val LEAVE = "Leave"
        const val STAY = "Stay"
        const val NO_GRADE_MESSAGE = "have no Grade"
        const val CM1 = "CM1"
        const val CM2 = "CM2"
        const val EFFECTIVE = "Effective"
        const val ADEQUATE = "Adequate"
        val GRADES =
            listOf(
                "Not Observed",
                "Exemplary",
                "Effective",
                "Adequate",
                "Minimum Acceptable Level",
                "Not Yet Competent"
            )
        val COMPETENCY_TITLE = Regex("""Competency (\d+)""")
    }
}
