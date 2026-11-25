package com.mebiad.checkpilot.domain.pin

/** Turns a PIN into a slow salted hash, so a copied settings file doesn't give the PIN away. */
interface PinHasher {
    /** The number of rounds new hashes are made with. */
    val iterations: Int

    fun newSalt(): ByteArray

    fun hash(
        pin: Pin,
        salt: ByteArray,
        iterations: Int,
    ): ByteArray
}
