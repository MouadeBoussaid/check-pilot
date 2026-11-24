package com.mebiad.checkpilot.acceptance

import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mebiad.checkpilot.testutils.AppHarness
import com.mebiad.checkpilot.testutils.SessionSeeds
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * US-6: Move a Note to another Competency. Each test seeds a Session with two Crew Members and Notes in their database
 * rows, opens it with Continue last Session, focuses a Note, taps its move button and picks the Competency to move it
 * to, and taps Undo on the snackbar.
 *
 * How the tests find things, so restyling doesn't break them:
 * - Note fields carry the test tag `note-field-<competencyId>`, in screen order; the last one of a Competency is its
 *   empty field.
 * - The move button of the focused Note is described "Move Note". In the picker it opens, each Competency it can move
 *   to is the clickable node with that Competency's title, such as "Competency 5"; the titles on the capture screen
 *   aren't clickable.
 * - A label counts as on a Note when it lies between the top of that Note's field and the next field of its
 *   Competency: its CM Tag badge and its OBS BEH. A label counts as under a Competency's title when it lies between
 *   the title and its first Note field.
 * - Before checking a Note's labels, the test taps the route at the top of the screen, so no Note is being edited.
 *
 * The screen is tall so every Competency of the capture screen is laid out at once.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h3000dp")
@OptIn(ExperimentalTestApi::class)
class US6MoveANoteToAnotherCompetencyTest {
    private val harness = AppHarness()
    private val composeRule = createEmptyComposeRule()
    private val seeds = SessionSeeds(harness)

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(harness).around(composeRule)

    /**
     * 001-mvp/US-6/AS1: **Given** a Note tagged CM2 under Competency 2, **when** the Assessor moves it to Competency 5,
     * **then** it is gone from Competency 2 and appears at the end of Competency 5's Notes, above the empty field, with
     * the same text and CM Tag.
     */
    @Test
    fun `001_mvp_US_6_AS1_aMovedNoteLeavesCompetency2AndEndsCompetency5sNotesWithItsTextAndCmTag`() {
        // Given
        seeds.session(SESSION)
        seeds.note(SESSION, COMPETENCY_2, text = "Missed ATC frequency change")
        seeds.note(SESSION, COMPETENCY_2, id = MOVED, text = READBACK, cmTag = "CM2")
        seeds.note(SESSION, COMPETENCY_5, text = "Briefed the threats")
        harness.launch()
        openLastSession()
        waitForNoteTexts(COMPETENCY_2, listOf("Missed ATC frequency change", READBACK, ""))
        waitForNoteTexts(COMPETENCY_5, listOf("Briefed the threats", ""))

        // When
        moveNote(COMPETENCY_2, index = 1, to = COMPETENCY_5)

        // Then
        waitForNoteTexts(COMPETENCY_2, listOf("Missed ATC frequency change", ""))
        waitForNoteTexts(COMPETENCY_5, listOf("Briefed the threats", READBACK, ""))
        tapOutsideTheNotes()
        waitForCount("CM2 on the moved Note", expected = 1) {
            labelsOnNote(COMPETENCY_5, index = 1).count { it == CM2 }
        }
    }

    /**
     * 001-mvp/US-6/AS2: **Given** a Note linked to OBS BEH 2b, **when** the Assessor moves it to Competency 5, **then**
     * it arrives without an OBS BEH and an undo snackbar appears.
     */
    @Test
    fun `001_mvp_US_6_AS2_aNoteLinkedTo2bArrivesInCompetency5WithoutAnObsBehAndAnUndoSnackbar`() {
        // Given
        seeds.session(SESSION)
        seeds.note(SESSION, COMPETENCY_2, id = MOVED, text = READBACK, observableBehaviour = "2b")
        harness.launch()
        openLastSession()
        waitForCount("2b on the Note", expected = 1) { labelsOnNote(COMPETENCY_2, index = 0).count { it == "2b" } }

        // When
        moveNote(COMPETENCY_2, index = 0, to = COMPETENCY_5)

        // Then
        composeRule.waitUntilExactlyOneExists(hasText(UNDO), TIMEOUT_MS)
        waitForNoteTexts(COMPETENCY_5, listOf(READBACK, ""))
        tapOutsideTheNotes()
        waitForLabels("Labels on the moved Note besides its text", expected = emptyList()) {
            labelsOnNote(COMPETENCY_5, index = 0).filterNot { it in READBACK_WORDS }
        }
        waitForLabels("Labels under Competency 5's title", expected = emptyList()) { labelsUnderTitle(COMPETENCY_5) }
    }

