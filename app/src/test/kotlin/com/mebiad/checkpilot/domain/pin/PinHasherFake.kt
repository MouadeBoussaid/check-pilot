package com.mebiad.checkpilot.domain.pin

/** A fast, readable stand-in: the hash is the salt, the iteration count and the PIN's digits as bytes. */
class PinHasherFake(
    override val iterations: Int = 1,
) : PinHasher {
    var salt: ByteArray = byteArrayOf(1, 2, 3)

    override fun newSalt(): ByteArray = salt.copyOf()

    override fun hash(
        pin: Pin,
        salt: ByteArray,
        iterations: Int,
    ): ByteArray = salt + iterations.toByte() + pin.digits.toByteArray()
}
