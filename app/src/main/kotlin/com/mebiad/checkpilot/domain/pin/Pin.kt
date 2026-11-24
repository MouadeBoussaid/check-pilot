package com.mebiad.checkpilot.domain.pin

/** An App PIN: 4 to 6 digits. */
@JvmInline
value class Pin private constructor(
    val digits: String,
) {
    override fun toString(): String = "Pin(****)"

    companion object {
        fun parse(text: String): Pin? = text.takeIf { it.length in 4..6 && it.all { c -> c in '0'..'9' } }?.let(::Pin)
    }
}
