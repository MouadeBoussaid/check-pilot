package com.mebiad.checkpilot.acceptance

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mebiad.checkpilot.data.local.db.CheckPilotDatabase
import com.mebiad.checkpilot.domain.session.AirportCode
import com.mebiad.checkpilot.domain.session.Session
import com.mebiad.checkpilot.domain.session.SessionRepository
import com.mebiad.checkpilot.domain.session.StaffNumber
import com.mebiad.checkpilot.testutils.AppHarness
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate

/**
 * US-2: Capture Notes. Each test seeds a Session with two Crew Members, and Notes in its database rows where the Given
 * needs them, opens it with Continue last Session, then types, backspaces, taps away, taps ✕ and Undo on the capture
 * screen. Note fields carry the test tag `note-field-<competencyId>`, in screen order, because an empty field has no
 * text to find it by.
 */
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class US2CaptureNotesTest {
    private val harness = AppHarness()
    private val composeRule = createEmptyComposeRule()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(harness).around(composeRule)

    /**
     * 001-mvp/US-2/AS1: **Given** a Competency with no Notes, **when** the capture screen shows it, **then** it has
     * exactly one empty Note field.
     */
    @Test
    fun `001_mvp_US_2_AS1_aCompetencyWithoutNotesHasOneEmptyNoteField`() {
        // Given
        seedSession()

        // When
        openLastSession()

        // Then
        waitForNoteTexts(COMPETENCY_1, listOf(""))
    }

    /**
     * 001-mvp/US-2/AS2: **Given** the empty Note field of a Competency, **when** the Assessor types text into it,
     * **then** a new empty Note field appears below it.
     */
    @Test
    fun `001_mvp_US_2_AS2_typingIntoTheEmptyFieldAddsANewEmptyFieldBelow`() {
        // Given
        seedSession()
        openLastSession()
        waitForNoteTexts(COMPETENCY_1, listOf(""))

        // When
        noteField(COMPETENCY_1, index = 0).performTextInput("Checklist read silently")

        // Then
        waitForNoteTexts(COMPETENCY_1, listOf("Checklist read silently", ""))
    }

    /**
     * 001-mvp/US-2/AS3: **Given** a Note the Assessor has backspaced to empty and is still typing in, **when** the
     * Assessor keeps typing, **then** the Note field stays in place.
     */
    @Test
    fun `001_mvp_US_2_AS3_aNoteBackspacedToEmptyStaysInPlaceWhileTyping`() {
        // Given
        seedSession()
        seedNote(id = "first", text = "Checklist read silently", sortIndex = 1)
        seedNote(id = "second", text = "Late flaps", sortIndex = 2)
        openLastSession()
        waitForNoteTexts(COMPETENCY_1, listOf("Checklist read silently", "Late flaps", ""))
        noteField(COMPETENCY_1, index = 0).performClick()
        noteField(COMPETENCY_1, index = 0).performTextClearance()
        waitForNoteTexts(COMPETENCY_1, listOf("", "Late flaps", ""))

        // When
        noteField(COMPETENCY_1, index = 0).performTextInput("Checklist read aloud")

        // Then
        waitForNoteTexts(COMPETENCY_1, listOf("Checklist read aloud", "Late flaps", ""))
    }

    /**
     * 001-mvp/US-2/AS4: **Given** a Note backspaced to empty that isn't the last field, **when** the Assessor taps
     * outside it, **then** the Note disappears and an undo snackbar appears.
     */
    @Test
    fun `001_mvp_US_2_AS4_anEmptiedNoteDisappearsOnTapOutsideWithAnUndoSnackbar`() {
        // Given
        seedSession()
        seedNote(id = "first", text = "Checklist read silently", sortIndex = 1)
        seedNote(id = "second", text = "Late flaps", sortIndex = 2)
        openLastSession()
        waitForNoteTexts(COMPETENCY_1, listOf("Checklist read silently", "Late flaps", ""))
        noteField(COMPETENCY_1, index = 0).performClick()
        noteField(COMPETENCY_1, index = 0).performTextClearance()
        waitForNoteTexts(COMPETENCY_1, listOf("", "Late flaps", ""))

        // When
        tapOutsideTheNotes()

        // Then
        waitForNoteTexts(COMPETENCY_1, listOf("Late flaps", ""))
        composeRule.waitUntilExactlyOneExists(hasText(UNDO), TIMEOUT_MS)
    }

    /**
     * 001-mvp/US-2/AS5: **Given** the undo snackbar after an emptied Note disappeared, **when** the Assessor taps Undo,
     * **then** the Note comes back in the same place with the text, CM Tag and OBS BEH it had before it was emptied.
     */
    @Test
    fun `001_mvp_US_2_AS5_undoBringsAnEmptiedNoteBackWithItsTextCmTagAndObsBeh`() {
        // Given
        seedSession()
        val obsBeh1b = observableBehaviourId("1b")
        seedNote(id = "first", text = "Checklist read silently", sortIndex = 1, cmTag = "CM2", obsBehId = obsBeh1b)
        seedNote(id = "second", text = "Late flaps", sortIndex = 2)
        openLastSession()
        waitForNoteTexts(COMPETENCY_1, listOf("Checklist read silently", "Late flaps", ""))
        noteField(COMPETENCY_1, index = 0).performClick()
        noteField(COMPETENCY_1, index = 0).performTextClearance()
        waitForNoteTexts(COMPETENCY_1, listOf("", "Late flaps", ""))
        tapOutsideTheNotes()
        composeRule.waitUntilExactlyOneExists(hasText(UNDO), TIMEOUT_MS)

        // When
        composeRule.onNodeWithText(UNDO).performClick()

        // Then
        waitForNoteTexts(COMPETENCY_1, listOf("Checklist read silently", "Late flaps", ""))
        waitForNoteRow(
            NoteRow(id = "first", text = "Checklist read silently", cmTag = "CM2", obsBehId = obsBeh1b, sortIndex = 1)
        )
    }

    /**
     * 001-mvp/US-2/AS6: **Given** a Competency with three Notes and the second one focused, **when** the Assessor taps
     * its ✕, **then** only that Note disappears, the other two stay, and an undo snackbar appears.
     */
    @Test
    fun `001_mvp_US_2_AS6_tappingTheFocusedNotesXRemovesOnlyThatNoteWithAnUndoSnackbar`() {
        // Given
        seedSession()
        seedNote(id = "first", text = "Checklist read silently", sortIndex = 1)
        seedNote(id = "second", text = "Late flaps", sortIndex = 2)
        seedNote(id = "third", text = "Missed the go-around call", sortIndex = 3)
        openLastSession()
        waitForNoteTexts(COMPETENCY_1, listOf("Checklist read silently", "Late flaps", "Missed the go-around call", ""))
        noteField(COMPETENCY_1, index = 1).performClick()

        // When
        composeRule.waitUntilExactlyOneExists(hasContentDescription(DELETE_NOTE), TIMEOUT_MS)
        composeRule.onNodeWithContentDescription(DELETE_NOTE).performClick()

        // Then
        waitForNoteTexts(COMPETENCY_1, listOf("Checklist read silently", "Missed the go-around call", ""))
        composeRule.waitUntilExactlyOneExists(hasText(UNDO), TIMEOUT_MS)
    }

    /**
     * 001-mvp/US-2/AS7: **Given** the undo snackbar after a ✕, **when** the Assessor taps Undo, **then** the Note comes
     * back in the same place with its text, CM Tag and OBS BEH.
     */
    @Test
    fun `001_mvp_US_2_AS7_undoAfterXBringsTheNoteBackWithItsTextCmTagAndObsBeh`() {
        // Given
        seedSession()
        val obsBeh1c = observableBehaviourId("1c")
        seedNote(id = "first", text = "Checklist read silently", sortIndex = 1)
        seedNote(id = "second", text = "Late flaps", sortIndex = 2, cmTag = "CM1", obsBehId = obsBeh1c)
        seedNote(id = "third", text = "Missed the go-around call", sortIndex = 3)
        openLastSession()
        waitForNoteTexts(COMPETENCY_1, listOf("Checklist read silently", "Late flaps", "Missed the go-around call", ""))
        noteField(COMPETENCY_1, index = 1).performClick()
        composeRule.waitUntilExactlyOneExists(hasContentDescription(DELETE_NOTE), TIMEOUT_MS)
        composeRule.onNodeWithContentDescription(DELETE_NOTE).performClick()
        composeRule.waitUntilExactlyOneExists(hasText(UNDO), TIMEOUT_MS)

        // When
        composeRule.onNodeWithText(UNDO).performClick()

        // Then
        waitForNoteTexts(COMPETENCY_1, listOf("Checklist read silently", "Late flaps", "Missed the go-around call", ""))
        waitForNoteRow(NoteRow(id = "second", text = "Late flaps", cmTag = "CM1", obsBehId = obsBeh1c, sortIndex = 2))
    }

    /**
     * 001-mvp/US-2/AS8: **Given** Notes typed in a Session, **when** the app is closed and the Session is opened again,
     * **then** every Note is there, without the Assessor ever having saved.
     */
    @Test
    fun `001_mvp_US_2_AS8_typedNotesAreThereAfterTheAppIsClosedAndTheSessionOpenedAgain`() {
        // Given
        seedSession()
        openLastSession()
        waitForNoteTexts(COMPETENCY_1, listOf(""))
        noteField(COMPETENCY_1, index = 0).performTextInput("Checklist read silently")
        waitForNoteTexts(COMPETENCY_1, listOf("Checklist read silently", ""))
        noteField(COMPETENCY_1, index = 1).performTextInput("Late flaps")
        waitForNoteTexts(COMPETENCY_1, listOf("Checklist read silently", "Late flaps", ""))

        // When
        harness.restart()
        openLastSession()

        // Then
        waitForNoteTexts(COMPETENCY_1, listOf("Checklist read silently", "Late flaps", ""))
    }

    /**
     * 001-mvp/US-2/AS9: **Given** the capture screen, **when** the Assessor taps Done, **then** the home screen opens.
     */
    @Test
    fun `001_mvp_US_2_AS9_doneOpensTheHomeScreen`() {
        // Given
        seedSession()
        openLastSession()

        // When
        composeRule.waitUntilExactlyOneExists(hasText("Done"), TIMEOUT_MS)
        composeRule.onNodeWithText("Done").performScrollTo().performClick()

        // Then
        composeRule.waitUntilAtLeastOneExists(hasText("New Session", ignoreCase = true), TIMEOUT_MS)
        composeRule.onNodeWithText(CONTINUE, ignoreCase = true).assertExists()
        composeRule.onNodeWithText("Done").assertDoesNotExist()
    }

    private fun openLastSession() {
        composeRule.waitUntilAtLeastOneExists(hasText(CONTINUE, ignoreCase = true), TIMEOUT_MS)
        composeRule.onNodeWithText(CONTINUE, ignoreCase = true).performClick()
        composeRule.waitUntilExactlyOneExists(hasText(ROUTE), TIMEOUT_MS)
    }

    /** Taps the route at the top of the capture screen, away from every Note. */
    private fun tapOutsideTheNotes() {
        composeRule.onNodeWithText(ROUTE).performClick()
    }

    private fun noteField(
        competencyId: Long,
        index: Int,
    ): SemanticsNodeInteraction = composeRule.onAllNodesWithTag(noteFieldTag(competencyId))[index]

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

    private fun noteTexts(competencyId: Long): List<String> =
        composeRule
            .onAllNodes(hasTestTag(noteFieldTag(competencyId)))
            .fetchSemanticsNodes()
            .map {
                it.config
                    .getOrNull(SemanticsProperties.EditableText)
                    ?.text
                    .orEmpty()
            }

    private fun noteFieldTag(competencyId: Long): String = "note-field-$competencyId"

    private fun seedSession() {
        val session =
            Session(
                id = SESSION_ID,
                createdAt = Instant.parse("2026-11-01T08:00:00Z"),
                date = LocalDate.of(2026, 11, 1),
                cm1StaffNumber = StaffNumber.parse("79125"),
                cm2StaffNumber = StaffNumber.parse("04512"),
                departure = checkNotNull(AirportCode.parse("DOH")),
                destination = checkNotNull(AirportCode.parse("SLL"))
            )
        runBlocking { harness.get<SessionRepository>().insert(session) }
        harness.launch()
    }

    /** Writes a Note's row under Competency 1 straight into the database; the CM Tag and OBS BEH screens are US-3's. */
    private fun seedNote(
        id: String,
        text: String,
        sortIndex: Long,
        cmTag: String = "BOTH",
        obsBehId: Long? = null,
    ) {
        database().execSQL(
            "INSERT INTO note (id, sessionId, competencyId, text, cmTag, observableBehaviourId, sortIndex) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)",
            arrayOf<Any?>(id, SESSION_ID, COMPETENCY_1, text, cmTag, obsBehId, sortIndex)
        )
    }

    private fun observableBehaviourId(name: String): Long =
        database().query("SELECT id FROM observable_behaviour WHERE name = ?", arrayOf(name)).use { cursor ->
            check(cursor.moveToFirst()) { "No OBS BEH named $name" }
            cursor.getLong(0)
        }

    private fun waitForNoteRow(expected: NoteRow) {
        val reached = runCatching { composeRule.waitUntil(TIMEOUT_MS) { noteRow(expected.id) == expected } }
        if (reached.isFailure) {
            assertEquals("Note row ${expected.id}", expected, noteRow(expected.id))
        }
    }

    private fun noteRow(id: String): NoteRow? =
        database()
            .query(
                "SELECT id, text, cmTag, observableBehaviourId, sortIndex FROM note WHERE id = ? AND competencyId = ?",
                arrayOf<Any>(id, COMPETENCY_1)
            ).use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                NoteRow(
                    id = cursor.getString(0),
                    text = cursor.getString(1),
                    cmTag = cursor.getString(2),
                    obsBehId = if (cursor.isNull(3)) null else cursor.getLong(3),
                    sortIndex = cursor.getLong(4)
                )
            }

    private fun database() = harness.get<CheckPilotDatabase>().openHelper.writableDatabase

    private data class NoteRow(
        val id: String,
        val text: String,
        val cmTag: String,
        val obsBehId: Long?,
        val sortIndex: Long,
    )

    private companion object {
        const val TIMEOUT_MS = 5000L
        const val SESSION_ID = "session"
        const val COMPETENCY_1 = 1L
        const val ROUTE = "DOH → SLL"
        const val CONTINUE = "Continue last Session"
        const val UNDO = "Undo"
        const val DELETE_NOTE = "Delete Note"
    }
}
