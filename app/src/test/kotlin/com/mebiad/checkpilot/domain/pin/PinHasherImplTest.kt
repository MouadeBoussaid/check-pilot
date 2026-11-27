package com.mebiad.checkpilot.domain.pin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinHasherImplTest {
    private val hasher = PinHasherImpl()

    @Test
    fun theHashIsPbkdf2HmacSha256OfThePin() {
        // Given
        val pin = checkNotNull(Pin.parse("1234"))
        val salt = ByteArray(16) { it.toByte() }

        // When
        val hash = hasher.hash(pin = pin, salt = salt, iterations = 1000)

        // Then: worked out with Python's hashlib.pbkdf2_hmac("sha256", b"1234", bytes(range(16)), 1000, 32)
        assertEquals("2240ea2a22754a3c4610356ef8e4d7acf25af2ac3d2d11866de97a8b7812c52c", hash.toHex())
    }

    @Test
    fun eachNewSaltIs16RandomBytes() {
        // Given
        val count = 3

        // When
        val salts = List(count) { hasher.newSalt() }

        // Then
        assertTrue(salts.all { it.size == 16 })
        assertEquals(count, salts.map { it.toHex() }.toSet().size)
    }
}

private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
