package com.mebiad.checkpilot.domain.pin

import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** PBKDF2-HMAC-SHA256 with a 16-byte random salt and a 32-byte hash. */
class PinHasherImpl(
    override val iterations: Int = DEFAULT_ITERATIONS,
) : PinHasher {
    private val random = SecureRandom()

    override fun newSalt(): ByteArray = ByteArray(SALT_BYTES).also(random::nextBytes)

    override fun hash(
        pin: Pin,
        salt: ByteArray,
        iterations: Int,
    ): ByteArray {
        val spec = PBEKeySpec(pin.digits.toCharArray(), salt, iterations, HASH_BYTES * Byte.SIZE_BITS)
        return try {
            SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private companion object {
        const val ALGORITHM = "PBKDF2WithHmacSHA256"
        const val DEFAULT_ITERATIONS = 120000
        const val SALT_BYTES = 16
        const val HASH_BYTES = 32
    }
}