    /**
     * 001-mvp/US-6/AS3: **Given** the undo snackbar after a move, **when** the Assessor taps Undo, **then** the Note is
     * back in Competency 2 in its old place, linked to 2b again.
     */
    @Test
    fun `001_mvp_US_6_AS3_undoPutsTheNoteBackInItsOldPlaceInCompetency2LinkedTo2b`() {
        // Given
        seeds.session(SESSION)
        seeds.note(SESSION, COMPETENCY_2, text = "Missed ATC frequency change")
        seeds.note(SESSION, COMPETENCY_2, id = MOVED, text = READBACK, observableBehaviour = "2b")
        seeds.note(SESSION, COMPETENCY_2, text = "Late handover to tower")
        harness.launch()
        openLastSession()
        waitForNoteTexts(COMPETENCY_2, listOf("Missed ATC frequency change", READBACK, "Late handover to tower", ""))
        moveNote(COMPETENCY_2, index = 1, to = COMPETENCY_5)
        waitForNoteTexts(COMPETENCY_5, listOf(READBACK, ""))
        composeRule.waitUntilExactlyOneExists(hasText(UNDO), TIMEOUT_MS)

        // When
        composeRule.onNodeWithText(UNDO).performClick()

        // Then
        waitForNoteTexts(COMPETENCY_2, listOf("Missed ATC frequency change", READBACK, "Late handover to tower", ""))
        waitForNoteTexts(COMPETENCY_5, listOf(""))
        tapOutsideTheNotes()
        waitForCount("2b on the Note", expected = 1) { labelsOnNote(COMPETENCY_2, index = 1).count { it == "2b" } }
        waitForCount("2b under Competency 2's title", expected = 1) {
            labelsUnderTitle(COMPETENCY_2).count { it == "2b" }
        }
    }

    /**
     * 001-mvp/US-6/AS4: **Given** the only Note under Competency 2 linked to 2b, **when** the Assessor moves it to
     * Competency 5, **then** 2b no longer appears under Competency 2's title.
     */
    @Test
    fun `001_mvp_US_6_AS4_movingTheOnlyNoteLinkedTo2bRemoves2bFromUnderCompetency2sTitle`() {
        // Given
        seeds.session(SESSION)
        seeds.note(SESSION, COMPETENCY_2, id = MOVED, text = READBACK, observableBehaviour = "2b")
        seeds.note(SESSION, COMPETENCY_2, text = "Missed ATC frequency change")
        harness.launch()
        openLastSession()
        waitForCount("2b under Competency 2's title", expected = 1) {
            labelsUnderTitle(COMPETENCY_2).count { it == "2b" }
        }

        // When
        moveNote(COMPETENCY_2, index = 0, to = COMPETENCY_5)

        // Then
        waitForNoteTexts(COMPETENCY_2, listOf("Missed ATC frequency change", ""))
        tapOutsideTheNotes()
        waitForCount("2b under Competency 2's title", expected = 0) {
            labelsUnderTitle(COMPETENCY_2).count { it == "2b" }
        }
    }

    private fun openLastSession() {
        composeRule.waitUntilAtLeastOneExists(hasText(CONTINUE, ignoreCase = true), TIMEOUT_MS)
        composeRule.onNodeWithText(CONTINUE, ignoreCase = true).performClick()
        composeRule.waitUntilExactlyOneExists(hasText(ROUTE), TIMEOUT_MS)
        composeRule.waitUntil(TIMEOUT_MS) { noteFieldNodes(COMPETENCY_5).isNotEmpty() }
    }

    /** Focuses the Note at [index] of [competencyId], taps its move button and picks the Competency [to]. */
    private fun moveNote(
        competencyId: Long,
        index: Int,
        to: Long,
    ) {
        val field = noteField(competencyId, index)
        field.performScrollTo().performClick()
        field.assertIsFocused()
        composeRule.waitUntilExactlyOneExists(hasContentDescription(MOVE_NOTE), TIMEOUT_MS)
        tap(hasContentDescription(MOVE_NOTE))
        val target = hasText("Competency $to") and hasClickAction()
        composeRule.waitUntilExactlyOneExists(target, TIMEOUT_MS)
        tap(target)
    }

    /** Scrolls the one node matching [matcher] into view, where it sits in a scrolling list, and taps it. */
    private fun tap(matcher: SemanticsMatcher) {
        val scrollable = composeRule.onAllNodes(matcher and hasAnyAncestor(hasScrollAction())).fetchSemanticsNodes()
        if (scrollable.isNotEmpty()) composeRule.onNode(matcher).performScrollTo()
        composeRule.onNode(matcher).performClick()
    }

