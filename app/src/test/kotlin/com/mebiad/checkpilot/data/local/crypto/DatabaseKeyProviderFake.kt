package com.mebiad.checkpilot.data.local.crypto

class DatabaseKeyProviderFake(
    var passphrase: ByteArray = ByteArray(32) { it.toByte() },
) : DatabaseKeyProvider {
    override fun passphrase(): ByteArray = passphrase.copyOf()
}
