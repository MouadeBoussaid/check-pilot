package com.mebiad.checkpilot.acceptance

import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
 * US-3: Tag a Note and link an Observable Behaviour. Each test seeds a Session and Notes in their database rows, opens
 * it with Continue last Session, then focuses Notes, taps the CM Tag selector and picks from the OBS BEH dropdown on
 * the capture screen.
 *
 * Note fields carry the test tag `note-field-<competencyId>`, in screen order. The CM Tag options are the selectable
 * nodes labelled CM1, CM2 and BOTH (the Staff Number chips also say CM1 and CM2, but aren't selectable). The OBS BEH
 * dropdown is the clickable node labelled "OBS BEH", and its choices are the clickable items of the popup it opens,
 * with "None" for no OBS BEH. An OBS BEH label counts as under a Note when it sits between that Note's field and the
 * next field of its Competency, and under a Competency's title when it sits between the title and its first Note
 * field. As the technical plan says, the tests don't check styling, so "small italic" isn't checked.
 */
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class US3TagANoteAndLinkAnObservableBehaviourTest {
    private val harness = AppHarness()
    private val composeRule = createEmptyComposeRule()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(harness).around(composeRule)

    /**
     * 001-mvp/US-3/AS1: **Given** a Session with two Crew Members, **when** the Assessor focuses a Note, **then** the
     * CM Tag selector CM1 / CM2 / BOTH appears under it with BOTH selected.
     */
    @Test
    fun `001_mvp_US_3_AS1_focusingANoteWithTwoCrewMembersShowsTheCmTagSelectorWithBothSelected`() {
        // Given
        seedSession(SESSION_TWO_CMS, twoCrewMembers = true, createdAt = MORNING)
        seedNote(SESSION_TWO_CMS, COMPETENCY_1, id = "note", text = "Readback incomplete", sortIndex = 0)
        harness.launch()
        openLastSession()

        // When
        focus(noteField(COMPETENCY_1, index = 0))

        // Then
        composeRule.waitUntilExactlyOneExists(cmTagOption(BOTH), TIMEOUT_MS)
        composeRule.onNode(cmTagOption(BOTH)).assertIsSelected()
        composeRule.onNode(cmTagOption(CM1)).assertIsNotSelected()
        composeRule.onNode(cmTagOption(CM2)).assertIsNotSelected()
        val note = noteField(COMPETENCY_1, index = 0).fetchSemanticsNode()
        val nextField = noteField(COMPETENCY_1, index = 1).fetchSemanticsNode()
        listOf(CM1, CM2, BOTH).forEach { label ->
            val option = composeRule.onNode(cmTagOption(label)).fetchSemanticsNode()
            check(option.top >= note.bottom && option.bottom <= nextField.top) {
                "The $label option isn't under the focused Note"
            }
        }
    }

    /**
     * 001-mvp/US-3/AS2: **Given** a focused Note tagged BOTH, **when** the Assessor taps CM1 and later taps BOTH again,
     * **then** the selector shows CM1 selected and then BOTH selected.
     */
    @Test
    fun `001_mvp_US_3_AS2_tappingCm1AndThenBothSelectsEachInTurn`() {
        // Given
        seedSession(SESSION_TWO_CMS, twoCrewMembers = true, createdAt = MORNING)
        seedNote(SESSION_TWO_CMS, COMPETENCY_1, id = "note", text = "Readback incomplete", sortIndex = 0)
        harness.launch()
        openLastSession()
        focus(noteField(COMPETENCY_1, index = 0))
        composeRule.waitUntilExactlyOneExists(cmTagOption(BOTH), TIMEOUT_MS)
        composeRule.onNode(cmTagOption(BOTH)).assertIsSelected()

        // When / Then: CM1 tapped
        composeRule.onNode(cmTagOption(CM1)).performClick()
        composeRule.onNode(cmTagOption(CM1)).assertIsSelected()
        composeRule.onNode(cmTagOption(CM2)).assertIsNotSelected()
        composeRule.onNode(cmTagOption(BOTH)).assertIsNotSelected()

        // When / Then: BOTH tapped again
        composeRule.onNode(cmTagOption(BOTH)).performClick()
        composeRule.onNode(cmTagOption(BOTH)).assertIsSelected()
        composeRule.onNode(cmTagOption(CM1)).assertIsNotSelected()
        composeRule.onNode(cmTagOption(CM2)).assertIsNotSelected()
    }

    /**
     * 001-mvp/US-3/AS3: **Given** one Session with one Crew Member and one with two, **when** the Assessor focuses a
     * Note in each, **then** only the Session with two shows the CM Tag selector.
     *
     * Only the Last Session can be opened before the Session list exists (US-7), so the Session with one Crew Member is
     * created later, after the Session with two was checked, and becomes the Last Session.
     */
    @Test
    fun `001_mvp_US_3_AS3_onlyASessionWithTwoCrewMembersShowsTheCmTagSelector`() {
        // Given
        seedSession(SESSION_TWO_CMS, twoCrewMembers = true, createdAt = MORNING)
        seedNote(SESSION_TWO_CMS, COMPETENCY_1, id = "note-two", text = "Readback incomplete", sortIndex = 0)
        harness.launch()

        // When / Then: a Note focused in the Session with two Crew Members
        openLastSession()
        focus(noteField(COMPETENCY_1, index = 0))
        composeRule.waitUntilExactlyOneExists(cmTagOption(BOTH), TIMEOUT_MS)
        composeRule.onNode(cmTagOption(CM1)).assertExists()
        composeRule.onNode(cmTagOption(CM2)).assertExists()

        // When / Then: a Note focused in the Session with one Crew Member
        composeRule.onNodeWithText(DONE).performScrollTo().performClick()
        composeRule.waitUntilAtLeastOneExists(hasText(CONTINUE, ignoreCase = true), TIMEOUT_MS)
        seedSession(SESSION_ONE_CM, twoCrewMembers = false, createdAt = MORNING.plusSeconds(3600))
        seedNote(SESSION_ONE_CM, COMPETENCY_1, id = "note-one", text = "Late flaps", sortIndex = 0)
        composeRule.waitUntilExactlyOneExists(hasText(ROUTE_ONE_CM), TIMEOUT_MS)
        openLastSession(route = ROUTE_ONE_CM)
        composeRule.waitUntilExactlyOneExists(hasText("Late flaps"), TIMEOUT_MS)
        focus(noteField(COMPETENCY_1, index = 0))
        composeRule.onNode(cmTagOption(CM1)).assertDoesNotExist()
        composeRule.onNode(cmTagOption(CM2)).assertDoesNotExist()
        composeRule.onNode(cmTagOption(BOTH)).assertDoesNotExist()
    }

    /**
     * 001-mvp/US-3/AS4: **Given** an empty Note field and a Note with text, **when** the Assessor focuses each,
     * **then** only the Note with text offers the OBS BEH dropdown.
     */
    @Test
    fun `001_mvp_US_3_AS4_onlyANoteWithTextOffersTheObsBehDropdown`() {
        // Given
        seedSession(SESSION_TWO_CMS, twoCrewMembers = true, createdAt = MORNING)
        seedNote(SESSION_TWO_CMS, COMPETENCY_3, id = "note", text = "Late flaps", sortIndex = 0)
        harness.launch()
        openLastSession()

        // When / Then: the Note with text focused
        focus(noteField(COMPETENCY_3, index = 0))
        composeRule.waitUntilExactlyOneExists(obsBehDropdown(), TIMEOUT_MS)

        // When / Then: the empty Note field focused
        focus(noteField(COMPETENCY_3, index = 1))
        composeRule.waitUntilDoesNotExist(obsBehDropdown(), TIMEOUT_MS)
    }

    /**
     * 001-mvp/US-3/AS5: **Given** a Note with text under Competency 3, **when** the Assessor opens the OBS BEH
     * dropdown, **then** it lists only Competency 3's OBS BEH, in Catalogue order.
     */
    @Test
    fun `001_mvp_US_3_AS5_theObsBehDropdownListsOnlyTheCompetencysObsBehInCatalogueOrder`() {
        // Given
        seedSession(SESSION_TWO_CMS, twoCrewMembers = true, createdAt = MORNING)
        seedNote(SESSION_TWO_CMS, COMPETENCY_3, id = "note", text = "Late flaps", sortIndex = 0)
        harness.launch()
        openLastSession()
        focus(noteField(COMPETENCY_3, index = 0))

        // When
        openObsBehDropdown()

        // Then
        val obsBeh = dropdownItems().filter { it != NONE }
        assertEquals(listOf("3a", "3b", "3c", "3d", "3e", "3f", "3g"), obsBeh)
    }

    /**
     * 001-mvp/US-3/AS6: **Given** a Note under Competency 3, **when** the Assessor selects OBS BEH 3c, **then** "3c" is
     * shown in small italic under the Note and under Competency 3's title.
     *
     * The Assessor then taps away from the Note, so its editing controls, which also show the chosen OBS BEH, close.
     */
    @Test
    fun `001_mvp_US_3_AS6_selecting3cShowsItUnderTheNoteAndUnderTheCompetencyTitle`() {
        // Given
        seedSession(SESSION_TWO_CMS, twoCrewMembers = true, createdAt = MORNING)
        seedNote(SESSION_TWO_CMS, COMPETENCY_3, id = "note", text = "Late flaps", sortIndex = 0)
        harness.launch()
        openLastSession()
        focus(noteField(COMPETENCY_3, index = 0))

        // When
        openObsBehDropdown()
        dropdownItem("3c").performClick()
        tapOutsideTheNotes()

        // Then
        waitForCount(
            "3c under the Note",
            expected = 1
        ) { labelsUnderNote(COMPETENCY_3, index = 0).count { it == "3c" } }
        waitForCount("3c under the title", expected = 1) { labelsUnderTitle(COMPETENCY_3).count { it == "3c" } }
    }

    /**
     * 001-mvp/US-3/AS7: **Given** two Notes under Competency 3 both linked to 3c, **when** the capture screen shows
     * Competency 3, **then** 3c appears once under its title.
     */
    @Test
    fun `001_mvp_US_3_AS7_twoNotesLinkedTo3cShowItOnceUnderTheTitle`() {
        // Given
        seedSession(SESSION_TWO_CMS, twoCrewMembers = true, createdAt = MORNING)
        val obsBeh3c = observableBehaviourId("3c")
        seedNote(SESSION_TWO_CMS, COMPETENCY_3, id = "first", text = "Late flaps", sortIndex = 0, obsBehId = obsBeh3c)
        seedNote(SESSION_TWO_CMS, COMPETENCY_3, id = "second", text = "Late gear", sortIndex = 1, obsBehId = obsBeh3c)
        harness.launch()

        // When
        openLastSession()
        composeRule.waitUntilExactlyOneExists(hasText("Late gear"), TIMEOUT_MS)

        // Then
        waitForCount("3c under the title", expected = 1) { labelsUnderTitle(COMPETENCY_3).count { it == "3c" } }
    }

    /**
     * 001-mvp/US-3/AS8: **Given** the only Note linked to 3c, **when** the Assessor sets its OBS BEH to none, **then**
     * 3c disappears from under the Note and from under Competency 3's title.
     */
    @Test
    fun `001_mvp_US_3_AS8_settingTheOnlyNoteLinkedTo3cToNoneRemoves3cFromTheNoteAndTheTitle`() {
        // Given
        seedSession(SESSION_TWO_CMS, twoCrewMembers = true, createdAt = MORNING)
        val obsBeh3c = observableBehaviourId("3c")
        seedNote(SESSION_TWO_CMS, COMPETENCY_3, id = "note", text = "Late flaps", sortIndex = 0, obsBehId = obsBeh3c)
        harness.launch()
        openLastSession()
        waitForCount(
            "3c under the Note",
            expected = 1
        ) { labelsUnderNote(COMPETENCY_3, index = 0).count { it == "3c" } }
        waitForCount("3c under the title", expected = 1) { labelsUnderTitle(COMPETENCY_3).count { it == "3c" } }
        focus(noteField(COMPETENCY_3, index = 0))

        // When
        openObsBehDropdown()
        dropdownItem(NONE).performClick()
        tapOutsideTheNotes()

        // Then
        waitForCount(
            "3c under the Note",
            expected = 0
        ) { labelsUnderNote(COMPETENCY_3, index = 0).count { it == "3c" } }
        waitForCount("3c under the title", expected = 0) { labelsUnderTitle(COMPETENCY_3).count { it == "3c" } }
    }

    private fun openLastSession(route: String = ROUTE_TWO_CMS) {
        composeRule.waitUntilAtLeastOneExists(hasText(CONTINUE, ignoreCase = true), TIMEOUT_MS)
        composeRule.onNodeWithText(CONTINUE, ignoreCase = true).performClick()
        composeRule.waitUntilExactlyOneExists(hasText(route), TIMEOUT_MS)
        composeRule.waitUntil(TIMEOUT_MS) {
            composeRule.onAllNodesWithTag(noteFieldTag(COMPETENCY_3)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun focus(field: SemanticsNodeInteraction) {
        field.performScrollTo().performClick()
        field.assertIsFocused()
    }

    /** Taps the route at the top of the capture screen, away from every Note. */
    private fun tapOutsideTheNotes() {
        composeRule.onNode(hasText(ROUTE_TWO_CMS)).performClick()
    }

    private fun cmTagOption(label: String): SemanticsMatcher = hasText(label) and isSelectable()

    private fun obsBehDropdown(): SemanticsMatcher = hasText(OBS_BEH) and hasClickAction()

    private fun openObsBehDropdown() {
        composeRule.waitUntilExactlyOneExists(obsBehDropdown(), TIMEOUT_MS)
        composeRule.onNode(obsBehDropdown()).performScrollTo().performClick()
        composeRule.waitUntilAtLeastOneExists(dropdownItemMatcher(), TIMEOUT_MS)
    }

    private fun dropdownItemMatcher(): SemanticsMatcher = hasAnyAncestor(isPopup()) and hasClickAction()

    private fun dropdownItem(label: String): SemanticsNodeInteraction =
        composeRule.onNode(dropdownItemMatcher() and hasText(label))

    /** The labels of the open dropdown's items, top to bottom. */
    private fun dropdownItems(): List<String> =
        composeRule.onAllNodes(dropdownItemMatcher()).fetchSemanticsNodes().map { node ->
            node.texts().joinToString(" ")
        }

    private fun noteField(
        competencyId: Long,
        index: Int,
    ): SemanticsNodeInteraction = composeRule.onAllNodesWithTag(noteFieldTag(competencyId))[index]

    private fun noteFieldTag(competencyId: Long): String = "note-field-$competencyId"

    private fun noteFieldNodes(competencyId: Long): List<SemanticsNode> =
        composeRule.onAllNodes(hasTestTag(noteFieldTag(competencyId))).fetchSemanticsNodes()

    /** The words shown between the Note field at [index] and the next field of its Competency. */
    private fun labelsUnderNote(
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
                .onAllNodes(hasText("Competency $competencyId"))
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

    private fun seedSession(
        id: String,
        twoCrewMembers: Boolean,
        createdAt: Instant,
    ) {
        val session =
            Session(
                id = id,
                createdAt = createdAt,
                date = LocalDate.of(2026, 11, 1),
                cm1StaffNumber = StaffNumber.parse("79125"),
                cm2StaffNumber = if (twoCrewMembers) StaffNumber.parse("04512") else null,
                departure = checkNotNull(AirportCode.parse(if (twoCrewMembers) "DOH" else "SLL")),
                destination = checkNotNull(AirportCode.parse(if (twoCrewMembers) "SLL" else "DOH"))
            )
        runBlocking { harness.get<SessionRepository>().insert(session) }
    }

    /** Writes a Note's row straight into the database, tagged BOTH, with [obsBehId] as its OBS BEH. */
    private fun seedNote(
        sessionId: String,
        competencyId: Long,
        id: String,
        text: String,
        sortIndex: Long,
        obsBehId: Long? = null,
    ) {
        database().execSQL(
            "INSERT INTO note (id, sessionId, competencyId, text, cmTag, observableBehaviourId, sortIndex) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)",
            arrayOf<Any?>(id, sessionId, competencyId, text, "BOTH", obsBehId, sortIndex)
        )
    }

    private fun observableBehaviourId(name: String): Long =
        database().query("SELECT id FROM observable_behaviour WHERE name = ?", arrayOf(name)).use { cursor ->
            check(cursor.moveToFirst()) { "No OBS BEH named $name" }
            cursor.getLong(0)
        }

    private fun database() = harness.get<CheckPilotDatabase>().openHelper.writableDatabase

    private companion object {
        const val TIMEOUT_MS = 5000L
        const val SESSION_TWO_CMS = "session-two-cms"
        const val SESSION_ONE_CM = "session-one-cm"
        val MORNING: Instant = Instant.parse("2026-11-01T08:00:00Z")
        const val COMPETENCY_1 = 1L
        const val COMPETENCY_3 = 3L
        const val ROUTE_TWO_CMS = "DOH → SLL"
        const val ROUTE_ONE_CM = "SLL → DOH"
        const val CONTINUE = "Continue last Session"
        const val DONE = "Done"
        const val CM1 = "CM1"
        const val CM2 = "CM2"
        const val BOTH = "BOTH"
        const val OBS_BEH = "OBS BEH"
        const val NONE = "None"
    }
}