    /** Taps the route at the top of the capture screen, away from every Note. */
    private fun tapOutsideTheNotes() {
        composeRule.onNode(hasText(ROUTE)).performClick()
    }

    private fun noteField(
        competencyId: Long,
        index: Int,
    ): SemanticsNodeInteraction = composeRule.onAllNodesWithTag(noteFieldTag(competencyId))[index]

    private fun noteFieldTag(competencyId: Long): String = "note-field-$competencyId"

    private fun noteFieldNodes(competencyId: Long): List<SemanticsNode> =
        composeRule.onAllNodes(hasTestTag(noteFieldTag(competencyId))).fetchSemanticsNodes()

    private fun noteTexts(competencyId: Long): List<String> =
        noteFieldNodes(competencyId).map {
            it.config
                .getOrNull(SemanticsProperties.EditableText)
                ?.text
                .orEmpty()
        }

    /** Waits until the Note fields of [competencyId] show [expected], top to bottom, and fails with what they show. */
    private fun waitForNoteTexts(
        competencyId: Long,
        expected: List<String>,
    ) {
        val reached = runCatching { composeRule.waitUntil(TIMEOUT_MS) { noteTexts(competencyId) == expected } }
        if (reached.isFailure) {
            assertEquals(
                "Note fields of Competency $competencyId",
                expected.map { "\"$it\"" },
                noteTexts(competencyId).map { "\"$it\"" }
            )
        }
    }

    /** The words shown from the top of the Note field at [index] down to the next field of its Competency. */
    private fun labelsOnNote(
        competencyId: Long,
        index: Int,
    ): List<String> {
        val fields = noteFieldNodes(competencyId)
        val note = fields.getOrNull(index) ?: return emptyList()
        val next = fields.getOrNull(index + 1) ?: return emptyList()
        return wordsBetween(top = note.top, bottom = next.top)
    }

    /** The words shown between the Competency's title and its first Note field. */
    private fun labelsUnderTitle(competencyId: Long): List<String> {
        val title =
            composeRule
                .onAllNodes(hasText("Competency $competencyId") and !hasAnyAncestor(isPopup()))
                .fetchSemanticsNodes()
                .firstOrNull() ?: return emptyList()
        val firstField = noteFieldNodes(competencyId).firstOrNull() ?: return emptyList()
        return wordsBetween(top = title.bottom, bottom = firstField.top)
    }

    /** The words of every text on the capture screen, outside popups, lying wholly between [top] and [bottom]. */
    private fun wordsBetween(
        top: Float,
        bottom: Float,
    ): List<String> =
        composeRule
            .onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text) and !hasAnyAncestor(isPopup()))
            .fetchSemanticsNodes()
            .filter { it.top >= top && it.bottom <= bottom }
            .flatMap { node -> node.texts().flatMap { it.split(Regex("\\s+")) } }
            .filter { it.isNotEmpty() }

    private fun SemanticsNode.texts(): List<String> =
        config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text }

    // Unclipped, unlike boundsInRoot, so Competencies scrolled out of view still have their real place.
    private val SemanticsNode.top: Float get() = positionInRoot.y
    private val SemanticsNode.bottom: Float get() = positionInRoot.y + size.height

    /** Waits until [actual] reaches [expected], and fails with what it shows. */
    private fun waitForCount(
        what: String,
        expected: Int,
        actual: () -> Int,
    ) {
        val reached = runCatching { composeRule.waitUntil(TIMEOUT_MS) { actual() == expected } }
        if (reached.isFailure) assertEquals(what, expected, actual())
    }

    /** Waits until [actual] shows [expected], and fails with what it shows. */
    private fun waitForLabels(
        what: String,
        expected: List<String>,
        actual: () -> List<String>,
    ) {
        val reached = runCatching { composeRule.waitUntil(TIMEOUT_MS) { actual() == expected } }
        if (reached.isFailure) assertEquals(what, expected, actual())
    }

    private companion object {
        const val TIMEOUT_MS = 5000L
        const val SESSION = "session"
        const val MOVED = "moved"
        const val COMPETENCY_2 = 2L
        const val COMPETENCY_5 = 5L
        const val READBACK = "Readback of altitude clearance incomplete"
        val READBACK_WORDS = READBACK.split(" ")
        const val ROUTE = "DOH → SLL"
        const val CONTINUE = "Continue last Session"
        const val UNDO = "Undo"
        const val MOVE_NOTE = "Move Note"
        const val CM2 = "CM2"
    }
}
