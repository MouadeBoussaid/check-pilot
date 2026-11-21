package com.mebiad.checkpilot.data.local.crypto

/** Stands in for the Android Keystore, which Robolectric doesn't have. Its output never equals its input. */
class PassphraseCipherFake : PassphraseCipher {
    override fun encrypt(plain: ByteArray): ByteArray = plain.map { (it.toInt() xor MASK).toByte() }.toByteArray()

    override fun decrypt(encrypted: ByteArray): ByteArray = encrypt(encrypted)

    private companion object {
        const val MASK = 0x5A
    }
}
