package com.mebiad.checkpilot.domain.pin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PinTest {
    @Test
    fun aPinHas4To6Digits() {
        // Given
        val accepted = listOf("0000", "12345", "012345")
        val refused = listOf("", "123", "1234567", "12a4", "12 34")

        // When
        val parsedAccepted = accepted.map { Pin.parse(it)?.digits }
        val parsedRefused = refused.map { Pin.parse(it) }

        // Then
        assertEquals(accepted, parsedAccepted)
        refused.zip(parsedRefused).forEach { (text, pin) -> assertNull("\"$text\" is refused", pin) }
    }
}
