package com.mebiad.checkpilot.domain.session

/** A Crew Member's Staff Number: 4 to 6 digits, kept exactly as typed, leading zeros included. */
@JvmInline
value class StaffNumber private constructor(
    val digits: String,
) {
    companion object {
        fun parse(text: String): StaffNumber? =
            text.takeIf { it.length in 4..6 && it.all(Char::isDigit) }?.let(::StaffNumber)
    }
}
