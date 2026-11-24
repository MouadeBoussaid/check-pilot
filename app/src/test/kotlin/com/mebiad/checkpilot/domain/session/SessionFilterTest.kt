package com.mebiad.checkpilot.domain.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class SessionFilterTest {
    @Test
    fun digitsSearchTheStartOfAStaffNumber() {
        // Given
        val text = " 0451 "

        // When
        val filter = SessionFilter.of(text = text, date = null)

        // Then
        assertEquals(SessionFilter(staffNumberPrefix = "0451"), filter)
    }

    @Test
    fun lettersSearchTheStartOfAnAirportCodeWhateverTheirCase() {
        // Given
        val text = "sL"

        // When
        val filter = SessionFilter.of(text = text, date = null)

        // Then
        assertEquals(SessionFilter(airportPrefix = "SL"), filter)
    }

    @Test
    fun noTextSearchesByTheDateAloneOrMatchesEverySession() {
        // Given
        val date = LocalDate.of(2026, 11, 8)

        // When
        val byDate = SessionFilter.of(text = "  ", date = date)
        val everything = SessionFilter.of(text = "", date = null)

        // Then
        assertEquals(SessionFilter(date = date), byDate)
        assertEquals(SessionFilter(), everything)
    }

    @Test
    fun theDateCombinesWithTheText() {
        // Given
        val date = LocalDate.of(2026, 11, 8)

        // When
        val filter = SessionFilter.of(text = "79125", date = date)

        // Then
        assertEquals(SessionFilter(staffNumberPrefix = "79125", date = date), filter)
    }

    @Test
    fun textThatIsNeitherDigitsNorLettersMatchesNothing() {
        // Given
        val texts = listOf("DOH1", "79 125", "%", "S_L", "DOH → SLL", "é")

        // When
        val filters = texts.map { SessionFilter.of(text = it, date = null) }

        // Then
        texts.zip(filters).forEach { (text, filter) -> assertNull("\"$text\" matches nothing", filter) }
    }
}
