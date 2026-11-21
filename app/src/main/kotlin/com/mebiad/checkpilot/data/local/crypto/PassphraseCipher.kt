package com.mebiad.checkpilot.data.local.crypto

interface PassphraseCipher {
    fun encrypt(plain: ByteArray): ByteArray

    fun decrypt(encrypted: ByteArray): ByteArray
}
