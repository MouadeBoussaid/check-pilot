package com.mebiad.checkpilot.acceptance

import androidx.compose.ui.semantics.SemanticsActions
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
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
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

/**
 * US-9: Edit the Catalogue. Each test seeds Sessions, Notes and Grades in their rows, opens the Catalogue editor from
 * the home screen's gear and Settings, adds, renames, moves and deletes Competencies and OBS BEH there, then checks the
 * editor, the capture screen, the OBS BEH dropdown, the Debrief and the Session list.
 *
 * How the tests find things, so restyling doesn't break them:
 * - The home screen's gear is the clickable node described "Settings". Settings offers a clickable "Edit the
 *   Catalogue". The editor is showing once that entry is gone and the text "Catalogue" stands alone, not clickable.
 * - An item's line is everything beside the text showing its name: the nodes whose height overlaps that text's. When
 *   several nodes on a line match, the shortest one counts, so a whole card never stands in for one of its rows.
 * - A collapsed Competency has on its line a clickable described "Expand"; an expanded one has one described
 *   "Collapse", and shows its OBS BEH below its name and above the next Competency's name, each named in one text.
 * - The "+ Competency" button is the clickable node whose text is "Competency". An expanded Competency's "+ OBS BEH"
 *   button is the clickable node whose text is "OBS BEH" below its name and above the next Competency's.
 * - An expanded Competency has on its line a clickable described "Rename". Adding and renaming show one text field,
 *   holding the current name when renaming, and a clickable "Save".
 * - An item that no Note or Grade uses has on its line an enabled clickable described "Delete"; if a dialog then asks,
 *   its clickable "Delete" confirms. An item in use shows "used in <n> Sessions" on its line, and no enabled "Delete".
 * - Every item's line offers the accessibility actions "Move up" and "Move down", the accessible way to drag it by its
 *   handle.
 * - The capture screen shows each Competency's name as its title, has Note fields tagged `note-field-<competencyId>`
 *   and its "Generate Debrief" button. A Note's OBS BEH dropdown is the clickable "OBS BEH", whose choices are the
 *   clickable items of the popup it opens, with "None" for no OBS BEH.
 * - The Debrief is showing while the text "Debrief" stands alone, not clickable, and lists each Competency's title.
 *   Its summary header shows the Grade count as "Grades 9/10" or "9 of 10 Grades set".
 * - The Session list is behind a PIN set through the keypad, and shows each Crew Member's Grade count as
 *   "<graded>/<total>" right of their Staff Number, in the row that starts with the Session's route.
 *
 * The screen is tall so every row of the editor, the capture screen and the Debrief is laid out at once.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h8000dp")
@OptIn(ExperimentalTestApi::class)
class US9EditTheCatalogueTest {
    private val harness = AppHarness()
    private val composeRule = createEmptyComposeRule()
    private val seeds = SessionSeeds(harness)

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(harness).around(composeRule)

    /**
     * 001-mvp/US-9/AS1: **Given** a fresh install, **when** the Assessor opens the Catalogue editor in Settings,
     * **then** it shows Competency 1 to 9, each with OBS BEH a to g.
     */
    @Test
    fun `001_mvp_US_9_AS1_aFreshInstallShowsCompetency1To9EachWithObsBehAToG`() {
        // Given
        harness.launch()

        // When
        openCatalogueEditor()

        // Then
        assertEquals((1..9).map { "Competency $it" }, placeholderCompetenciesInEditor())
        (1..9).forEach { number ->
            expand("Competency $number")
            val next = if (number < 9) "Competency ${number + 1}" else null
            assertEquals(
                "OBS BEH of Competency $number",
                "abcdefg".map { "$number$it" },
                obsBehInEditor(below = "Competency $number", above = next)
            )
        }
    }

    /**
     * 001-mvp/US-9/AS2: **Given** an existing Session with Notes under Competency 1, **when** the Assessor renames
     * Competency 1 to "Communication", **then** that Session and new Sessions show "Communication".
     */
    @Test
    fun `001_mvp_US_9_AS2_renamingCompetency1ShowsTheNewNameInTheSessionAndInNewSessions`() {
        // Given
        seeds.session(EXISTING, cm2 = null, createdAt = CREATED_AT)
        seeds.note(EXISTING, competencyId = 1, text = "Late readback", cmTag = "CM1")
        harness.launch()
        openCatalogueEditor()

        // When
        rename("Competency 1", to = COMMUNICATION)

        // Then
        backToHome()
        openLastSession()
        composeRule.waitUntilExactlyOneExists(hasText(COMMUNICATION) and !inPopUp(), TIMEOUT_MS)
        composeRule.onNode(hasText("Late readback")).assertExists()
        composeRule.onNode(hasText("Competency 1") and !inPopUp()).assertDoesNotExist()

        // When / Then: a new Session shows it too
        backToHome()
        startNewSession(departure = "MCT", destination = "DOH")
        composeRule.waitUntilExactlyOneExists(hasText(COMMUNICATION) and !inPopUp(), TIMEOUT_MS)
        composeRule.onNode(hasText("Competency 1") and !inPopUp()).assertDoesNotExist()
    }

    /**
     * 001-mvp/US-9/AS3: **Given** a Catalogue of 9 Competencies and an older Session with all 9 graded for CM1,
     * **when** the Assessor adds a Competency, **then** the older Session shows the new Competency without a Grade, and
     * its row in the Session list shows 9 of 10 Competencies graded.
     */
    @Test
    fun `001_mvp_US_9_AS3_anAddedCompetencyShowsUngradedInAnOlderSessionAndItsRowCounts9Of10`() {
        // Given
        seeds.session(OLDER, cm1 = "79125", cm2 = null, departure = "DOH", destination = "LHR", createdAt = CREATED_AT)
        (1L..9L).forEach { seeds.grade(OLDER, competencyId = it, crewMember = "CM1", value = "EFFECTIVE") }
        seeds.session(LAST, departure = "DOH", destination = "SLL", createdAt = CREATED_AT.plusSeconds(3600))
        harness.launch()
        openCatalogueEditor()

        // When
        addCompetency(WORKLOAD_MANAGEMENT)

        // Then
        backToHome()
        unlockSessionListWithANewPin()
        val olderRow = route("DOH", "LHR")
        composeRule.waitUntil(TIMEOUT_MS) { gradeCountRightOf(olderRow, "79125") == "9/10" }
        assertEquals("9/10", gradeCountRightOf(olderRow, "79125"))

        tapInRow(olderRow, DEBRIEF)
        waitForDebrief()
        composeRule.waitUntilExactlyOneExists(hasText(WORKLOAD_MANAGEMENT) and !inPopUp(), TIMEOUT_MS)
        composeRule.waitUntilExactlyOneExists(gradesSummary(graded = 9, total = 10), TIMEOUT_MS)
        val shownUnderTheNewCompetency = textsBelowTitle(WORKLOAD_MANAGEMENT)
        assertNull(
            "the new Competency has no Grade, but shows $shownUnderTheNewCompetency",
            GRADES.firstOrNull { grade -> shownUnderTheNewCompetency.any { it.trim() == grade } }
        )
    }

    /**
     * 001-mvp/US-9/AS4: **Given** Competency 3, **when** the Assessor adds an OBS BEH to it, **then** the new OBS BEH
     * is offered in the dropdown of Notes under Competency 3.
     *
     * The new OBS BEH comes last, as the spec's assumptions say an added item does.
     */
    @Test
    fun `001_mvp_US_9_AS4_anObsBehAddedToCompetency3IsOfferedInItsNotesDropdown`() {
        // Given
        seeds.session(EXISTING, createdAt = CREATED_AT)
        seeds.note(EXISTING, competencyId = 3, text = "Late flaps")
        harness.launch()
        openCatalogueEditor()

        // When
        addObsBeh(to = "Competency 3", next = "Competency 4", name = NEW_OBS_BEH)

        // Then
        backToHome()
        openLastSession()
        focus(noteField(competencyId = 3, index = 0))
        openObsBehDropdown()
        assertEquals("abcdefg".map { "3$it" } + NEW_OBS_BEH, dropdownItems().filter { it != NONE })
    }

    /**
     * 001-mvp/US-9/AS5: **Given** the Catalogue editor, **when** the Assessor moves Competency 9 to the top, **then**
     * the capture screen lists Competency 9 first, and Debrief ties are broken in the new order.
     *
     * Competencies 2 and 9 each have one Note for CM1 with one OBS BEH, a full tie that Catalogue order breaks.
     */
    @Test
    fun `001_mvp_US_9_AS5_movingCompetency9ToTheTopListsItFirstAndBreaksDebriefTiesInTheNewOrder`() {
        // Given
        seeds.session(EXISTING, cm2 = null, createdAt = CREATED_AT)
        seeds.note(EXISTING, competencyId = 2, text = "Late readback", cmTag = "CM1", observableBehaviour = "2a")
        seeds.note(EXISTING, competencyId = 9, text = "Late gear", cmTag = "CM1", observableBehaviour = "9a")
        harness.launch()
        openCatalogueEditor()

        // When
        moveUp("Competency 9", times = 8)

        // Then
        backToHome()
        openLastSession()
        waitForOrder("the capture screen lists Competency 9 first") { it == listOf(9L, 1, 2, 3, 4, 5, 6, 7, 8) }

        tapGenerate()
        waitForDebrief()
        waitForOrder("the Debrief breaks the tie with Competency 9 first") { it == listOf(9L, 2, 1, 3, 4, 5, 6, 7, 8) }
    }

    /**
     * 001-mvp/US-9/AS6: **Given** Competency 3's OBS BEH, **when** the Assessor moves 3g to the top, **then** the
     * dropdown of Notes under Competency 3 lists 3g first.
     */
    @Test
    fun `001_mvp_US_9_AS6_moving3gToTheTopListsItFirstInCompetency3sDropdown`() {
        // Given
        seeds.session(EXISTING, createdAt = CREATED_AT)
        seeds.note(EXISTING, competencyId = 3, text = "Late flaps")
        harness.launch()
        openCatalogueEditor()
        expand("Competency 3")

        // When
        moveUp("3g", times = 6)

        // Then
        backToHome()
        openLastSession()
        focus(noteField(competencyId = 3, index = 0))
        openObsBehDropdown()
        assertEquals(listOf("3g", "3a", "3b", "3c", "3d", "3e", "3f"), dropdownItems().filter { it != NONE })
    }

    /**
     * 001-mvp/US-9/AS7: **Given** a Competency or OBS BEH that no Note or Grade uses, **when** the Assessor deletes it,
     * **then** it is gone from the Catalogue editor and the capture screen.
     *
     * The Session uses Competency 3 and its OBS BEH 3a, but neither Competency 9 nor OBS BEH 3e.
     */
    @Test
    fun `001_mvp_US_9_AS7_deletingAnUnusedCompetencyAndObsBehRemovesThemFromTheEditorAndTheCaptureScreen`() {
        // Given
        seeds.session(EXISTING, createdAt = CREATED_AT)
        seeds.note(EXISTING, competencyId = 3, text = "Late flaps", observableBehaviour = "3a")
        seeds.grade(EXISTING, competencyId = 1, crewMember = "CM1", value = "ADEQUATE")
        harness.launch()
        openCatalogueEditor()
        expand("Competency 3")
        expand("Competency 9")

        // When
        delete("3e")
        delete("Competency 9")

        // Then
        assertEquals((1..8).map { "Competency $it" }, placeholderCompetenciesInEditor())
        assertEquals(
            listOf("3a", "3b", "3c", "3d", "3f", "3g"),
            obsBehInEditor(below = "Competency 3", above = "Competency 4")
        )

        backToHome()
        openLastSession()
        composeRule.waitUntil(TIMEOUT_MS) { competencyOrder().size == 8 }
        assertEquals(listOf(1L, 2, 3, 4, 5, 6, 7, 8), competencyOrder())
        focus(noteField(competencyId = 3, index = 0))
        openObsBehDropdown()
        assertEquals(listOf("3a", "3b", "3c", "3d", "3f", "3g"), dropdownItems().filter { it != NONE })
    }

    /**
     * 001-mvp/US-9/AS8: **Given** an OBS BEH that Notes in 4 Sessions use, **when** the Assessor looks at its delete
     * option, **then** it is greyed out with "used in 4 Sessions".
     *
     * One of the 4 Sessions has two Notes linked to 2c, and a fifth Session links only 2d. 2e, which nothing uses, can
     * still be deleted.
     */
    @Test
    fun `001_mvp_US_9_AS8_anObsBehUsedIn4SessionsCannotBeDeletedAndSaysUsedIn4Sessions`() {
        // Given
        (1..4).forEach { number ->
            val session = "session-$number"
            seeds.session(session, createdAt = CREATED_AT.plusSeconds(number * 3600L))
            seeds.note(session, competencyId = 2, text = "Readback $number", observableBehaviour = "2c")
        }
        seeds.note("session-1", competencyId = 2, text = "Readback again", observableBehaviour = "2c")
        seeds.session("session-5", createdAt = CREATED_AT.plusSeconds(5 * 3600L))
        seeds.note("session-5", competencyId = 2, text = "Unclear", observableBehaviour = "2d")
        harness.launch()
        openCatalogueEditor()

        // When
        expand("Competency 2")

        // Then
        assertTrue(
            "2c's line says it's used in 4 Sessions: ${textsOnLine("2c")}",
            textsOnLine("2c").any { USED_IN_4_SESSIONS.containsMatchIn(it) }
        )
        assertNull("2c offers no enabled Delete", shortestOnLine("2c", enabledDelete()))
        assertTrue(
            "2d's line says it's used in 1 Session: ${textsOnLine("2d")}",
            textsOnLine("2d").any { USED_IN_1_SESSION.containsMatchIn(it) }
        )
        assertTrue("2e, used nowhere, offers an enabled Delete", shortestOnLine("2e", enabledDelete()) != null)
    }

    /**
     * 001-mvp/US-9/AS9: **Given** Settings, the capture screen and a Debrief, **when** the Assessor looks for the
     * Catalogue editor, **then** only Settings offers a way to open it.
     *
     * Neither the capture screen nor the Debrief shows anything naming the Catalogue or Settings.
     */
    @Test
    fun `001_mvp_US_9_AS9_onlySettingsOffersAWayToOpenTheCatalogueEditor`() {
        // Given
        seeds.session(EXISTING, cm2 = null, createdAt = CREATED_AT)
        harness.launch()

        // When / Then: the capture screen offers no way
        openLastSession()
        assertNothingNamesTheEditor("the capture screen")

        // When / Then: the Debrief offers no way
        tapGenerate()
        waitForDebrief()
        assertNothingNamesTheEditor("the Debrief")

        // When / Then: Settings opens it
        backToHome()
        openCatalogueEditor()
        composeRule.waitUntilExactlyOneExists(hasText("Competency 1") and !inPopUp(), TIMEOUT_MS)
    }

    /**
     * 001-mvp/US-9/AS10: **Given** a PIN is set, **when** the Assessor opens the Catalogue editor from Settings,
     * **then** no PIN is asked.
     */
    @Test
    fun `001_mvp_US_9_AS10_withAPinSetTheEditorOpensWithoutAskingForIt`() {
        // Given
        givenPinIsSet()

        // When
        openCatalogueEditor()

        // Then
        composeRule.waitUntilExactlyOneExists(hasText("Competency 1") and !inPopUp(), TIMEOUT_MS)
        composeRule.onNode(hasText(ENTER_PIN)).assertDoesNotExist()
        composeRule.onNode(confirmKey()).assertDoesNotExist()
    }

    /**
     * 001-mvp/US-9/AS11: **Given** an edited Catalogue, **when** the app is updated to a new version, **then** the
     * Catalogue keeps its edits.
     *
     * The update restarts the app over the same database file. Exactly 10 Competencies show, without "Competency 1", so
     * the placeholder Catalogue wasn't seeded again.
     */
    @Test
    fun `001_mvp_US_9_AS11_anAppUpdateKeepsTheEditedCatalogue`() {
        // Given
        harness.launch()
        openCatalogueEditor()
        rename("Competency 1", to = COMMUNICATION)
        addCompetency(WORKLOAD_MANAGEMENT)

        // When
        harness.restart()

        // Then
        openCatalogueEditor()
        composeRule.waitUntilExactlyOneExists(hasText(COMMUNICATION) and !inPopUp(), TIMEOUT_MS)
        composeRule.waitUntilExactlyOneExists(hasText(WORKLOAD_MANAGEMENT) and !inPopUp(), TIMEOUT_MS)
        composeRule.onNode(hasText("Competency 1") and !inPopUp()).assertDoesNotExist()
        assertEquals((2..9).map { "Competency $it" }, placeholderCompetenciesInEditor())
        assertEquals("Competencies in the editor", 10, competenciesInEditor())
    }

    // Home and Settings

    /** Presses back until the home screen shows, leaving a Debrief that asks whether to leave with Grades unset. */
    private fun backToHome() {
        val home = hasText(OPEN_SESSION, ignoreCase = true) and hasClickAction()
        val leave = inPopUp() and hasText(LEAVE) and hasClickAction()
        repeat(MAX_BACKS) {
            composeRule.waitForIdle()
            if (exists(home)) return
            if (exists(leave)) composeRule.onNode(leave).performClick() else harness.pressBack()
        }
        composeRule.waitUntilExactlyOneExists(home, TIMEOUT_MS)
    }

    private fun editCatalogueEntry(): SemanticsMatcher = hasText(EDIT_THE_CATALOGUE) and hasClickAction() and !inPopUp()

    /** Opens Settings from the home screen's gear, then the Catalogue editor. */
    private fun openCatalogueEditor() {
        val gear = hasContentDescription(SETTINGS) and hasClickAction() and !inPopUp()
        composeRule.waitUntilExactlyOneExists(gear, TIMEOUT_MS)
        composeRule.onNode(gear).performClick()
        composeRule.waitUntilExactlyOneExists(editCatalogueEntry(), TIMEOUT_MS)
        composeRule.onNode(editCatalogueEntry()).performClick()
        composeRule.waitUntilDoesNotExist(editCatalogueEntry(), TIMEOUT_MS)
        composeRule.waitUntilExactlyOneExists(hasText(CATALOGUE) and !hasClickAction() and !inPopUp(), TIMEOUT_MS)
    }

    /** Opens the setup screen from home and starts a Session with one Crew Member on the route given. */
    private fun startNewSession(
        departure: String,
        destination: String,
    ) {
        val newSession = hasText(NEW_SESSION, ignoreCase = true) and hasClickAction()
        composeRule.waitUntilExactlyOneExists(newSession, TIMEOUT_MS)
        composeRule.onNode(newSession).performClick()
        composeRule.waitUntilAtLeastOneExists(hasText(START), TIMEOUT_MS)
        setupField("CM1").performTextInput("79125")
        setupField("From").performTextInput(departure)
        setupField("To").performTextInput(destination)
        composeRule.onNode(hasText(START) and hasClickAction()).performClick()
        composeRule.waitUntilExactlyOneExists(hasText("$departure → $destination") and !inPopUp(), TIMEOUT_MS)
    }

    /** The setup screen's text field labelled [label]: "CM1", "CM2", "From" or "To". */
    private fun setupField(label: String): SemanticsNodeInteraction =
        composeRule.onNode(hasSetTextAction() and (hasText(label) or hasContentDescription(label)))

    // Catalogue editor

    /** The editor's Competencies still named like the placeholders, "Competency <n>", top to bottom. */
    private fun placeholderCompetenciesInEditor(): List<String> =
        textNodes()
            .filter { node -> node.texts().any { PLACEHOLDER_COMPETENCY.matches(it) } }
            .sortedBy { it.top }
            .map { node -> node.texts().first { PLACEHOLDER_COMPETENCY.matches(it) } }

    /** How many Competencies the editor shows: one "Expand" or "Collapse" each. */
    private fun competenciesInEditor(): Int =
        composeRule
            .onAllNodes((hasContentDescription(EXPAND) or hasContentDescription(COLLAPSE)) and !inPopUp())
            .fetchSemanticsNodes()
            .size

    /** The OBS BEH names, like "3c", shown below the Competency [below] and above the Competency [above]. */
    private fun obsBehInEditor(
        below: String,
        above: String?,
    ): List<String> {
        val top = nameNode(below).top
        val bottom = above?.let { nameNode(it).top } ?: Float.MAX_VALUE
        return textNodes()
            .filter { it.top > top && it.top < bottom }
            .sortedBy { it.top }
            .flatMap { node -> node.texts().filter { PLACEHOLDER_OBS_BEH.matches(it) } }
            .distinct()
    }

    private fun expand(competency: String) {
        shortestOnLine(competency, hasContentDescription(EXPAND) and hasClickAction())?.let { tap(it) }
        composeRule.waitUntil(TIMEOUT_MS) { shortestOnLine(competency, hasContentDescription(COLLAPSE)) != null }
    }

    private fun rename(
        competency: String,
        to: String,
    ) {
        expand(competency)
        tap(checkNotNull(shortestOnLine(competency, hasContentDescription(RENAME) and hasClickAction())))
        enterName(to)
    }

    private fun addCompetency(name: String) {
        val add = hasText(COMPETENCY) and hasClickAction() and !inPopUp()
        composeRule.waitUntilExactlyOneExists(add, TIMEOUT_MS)
        composeRule.onNode(add).performScrollTo().performClick()
        enterName(name)
    }

    /** Adds the OBS BEH [name] with the "+ OBS BEH" of the Competency [to], which the Competency [next] follows. */
    private fun addObsBeh(
        to: String,
        next: String,
        name: String,
    ) {
        expand(to)
        val top = nameNode(to).top
        val bottom = nameNode(next).top
        val add = hasText(OBS_BEH) and hasClickAction() and !inPopUp()
        val index =
            composeRule
                .onAllNodes(add)
                .fetchSemanticsNodes()
                .indexOfFirst { it.top > top && it.top < bottom }
        check(index >= 0) { "No + OBS BEH between $to and $next" }
        composeRule.onAllNodes(add)[index].performScrollTo().performClick()
        enterName(name)
    }

    /** Types [name] in the text field that adding or renaming shows, replacing what it holds, and taps Save. */
    private fun enterName(name: String) {
        val field = hasSetTextAction()
        composeRule.waitUntilExactlyOneExists(field, TIMEOUT_MS)
        composeRule.onNode(field).performTextReplacement(name)
        composeRule.onNode(hasText(SAVE) and hasClickAction()).performClick()
        composeRule.waitUntilDoesNotExist(field, TIMEOUT_MS)
        composeRule.waitUntilAtLeastOneExists(hasText(name) and !inPopUp(), TIMEOUT_MS)
    }

    private fun enabledDelete(): SemanticsMatcher = hasContentDescription(DELETE) and hasClickAction() and isEnabled()

    /** Deletes the unused item [name] with the Delete on its line, confirming if the app asks. */
    private fun delete(name: String) {
        tap(checkNotNull(shortestOnLine(name, enabledDelete())) { "$name offers no enabled Delete" })
        composeRule.waitForIdle()
        val confirm = inPopUp() and hasText(DELETE) and hasClickAction()
        if (exists(confirm)) composeRule.onNode(confirm).performClick()
        composeRule.waitUntilDoesNotExist(hasText(name) and !inPopUp(), TIMEOUT_MS)
    }

    /** Moves the item [name] up [times] places with its "Move up" action, each time waiting until it has moved. */
    private fun moveUp(
        name: String,
        times: Int,
    ) {
        repeat(times) { step ->
            val before = nameNode(name).top
            val handle = checkNotNull(shortestOnLine(name, hasCustomAction(MOVE_UP))) { "$name can't be moved up" }
            val moveUp = handle.config[SemanticsActions.CustomActions].first { it.label == MOVE_UP }
            composeRule.runOnUiThread { moveUp.action() }
            val moved = runCatching { composeRule.waitUntil(TIMEOUT_MS) { nameNode(name).top < before } }
            assertTrue("$name moved up on move ${step + 1} of $times", moved.isSuccess)
        }
    }

    private fun hasCustomAction(label: String): SemanticsMatcher =
        SemanticsMatcher("offers the action \"$label\"") { node ->
            node.config
                .getOrNull(SemanticsActions.CustomActions)
                .orEmpty()
                .any { it.label == label }
        }

    /** The text node showing exactly [name] outside pop-ups, the topmost if there are several. */
    private fun nameNode(name: String): SemanticsNode {
        composeRule.waitUntilAtLeastOneExists(hasText(name) and !inPopUp(), TIMEOUT_MS)
        return composeRule
            .onAllNodes(hasText(name) and !inPopUp(), useUnmergedTree = true)
            .fetchSemanticsNodes()
            .minBy { it.top }
    }

    /** The shortest node matching [matcher], outside pop-ups, on the line of the item [name], or null. */
    private fun shortestOnLine(
        name: String,
        matcher: SemanticsMatcher,
    ): SemanticsNode? {
        val line = nameNode(name)
        return composeRule
            .onAllNodes(matcher and !inPopUp())
            .fetchSemanticsNodes()
            .filter { it.overlapsLineOf(line) }
            .minByOrNull { it.size.height }
    }

    /** The texts beside the item [name], its own included. */
    private fun textsOnLine(name: String): List<String> {
        val line = nameNode(name)
        return textNodes().filter { it.overlapsLineOf(line) }.flatMap { it.texts() }
    }

    private fun SemanticsNode.overlapsLineOf(line: SemanticsNode): Boolean =
        top < line.top + line.size.height && top + size.height > line.top

    /** Taps [node], found in the merged tree, through its own interaction. */
    private fun tap(node: SemanticsNode) {
        val interaction = composeRule.onNode(SemanticsMatcher("node ${node.id}") { it.id == node.id })
        interaction.performScrollTo().performClick()
        composeRule.waitForIdle()
    }

    // Capture screen

    private fun generateButton(): SemanticsMatcher =
        hasText(GENERATE, substring = true, ignoreCase = true) and hasClickAction() and !inPopUp()

    private fun openLastSession() {
        val continueLast = hasText(CONTINUE, ignoreCase = true) and hasClickAction()
        composeRule.waitUntilAtLeastOneExists(continueLast, TIMEOUT_MS)
        composeRule.onNode(continueLast).performClick()
        composeRule.waitUntilExactlyOneExists(generateButton(), TIMEOUT_MS)
    }

    private fun tapGenerate() {
        composeRule.onNode(generateButton()).performScrollTo().performClick()
    }

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

    private fun obsBehDropdown(): SemanticsMatcher = hasText(OBS_BEH) and hasClickAction() and !inPopUp()

    private fun openObsBehDropdown() {
        composeRule.waitUntilExactlyOneExists(obsBehDropdown(), TIMEOUT_MS)
        composeRule.onNode(obsBehDropdown()).performScrollTo().performClick()
        composeRule.waitUntilAtLeastOneExists(dropdownItem(), TIMEOUT_MS)
    }

    private fun dropdownItem(): SemanticsMatcher = hasAnyAncestor(isPopup()) and hasClickAction()

    /** The labels of the open dropdown's items, top to bottom. */
    private fun dropdownItems(): List<String> =
        composeRule
            .onAllNodes(dropdownItem())
            .fetchSemanticsNodes()
            .sortedBy { it.top }
            .map { node -> node.texts().joinToString(" ") }

    /** The placeholder Competencies' ids, top to bottom, as the capture screen or the Debrief lists their titles. */
    private fun competencyOrder(): List<Long> =
        textNodes()
            .mapNotNull { node ->
                node
                    .texts()
                    .firstNotNullOfOrNull { PLACEHOLDER_COMPETENCY.matchEntire(it) }
                    ?.let { it.groupValues[1].toLong() to node.top }
            }.sortedBy { it.second }
            .map { it.first }

    /** Waits until the listed order satisfies [condition], and fails with the order shown. */
    private fun waitForOrder(
        what: String,
        condition: (List<Long>) -> Boolean,
    ) {
        val reached = runCatching { composeRule.waitUntil(TIMEOUT_MS) { condition(competencyOrder()) } }
        val order = competencyOrder()
        assertTrue("$what, but it lists $order", reached.isSuccess && condition(order))
    }

    /** Fails if anything on screen names the Catalogue or Settings, in its text or its description. */
    private fun assertNothingNamesTheEditor(screen: String) {
        composeRule.waitForIdle()
        val naming =
            composeRule
                .onAllNodes(
                    hasText(CATALOGUE, substring = true, ignoreCase = true) or
                        hasContentDescription(CATALOGUE, substring = true, ignoreCase = true) or
                        hasText(SETTINGS, substring = true, ignoreCase = true) or
                        hasContentDescription(SETTINGS, substring = true, ignoreCase = true),
                    useUnmergedTree = true
                ).fetchSemanticsNodes()
        assertTrue(
            "$screen offers no way to the Catalogue editor, but shows ${naming.map { it.config }}",
            naming.isEmpty()
        )
    }

    // Debrief

    private fun debriefTitle(): SemanticsMatcher = hasText(DEBRIEF) and !hasClickAction() and !inPopUp()

    private fun waitForDebrief() {
        composeRule.waitUntilExactlyOneExists(debriefTitle(), TIMEOUT_MS)
        composeRule.waitUntilDoesNotExist(generateButton(), TIMEOUT_MS)
    }

    /** Matches the Grade count in the summary header, as the spec writes it or as the design draws it. */
    private fun gradesSummary(
        graded: Int,
        total: Int,
    ): SemanticsMatcher {
        val pattern = Regex("""\s*(Grades $graded/$total|$graded of $total Grades set)\s*""")
        return SemanticsMatcher("shows $graded of $total Grades") { node ->
            node.texts().any { pattern.matches(it) }
        } and !inPopUp()
    }

    /** The texts below the Debrief's title [title], down to the next placeholder Competency's title. */
    private fun textsBelowTitle(title: String): List<String> {
        val top = nameNode(title).top
        val nodes = textNodes()
        val bottom =
            nodes
                .filter { node -> node.top > top && node.texts().any { PLACEHOLDER_COMPETENCY.matches(it) } }
                .minOfOrNull { it.top } ?: Float.MAX_VALUE
        return nodes.filter { it.top > top && it.top < bottom }.flatMap { it.texts() }
    }

    // PIN and Session list

    private fun tapOpenSession() {
        val openSession = hasText(OPEN_SESSION, ignoreCase = true) and hasClickAction()
        composeRule.waitUntilAtLeastOneExists(openSession, TIMEOUT_MS)
        composeRule.onNode(openSession).performClick()
    }

    private fun digitKey(digit: Char): SemanticsMatcher = hasText(digit.toString()) and hasClickAction() and !inPopUp()

    private fun confirmKey(): SemanticsMatcher = hasContentDescription(CONFIRM) and hasClickAction() and !inPopUp()

    /** Types [pin] on the keypad and taps its ✓. */
    private fun enterPin(pin: String) {
        pin.forEach { digit ->
            composeRule.waitUntilExactlyOneExists(digitKey(digit), TIMEOUT_MS)
            composeRule.onNode(digitKey(digit)).performClick()
        }
        composeRule.onNode(confirmKey()).performClick()
        composeRule.waitForIdle()
    }

    private fun unlockSessionListWithANewPin() {
        tapOpenSession()
        composeRule.waitUntilExactlyOneExists(hasText(SET_A_PIN), TIMEOUT_MS)
        enterPin(PIN)
        composeRule.waitUntilAtLeastOneExists(hasText(AGAIN, substring = true), TIMEOUT_MS)
        enterPin(PIN)
        composeRule.waitUntilExactlyOneExists(hasText(SESSIONS) and !inPopUp(), TIMEOUT_MS)
    }

    /** Sets a PIN through the PIN screens, then restarts the app on its home screen. */
    private fun givenPinIsSet() {
        harness.launch()
        unlockSessionListWithANewPin()
        harness.restart()
    }

    private fun route(
        departure: String,
        destination: String,
    ): SemanticsMatcher = hasText("$departure → $destination") and !inPopUp()

    /** The vertical band of the Session list that belongs to the row with [route]: from its route to the next route. */
    private fun bandOf(route: SemanticsMatcher): ClosedFloatingPointRange<Float> {
        composeRule.waitUntilExactlyOneExists(route, TIMEOUT_MS)
        val routes = textNodes().filter { node -> node.texts().any { ROUTE.matches(it) } }.sortedBy { it.top }
        val index = routes.indexOfFirst { route.matches(it) }
        check(index >= 0) { "No row with $route" }
        return routes[index].top..(routes.getOrNull(index + 1)?.top ?: Float.MAX_VALUE)
    }

    /**
     * The Grade count shown for the Crew Member with [staffNumber] in the row with [route]: the first "<n>/<total>" in
     * the Staff Number's own text after it, or else in the nearest text right of it on the same line.
     */
    private fun gradeCountRightOf(
        route: SemanticsMatcher,
        staffNumber: String,
    ): String? {
        val band = bandOf(route)
        val nodes = textNodes().filter { it.top >= band.start && it.top < band.endInclusive }
        val own = nodes.firstOrNull { node -> node.texts().any { staffNumber in it } } ?: return null
        own
            .texts()
            .firstNotNullOfOrNull { text ->
                GRADE_COUNT.find(text.substringAfter(staffNumber, missingDelimiterValue = ""))
            }?.let { return it.value }
        val right = own.positionInRoot.x + own.size.width
        return nodes
            .filter { it !== own && it.positionInRoot.x >= right && it.overlapsLineOf(own) }
            .minByOrNull { it.positionInRoot.x }
            ?.texts()
            ?.firstNotNullOfOrNull { GRADE_COUNT.find(it)?.value }
    }

    /** Taps the clickable [label], such as "Debrief", in the row with [route]. */
    private fun tapInRow(
        route: SemanticsMatcher,
        label: String,
    ) {
        val band = bandOf(route)
        val matcher = hasText(label) and hasClickAction() and !inPopUp()
        val index = composeRule.onAllNodes(matcher).fetchSemanticsNodes().indexOfFirst { it.top in band }
        check(index >= 0) { "No clickable $label in the row with $route" }
        composeRule.onAllNodes(matcher)[index].performClick()
    }

    // Shared

    private fun inPopUp(): SemanticsMatcher = hasAnyAncestor(isDialog() or isPopup())

    private fun exists(matcher: SemanticsMatcher): Boolean =
        composeRule.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty()

    /** Every text node outside pop-ups, each on its own. */
    private fun textNodes(): List<SemanticsNode> =
        composeRule
            .onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text) and !inPopUp(), useUnmergedTree = true)
            .fetchSemanticsNodes()

    private fun SemanticsNode.texts(): List<String> =
        config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text }

    // Unclipped, unlike boundsInRoot, so rows scrolled out of view still have their real place.
    private val SemanticsNode.top: Float get() = positionInRoot.y

    private companion object {
        const val TIMEOUT_MS = 5000L
        const val MAX_BACKS = 5
        const val PIN = "2468"
        const val EXISTING = "existing"
        const val OLDER = "older"
        const val LAST = "last"
        const val COMMUNICATION = "Communication"
        const val WORKLOAD_MANAGEMENT = "Workload Management"
        const val NEW_OBS_BEH = "Monitors fuel state"
        const val OPEN_SESSION = "Open Session"
        const val NEW_SESSION = "New Session"
        const val START = "Start"
        const val CONTINUE = "Continue last Session"
        const val SETTINGS = "Settings"
        const val EDIT_THE_CATALOGUE = "Edit the Catalogue"
        const val CATALOGUE = "Catalogue"
        const val COMPETENCY = "Competency"
        const val OBS_BEH = "OBS BEH"
        const val EXPAND = "Expand"
        const val COLLAPSE = "Collapse"
        const val RENAME = "Rename"
        const val SAVE = "Save"
        const val DELETE = "Delete"
        const val MOVE_UP = "Move up"
        const val NONE = "None"
        const val GENERATE = "Generate debrief"
        const val DEBRIEF = "Debrief"
        const val LEAVE = "Leave"
        const val SET_A_PIN = "Set a PIN"
        const val ENTER_PIN = "Enter PIN"
        const val AGAIN = "again"
        const val CONFIRM = "Confirm"
        const val SESSIONS = "Sessions"
        val CREATED_AT: Instant = Instant.parse("2026-11-01T08:00:00Z")
        val PLACEHOLDER_COMPETENCY = Regex("""Competency (\d+)""")
        val PLACEHOLDER_OBS_BEH = Regex("""\d+[a-z]""")
        val USED_IN_4_SESSIONS = Regex("""used in 4 Sessions""", RegexOption.IGNORE_CASE)
        val USED_IN_1_SESSION = Regex("""used in 1 Session\b""", RegexOption.IGNORE_CASE)
        val ROUTE = Regex("""[A-Z]{3} → [A-Z]{3}""")
        val GRADE_COUNT = Regex("""\d+/\d+""")
        val GRADES =
            listOf(
                "Not Observed",
                "Exemplary",
                "Effective",
                "Adequate",
                "Minimum Acceptable Level",
                "Not Yet Competent"
            )
    }
}
