package com.mebiad.checkpilot.acceptance

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mebiad.checkpilot.testutils.AppHarness
import com.mebiad.checkpilot.testutils.SessionListDriver
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

/**
 * US-10: Duplicate a Session. Each test seeds the Session DOH → SLL on 01 Nov 2026 with Staff Numbers 79125 and 04512,
 * Notes and Grades in their rows, unlocks the Session list with a new PIN, and duplicates the Session through its ⋮
 * menu's "Duplicate". Today is 27 Nov 2026 on the app's clock.
 *
 * How the tests find things, so restyling doesn't break them:
 * - The Session list, its rows, their Grade counts and their menus as [SessionListDriver] describes them.
 * - The capture screen is showing while its "Generate debrief" button is, and shows its Session's route and date.
 *   Note fields carry the test tag `note-field-<competencyId>`; an empty Competency shows one empty field.
 * - The Debrief opened from a row is CM1's. It is showing while its title "Debrief" stands alone, not clickable. Its
 *   summary header shows the Grade count as the spec writes it, "Grades 3/9", or as the design draws it, "3 of 9 Grades
 *   set", and each Grade selector shows its Grade's name.
 *
 * The screen is tall so every row of the Session list is laid out at once, whatever list it uses.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h3000dp")
@OptIn(ExperimentalTestApi::class)
class US10DuplicateASessionTest {
    private val harness = AppHarness()
    private val composeRule = createEmptyComposeRule()
    private val seeds = SessionSeeds(harness)
    private val list = SessionListDriver(composeRule)

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(harness).around(composeRule)

    /**
     * 001-mvp/US-10/AS1: **Given** a Session DOH → SLL on 01 Nov 2026 with Staff Numbers 79125 and 04512, Notes and
     * Grades, **when** the Assessor duplicates it, **then** a new Session SLL → DOH appears at the top of the list,
     * dated today, with Staff Numbers 79125 and 04512, no Notes and no Grades.
     *
     * A newer Session MCT → DOH sits above the original before it is duplicated.
     */
    @Test
    fun `001_mvp_US_10_AS1_duplicatingAddsTheReturnSectorTodayWithTheCrewAndNoNotesOrGrades`() {
        // Given
        harness.clock.setToday(TODAY)
        givenOriginalWithNotesAndGrades()
        givenNewerSession()
        harness.launch()
        list.unlockWithANewPin()
        composeRule.waitUntil(TIMEOUT_MS) { list.routesShown().size == 2 }
        assertEquals(listOf("MCT → DOH", "DOH → SLL"), list.routesShown())

        // When
        list.tapInMenuOf(list.route("DOH", "SLL"), DUPLICATE)

        // Then
        composeRule.waitUntil(TIMEOUT_MS) { list.routesShown().size == 3 }
        assertEquals(listOf("SLL → DOH", "MCT → DOH", "DOH → SLL"), list.routesShown())
        val duplicate = list.rowTexts(list.route("SLL", "DOH"))
        assertTrue("the duplicate is dated today: $duplicate", duplicate.any { "27 Nov 2026" in it })
        assertEquals("0/9", list.gradeCountRightOf(list.route("SLL", "DOH"), "79125"))
        assertEquals("0/9", list.gradeCountRightOf(list.route("SLL", "DOH"), "04512"))

        list.tapInRow(list.route("SLL", "DOH"), CAPTURE)
        waitForCapture(list.route("SLL", "DOH"))
        assertEquals(listOf(""), noteTexts(COMPETENCY_1))
        assertEquals(listOf(""), noteTexts(COMPETENCY_2))
        NOTES.forEach { composeRule.onNode(hasText(it, substring = true)).assertDoesNotExist() }
    }

    /**
     * 001-mvp/US-10/AS2: **Given** a duplicated Session, **when** the Assessor opens the original, **then** its route,
     * date, Notes and Grades are unchanged.
     */
    @Test
    fun `001_mvp_US_10_AS2_theOriginalKeepsItsRouteDateNotesAndGrades`() {
        // Given
        harness.clock.setToday(TODAY)
        givenOriginalWithNotesAndGrades()
        harness.launch()
        list.unlockWithANewPin()
        list.tapInMenuOf(list.route("DOH", "SLL"), DUPLICATE)
        composeRule.waitUntil(TIMEOUT_MS) { list.routesShown().size == 2 }

        // When
        list.tapInRow(list.route("DOH", "SLL"), CAPTURE)

        // Then
        waitForCapture(list.route("DOH", "SLL"))
        composeRule.onNode(list.route("SLL", "DOH")).assertDoesNotExist()
        composeRule.onNode(hasText("01 Nov 2026", substring = true) and !list.inPopUp()).assertExists()
        composeRule.onNode(hasText("27 Nov 2026", substring = true)).assertDoesNotExist()
        assertEquals(listOf(NOTES[0], NOTES[1], ""), noteTexts(COMPETENCY_1))
        assertEquals(listOf(NOTES[2], ""), noteTexts(COMPETENCY_2))

        harness.pressBack()
        list.waitForList()
        assertEquals("3/9", list.gradeCountRightOf(list.route("DOH", "SLL"), "79125"))
        assertEquals("9/9", list.gradeCountRightOf(list.route("DOH", "SLL"), "04512"))
        list.tapInRow(list.route("DOH", "SLL"), DEBRIEF)
        composeRule.waitUntilExactlyOneExists(debriefTitle(), TIMEOUT_MS)
        composeRule.waitUntilExactlyOneExists(gradesSummary(graded = 3, total = 9), TIMEOUT_MS)
        assertEquals(3, composeRule.onAllNodes(hasText(ADEQUATE) and !list.inPopUp()).fetchSemanticsNodes().size)
    }

    /**
     * 001-mvp/US-10/AS3: **Given** a Session was just duplicated, **when** the Assessor taps Continue last Session,
     * **then** the duplicate opens.
     *
     * Before the duplicate, the Last Session was a newer Session MCT → DOH.
     */
    @Test
    fun `001_mvp_US_10_AS3_continueLastSessionOpensTheDuplicate`() {
        // Given
        harness.clock.setToday(TODAY)
        givenOriginalWithNotesAndGrades()
        givenNewerSession()
        harness.launch()
        list.unlockWithANewPin()
        list.tapInMenuOf(list.route("DOH", "SLL"), DUPLICATE)
        composeRule.waitUntil(TIMEOUT_MS) { list.routesShown().size == 3 }
        harness.pressBack()

        // When
        val continueLast = hasText(CONTINUE, ignoreCase = true) and hasClickAction()
        composeRule.waitUntilAtLeastOneExists(continueLast, TIMEOUT_MS)
        composeRule.onNode(continueLast).performClick()

        // Then
        waitForCapture(list.route("SLL", "DOH"))
        composeRule.onNode(list.route("MCT", "DOH")).assertDoesNotExist()
        composeRule.onNode(list.route("DOH", "SLL")).assertDoesNotExist()
        composeRule.onNode(hasText("27 Nov 2026", substring = true) and !list.inPopUp()).assertExists()
    }

    // Given

    /** DOH → SLL on 01 Nov 2026, 79125 and 04512, Notes under Competencies 1 and 2, 3/9 Grades for CM1, 9/9 for CM2. */
    private fun givenOriginalWithNotesAndGrades() {
        seeds.session(
            ORIGINAL,
            cm1 = "79125",
            cm2 = "04512",
            departure = "DOH",
            destination = "SLL",
            date = LocalDate.of(2026, 11, 1),
            createdAt = Instant.parse("2026-11-01T08:00:00Z")
        )
        seeds.note(ORIGINAL, COMPETENCY_1, NOTES[0], cmTag = "CM1")
        seeds.note(ORIGINAL, COMPETENCY_1, NOTES[1], cmTag = "BOTH", observableBehaviour = "1a")
        seeds.note(ORIGINAL, COMPETENCY_2, NOTES[2], cmTag = "CM2")
        (1L..3L).forEach { seeds.grade(ORIGINAL, competencyId = it, crewMember = "CM1", value = "ADEQUATE") }
        (1L..9L).forEach { seeds.grade(ORIGINAL, competencyId = it, crewMember = "CM2", value = "NOT_OBSERVED") }
    }

    /** MCT → DOH, created after the original and before today, so it is the Last Session until a duplicate. */
    private fun givenNewerSession() {
        seeds.session(
            "newer",
            cm1 = "81530",
            cm2 = null,
            departure = "MCT",
            destination = "DOH",
            date = LocalDate.of(2026, 11, 20),
            createdAt = Instant.parse("2026-11-20T08:00:00Z")
        )
    }

    // Capture and Debrief

    private fun waitForCapture(route: SemanticsMatcher) {
        composeRule.waitUntilExactlyOneExists(
            hasText(GENERATE, substring = true, ignoreCase = true) and hasClickAction() and !list.inPopUp(),
            TIMEOUT_MS
        )
        composeRule.waitUntilExactlyOneExists(route, TIMEOUT_MS)
    }

    /** The texts of the Competency's Note fields, top to bottom, the trailing empty field included. */
    private fun noteTexts(competencyId: Long): List<String> {
        val tag = hasTestTag("note-field-$competencyId")
        composeRule.waitUntilAtLeastOneExists(tag, TIMEOUT_MS)
        return composeRule
            .onAllNodes(tag)
            .fetchSemanticsNodes()
            .map {
                it.config
                    .getOrNull(SemanticsProperties.EditableText)
                    ?.text
                    .orEmpty()
            }
    }

    private fun debriefTitle(): SemanticsMatcher = hasText(DEBRIEF) and !hasClickAction() and !list.inPopUp()

    /** Matches the Grade count in the summary header, as the spec writes it or as the design draws it. */
    private fun gradesSummary(
        graded: Int,
        total: Int,
    ): SemanticsMatcher {
        val pattern = Regex("""\s*(Grades $graded/$total|$graded of $total Grades set)\s*""")
        return SemanticsMatcher("shows $graded of $total Grades") { node ->
            node.config
                .getOrNull(SemanticsProperties.Text)
                .orEmpty()
                .any { pattern.matches(it.text) }
        } and !list.inPopUp()
    }

    private companion object {
        const val TIMEOUT_MS = 5000L
        const val ORIGINAL = "original"
        const val COMPETENCY_1 = 1L
        const val COMPETENCY_2 = 2L
        const val DUPLICATE = "Duplicate"
        const val CAPTURE = "Capture"
        const val DEBRIEF = "Debrief"
        const val CONTINUE = "Continue last Session"
        const val GENERATE = "Generate debrief"
        const val ADEQUATE = "Adequate"
        val TODAY: LocalDate = LocalDate.of(2026, 11, 27)
        val NOTES = listOf("Late gear", "Briefed the taxi route", "Missed the callout")
    }
}
