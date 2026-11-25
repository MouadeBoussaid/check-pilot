package com.mebiad.checkpilot.domain.session

import java.time.LocalDate

/** What the Session list is narrowed to. Each condition left null matches every Session; the rest combine with AND. */
data class SessionFilter(
    val staffNumberPrefix: String? = null,
    val airportPrefix: String? = null,
    val date: LocalDate? = null,
) {
    companion object {
        /**
         * Reads the search [text]: digits match the start of either Staff Number, letters the start of either airport
         * code. Null when the text can match no Session, so it never reaches the database.
         */
        fun of(
            text: String,
            date: LocalDate?,
        ): SessionFilter? {
            val typed = text.trim()
            return when {
                typed.isEmpty() -> {
                    SessionFilter(date = date)
                }

                typed.all { it in '0'..'9' } -> {
                    SessionFilter(staffNumberPrefix = typed, date = date)
                }

                typed.all { it in 'A'..'Z' || it in 'a'..'z' } -> {
                    SessionFilter(airportPrefix = typed.uppercase(), date = date)
                }

                else -> {
                    null
                }
            }
        }
    }
}
