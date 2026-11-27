package com.mebiad.checkpilot.domain.session

/** An airport code: 3 letters, held in capitals whatever case they were typed in. */
@JvmInline
value class AirportCode private constructor(
    val letters: String,
) {
    companion object {
        fun parse(text: String): AirportCode? =
            text
                .uppercase()
                .takeIf { code -> code.length == 3 && code.all { it in 'A'..'Z' } }
                ?.let(::AirportCode)
    }
}
