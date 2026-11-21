package com.mebiad.checkpilot.data.local.crypto

/** Gives the passphrase that opens the encrypted database. */
interface DatabaseKeyProvider {
    fun passphrase(): ByteArray
}
