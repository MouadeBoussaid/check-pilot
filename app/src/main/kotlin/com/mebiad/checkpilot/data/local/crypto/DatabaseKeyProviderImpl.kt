package com.mebiad.checkpilot.data.local.crypto

import java.io.File
import java.security.SecureRandom

/**
 * Creates a random passphrase on first use and keeps it, encrypted by [cipher], in a key file in [directory]
 * (`noBackupFilesDir` in the app). Later calls, and later instances over the same folder, read it back.
 */
class DatabaseKeyProviderImpl(
    private val directory: File,
    private val cipher: PassphraseCipher,
    private val random: SecureRandom = SecureRandom(),
) : DatabaseKeyProvider {
    private val keyFile = File(directory, KEY_FILE_NAME)

    private val passphrase: ByteArray by lazy {
        if (keyFile.exists()) cipher.decrypt(keyFile.readBytes()) else create()
    }

    override fun passphrase(): ByteArray = passphrase.copyOf()

    private fun create(): ByteArray {
        val created = ByteArray(PASSPHRASE_BYTES).also(random::nextBytes)
        // Written aside and then renamed, so a process killed mid-write can't leave a truncated key file behind:
        // without the key, the database can never be opened again.
        val partial = File(directory, "$KEY_FILE_NAME.partial")
        partial.writeBytes(cipher.encrypt(created))
        check(partial.renameTo(keyFile)) { "Couldn't store the database key file." }
        return created
    }

    private companion object {
        const val KEY_FILE_NAME = "database.key"
        const val PASSPHRASE_BYTES = 32
    }
}
