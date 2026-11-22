package com.mebiad.checkpilot.acceptance

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.AnnotatedString
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mebiad.checkpilot.data.local.db.CheckPilotDatabase
import com.mebiad.checkpilot.testutils.AppHarness
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * US-1: Start a Session. Each test starts on the home screen, taps New Session, fills in the setup screen and taps
 * Start, then checks the capture screen, or the validation message and that no Session was created.
 *
 * The screen is tall so every Competency of the capture screen is laid out at once, whatever list it uses.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h3000dp")
@OptIn(ExperimentalTestApi::class)
class US1StartASessionTest {
    private val harness = AppHarness()
    private val composeRule = createEmptyComposeRule()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(harness).around(composeRule)

    private var launched = false

    /**
     * 001-mvp/US-1/AS1: **Given** the setup screen, **when** the Assessor enters CM1 Staff Number 79125, departure
     * DOH and destination SLL and taps Start, **then** the capture screen opens showing "DOH → SLL", today's date,
     * and every Competency of the Catalogue in Catalogue order.
     */
    @Test
    fun `001_mvp_US_1_AS1_startingOpensTheCaptureScreenWithRouteDateAndCompetencies`() {
        // Given
        harness.clock.setToday(LocalDate.of(2026, 11, 22))
        openSetupScreen()

        // When
        field("CM1").performTextInput("79125")
        field("From").performTextInput("DOH")
        field("To").performTextInput("SLL")
        composeRule.onNodeWithText("Start").performClick()

        // Then
        composeRule.waitUntilExactlyOneExists(hasText("DOH → SLL"), TIMEOUT_MS)
        composeRule.onNodeWithText("22 Nov 2026").assertExists()
        val catalogue = competencyNamesInCatalogueOrder()
        assertEquals(9, catalogue.size)
        val tops =
            catalogue.map { name ->
                composeRule
                    .onNodeWithText(name)
                    .fetchSemanticsNode()
                    .boundsInRoot.top
            }
        assertTrue(
            "Competencies must show in Catalogue order: $catalogue at $tops",
            tops.zipWithNext().all { (above, below) -> above < below }
        )
    }

    /**
     * 001-mvp/US-1/AS2: **Given** the setup screen with both Staff Number fields empty, **when** the Assessor taps
     * Start, **then** no Session is created and a message says at least one Staff Number is required.
     */
    @Test
    fun `001_mvp_US_1_AS2_noStaffNumberIsRefused`() {
        // Given
        openSetupScreen()
        field("From").performTextInput("DOH")
        field("To").performTextInput("SLL")

        // When
        composeRule.onNodeWithText("Start").performClick()

        // Then
        composeRule.waitUntilExactlyOneExists(hasText("At least one Staff Number is required."), TIMEOUT_MS)
        assertEquals(0, sessionRows().size)
    }

    /**
     * 001-mvp/US-1/AS3: **Given** the setup screen with only the CM2 Staff Number filled in, **when** the Assessor
     * taps Start, **then** the Session is created with one Crew Member, in the CM2 slot.
     */
    @Test
    fun `001_mvp_US_1_AS3_onlyCm2FillsTheCm2Slot`() {
        // Given
        openSetupScreen()
        field("CM2").performTextInput("04512")
        field("From").performTextInput("DOH")
        field("To").performTextInput("SLL")

        // When
        composeRule.onNodeWithText("Start").performClick()

        // Then
        composeRule.waitUntilExactlyOneExists(hasText("DOH → SLL"), TIMEOUT_MS)
        composeRule.waitUntil(TIMEOUT_MS) { sessionRows().isNotEmpty() }
        assertEquals(listOf(SessionRow(cm1StaffNumber = null, cm2StaffNumber = "04512")), sessionRows())
    }

    /**
     * 001-mvp/US-1/AS4: **Given** a Staff Number field, **when** the Assessor tries to type a letter, **then** the
     * field accepts only digits.
     */
    @Test
    fun `001_mvp_US_1_AS4_staffNumberFieldsAcceptOnlyDigits`() {
        // Given
        openSetupScreen()

        for (slot in listOf("CM1", "CM2")) {
            // When / Then: a letter typed between digits, in the CM1 field then the CM2 field
            field(slot).performTextInput("791")
            field(slot).performTextInput("a")
            field(slot).performTextInput("25")
            field(slot).assert(hasEditableText("79125"))
        }
    }

    /**
     * 001-mvp/US-1/AS5: **Given** a Staff Number of 3 digits or of 7 digits, **when** the Assessor taps Start,
     * **then** no Session is created and a message says a Staff Number has 4 to 6 digits.
     */
    @Test
    fun `001_mvp_US_1_AS5_staffNumberOf3Or7DigitsIsRefused`() {
        for (staffNumber in listOf("791", "7912534")) {
            // Given
            openSetupScreen()
            field("From").performTextInput("DOH")
            field("To").performTextInput("SLL")

            // When / Then: a Staff Number that is too short, then one that is too long
            field("CM1").performTextInput(staffNumber)
            composeRule.onNodeWithText("Start").performClick()
            composeRule.waitUntilExactlyOneExists(hasText("A Staff Number has 4 to 6 digits."), TIMEOUT_MS)
            assertEquals(0, sessionRows().size)
        }
    }

