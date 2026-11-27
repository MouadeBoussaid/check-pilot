package com.mebiad.checkpilot.testutils

import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performClick

/**
 * Drives the home screen's Open Session, the PIN keypad and the Session list in acceptance tests, the way the Assessor
 * does, and reads what the list shows.
 *
 * How it finds things, so restyling doesn't break it:
 * - The PIN screens are told apart by their titles: "Set a PIN" with no PIN set. Setting a PIN asks for it a second
 *   time under a text containing "again". The keypad's digits are the clickable nodes whose text is the digit; its ✓
 *   is the clickable node described "Confirm".
 * - The Session list is showing while the text "Sessions" stands alone outside pop-ups.
 * - Each Session's row is the band from its route, such as "DOH → SLL", down to the next route. In it: the date, each
 *   Staff Number, each Crew Member's Grade count as "<graded>/<total>" right of their Staff Number, the clickable
 *   "Capture" and "Debrief", and the ⋮ described "More options", whose menu holds the row's actions.
 *
 * Use it on a tall screen, so every row of the list is laid out at once, whatever list it uses.
 */
@OptIn(ExperimentalTestApi::class)
class SessionListDriver(
    private val composeRule: ComposeTestRule,
) {
    fun tapOpenSession() {
        val openSession = hasText(OPEN_SESSION, ignoreCase = true) and hasClickAction()
        composeRule.waitUntilAtLeastOneExists(openSession, TIMEOUT_MS)
        composeRule.onNode(openSession).performClick()
    }

    /** Types [pin] on the keypad and taps its ✓. */
    fun enterPin(pin: String) {
        pin.forEach { digit ->
            val key = hasText(digit.toString()) and hasClickAction() and !inPopUp()
            composeRule.waitUntilExactlyOneExists(key, TIMEOUT_MS)
            composeRule.onNode(key).performClick()
        }
        composeRule.onNode(hasContentDescription(CONFIRM) and hasClickAction() and !inPopUp()).performClick()
        composeRule.waitForIdle()
    }

    /** From the home screen with no PIN set: Open Session, the new [pin] typed twice, and the Session list. */
    fun unlockWithANewPin(pin: String = PIN) {
        tapOpenSession()
        composeRule.waitUntilExactlyOneExists(hasText(SET_A_PIN), TIMEOUT_MS)
        enterPin(pin)
        composeRule.waitUntilAtLeastOneExists(hasText(AGAIN, substring = true), TIMEOUT_MS)
        enterPin(pin)
        waitForList()
    }

    fun title(): SemanticsMatcher = hasText(SESSIONS) and !inPopUp()

    fun waitForList() {
        composeRule.waitUntilExactlyOneExists(title(), TIMEOUT_MS)
    }

    fun route(
        departure: String,
        destination: String,
    ): SemanticsMatcher = hasText("$departure → $destination") and !inPopUp()

    /** The routes of the listed Sessions, top to bottom. */
    fun routesShown(): List<String> = routeNodes().map { node -> node.texts().first { ROUTE.matches(it) } }

    /** The texts in the row with [route], each text node on its own. */
    fun rowTexts(route: SemanticsMatcher): List<String> = textNodesInRow(route).flatMap { it.texts() }

    /**
     * The Grade count shown for the Crew Member with [staffNumber] in the row with [route]: the first "<n>/<total>" in
     * the Staff Number's own text after it, or else in the nearest text right of it on the same line.
     */
    fun gradeCountRightOf(
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
            .minByOrNull { it.positionInRoot.x }
            ?.texts()
            ?.firstNotNullOfOrNull { GRADE_COUNT.find(it)?.value }
    }

    /** Taps the clickable [label], such as "Capture", in the row with [route]. */
    fun tapInRow(
        route: SemanticsMatcher,
        label: String,
    ) {
        tapInBand(route, hasText(label) and hasClickAction())
    }

    /** Opens the ⋮ menu of the row with [route] and taps its clickable item [label], such as "Delete". */
    fun tapInMenuOf(
        route: SemanticsMatcher,
        label: String,
    ) {
        tapInBand(route, hasContentDescription(MORE_OPTIONS) and hasClickAction())
        val item = inPopUp() and hasText(label) and hasClickAction()
        composeRule.waitUntilExactlyOneExists(item, TIMEOUT_MS)
        composeRule.onNode(item).performClick()
    }

    fun inPopUp(): SemanticsMatcher = hasAnyAncestor(isDialog() or isPopup())

    private fun routeNodes(): List<SemanticsNode> =
        textNodes()
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

    private fun textNodesInRow(route: SemanticsMatcher): List<SemanticsNode> {
        val band = bandOf(route)
        return textNodes().filter { it.top >= band.start && it.top < band.endInclusive }
    }

    private fun tapInBand(
        route: SemanticsMatcher,
        matcher: SemanticsMatcher,
    ) {
        waitForList()
        val band = bandOf(route)
        val candidates = composeRule.onAllNodes(matcher and !inPopUp()).fetchSemanticsNodes()
        val index = candidates.indexOfFirst { it.top >= band.start && it.top < band.endInclusive }
        check(index >= 0) { "Nothing matching $matcher in the row with $route" }
        composeRule.onAllNodes(matcher and !inPopUp())[index].performClick()
    }

    /** Every text node outside pop-ups, each on its own. */
    private fun textNodes(): List<SemanticsNode> =
        composeRule
            .onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text) and !inPopUp(), useUnmergedTree = true)
            .fetchSemanticsNodes()

    private fun SemanticsNode.texts(): List<String> =
        config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text }

    // Unclipped, unlike boundsInRoot, so rows scrolled out of view still have their real place.
    private val SemanticsNode.top: Float get() = positionInRoot.y

    companion object {
        const val TIMEOUT_MS = 5000L
        const val PIN = "1234"
        const val OPEN_SESSION = "Open Session"
        const val SET_A_PIN = "Set a PIN"
        const val AGAIN = "again"
        const val CONFIRM = "Confirm"
        const val SESSIONS = "Sessions"
        const val MORE_OPTIONS = "More options"
        val ROUTE = Regex("""[A-Z]{3} → [A-Z]{3}""")
        val GRADE_COUNT = Regex("""\d+/\d+""")
    }
}