    /**
     * 001-mvp/US-1/AS6: **Given** a departure or destination field left empty, **when** the Assessor taps Start,
     * **then** no Session is created and a message says both airport codes are required.
     */
    @Test
    fun `001_mvp_US_1_AS6_anEmptyAirportCodeIsRefused`() {
        for ((departure, destination) in listOf("" to "SLL", "DOH" to "")) {
            // Given
            openSetupScreen()
            field("CM1").performTextInput("79125")

            // When / Then: the departure left empty, then the destination left empty
            if (departure.isNotEmpty()) field("From").performTextInput(departure)
            if (destination.isNotEmpty()) field("To").performTextInput(destination)
            composeRule.onNodeWithText("Start").performClick()
            composeRule.waitUntilExactlyOneExists(hasText("Both airport codes are required."), TIMEOUT_MS)
            assertEquals(0, sessionRows().size)
        }
    }

    /**
     * 001-mvp/US-1/AS7: **Given** an airport code that isn't 3 letters, such as "AM" or "AM1", **when** the Assessor
     * taps Start, **then** no Session is created and a message says an airport code is 3 letters.
     */
    @Test
    fun `001_mvp_US_1_AS7_anAirportCodeThatIsNot3LettersIsRefused`() {
        for (code in listOf("AM", "AM1")) {
            // Given
            openSetupScreen()
            field("CM1").performTextInput("79125")
            field("From").performTextInput("DOH")

            // When / Then: a destination of 2 letters, then of 2 letters and a digit
            field("To").performTextInput(code)
            composeRule.onNodeWithText("Start").performClick()
            composeRule.waitUntilExactlyOneExists(hasText("An airport code is 3 letters."), TIMEOUT_MS)
            assertEquals(0, sessionRows().size)
        }
    }

    /**
     * 001-mvp/US-1/AS8: **Given** the Assessor types "doh" as departure, **when** the Session starts, **then** the
     * route shows "DOH".
     */
    @Test
    fun `001_mvp_US_1_AS8_aLowercaseAirportCodeShowsInCapitals`() {
        // Given
        openSetupScreen()
        field("CM1").performTextInput("79125")
        field("From").performTextInput("doh")
        field("To").performTextInput("SLL")

        // When
        composeRule.onNodeWithText("Start").performClick()

        // Then
        composeRule.waitUntilExactlyOneExists(hasText("DOH → SLL"), TIMEOUT_MS)
    }

    /**
     * 001-mvp/US-1/AS9: **Given** the setup screen, **when** the Assessor picks 01 Nov 2026 in the date picker and
     * starts, **then** the capture screen shows 01 Nov 2026.
     */
    @Test
    fun `001_mvp_US_1_AS9_thePickedDateShowsOnTheCaptureScreen`() {
        // Given
        harness.clock.setToday(LocalDate.of(2026, 11, 21))
        openSetupScreen()
        field("CM1").performTextInput("79125")
        field("From").performTextInput("DOH")
        field("To").performTextInput("SLL")

        // When
        composeRule.onNodeWithText("21 Nov 2026").performClick()
        pickInDatePicker(LocalDate.of(2026, 11, 1))
        composeRule.onNodeWithText("Start").performClick()

        // Then
        composeRule.waitUntilExactlyOneExists(hasText("DOH → SLL"), TIMEOUT_MS)
        composeRule.onNodeWithText("01 Nov 2026").assertExists()
    }

    /** Opens the app on the home screen, or opens it again over the same data, and taps New Session. */
    private fun openSetupScreen() {
        if (launched) harness.restart() else harness.launch()
        launched = true
        composeRule.onNodeWithText("New Session", ignoreCase = true).performClick()
        composeRule.waitUntilAtLeastOneExists(hasText("Start"), TIMEOUT_MS)
    }

    /** The text field labelled [label] on screen: "CM1", "CM2", "From" or "To". */
    private fun field(label: String): SemanticsNodeInteraction =
        composeRule.onNode(hasSetTextAction() and (hasText(label) or hasContentDescription(label)))

    /** Taps [date]'s day in the open Material 3 date picker, which describes each day like "Sunday, November 1, 2026". */
    private fun pickInDatePicker(date: LocalDate) {
        val description = date.format(DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.US))
        composeRule.waitUntilAtLeastOneExists(hasText(description, substring = true), TIMEOUT_MS)
        composeRule.onNode(hasText(description, substring = true)).performClick()
        composeRule.onNodeWithText("OK").performClick()
    }

    private fun hasEditableText(text: String): SemanticsMatcher =
        SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString(text))

    private fun competencyNamesInCatalogueOrder(): List<String> =
        query("SELECT name FROM competency ORDER BY sortIndex") { getString(0) }

    private fun sessionRows(): List<SessionRow> =
        query("SELECT cm1StaffNumber, cm2StaffNumber FROM session") {
            SessionRow(
                cm1StaffNumber = if (isNull(0)) null else getString(0),
                cm2StaffNumber = if (isNull(1)) null else getString(1)
            )
        }

    private fun <T> query(
        sql: String,
        row: android.database.Cursor.() -> T,
    ): List<T> =
        harness
            .get<CheckPilotDatabase>()
            .openHelper.readableDatabase
            .query(sql)
            .use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.row()) } }

    private data class SessionRow(
        val cm1StaffNumber: String?,
        val cm2StaffNumber: String?,
    )

    private companion object {
        const val TIMEOUT_MS = 5000L
    }
}
