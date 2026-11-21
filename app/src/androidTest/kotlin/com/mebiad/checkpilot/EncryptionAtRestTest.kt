package com.mebiad.checkpilot

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mebiad.checkpilot.data.local.crypto.DatabaseKeyProviderImpl
import com.mebiad.checkpilot.data.local.crypto.KeystorePassphraseCipherImpl
import com.mebiad.checkpilot.data.local.db.CheckPilotDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.security.KeyStore

/** Opens the real SQLCipher database with the real Keystore-backed key provider. Runs on a phone or emulator only. */
@RunWith(AndroidJUnit4::class)
class EncryptionAtRestTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val folder = File(context.cacheDir, "encryption-at-rest")
    private val databaseFile = File(folder, CheckPilotDatabase.FILE_NAME)

    @Before
    fun setUp() {
        System.loadLibrary("sqlcipher")
        folder.deleteRecursively()
        folder.mkdirs()
    }

    @After
    fun tearDown() {
        folder.deleteRecursively()
        KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.deleteEntry(KEY_ALIAS)
    }

    @Test
    fun theDatabaseOpensWithTheProvidersPassphraseAndItsFileIsNotPlainSqlite() {
        // Given
        open().close()

        // When: a new provider reads the passphrase back from the key file, as after a restart
        val database = open()
        val competencies =
            database.openHelper.readableDatabase.query("SELECT COUNT(*) FROM competency").use {
                it.moveToFirst()
                it.getInt(0)
            }
        database.close()

        // Then
        assertEquals(9, competencies)
        val header = ByteArray(PLAIN_HEADER.size)
        databaseFile.inputStream().use { it.read(header) }
        assertFalse(header.contentEquals(PLAIN_HEADER))
    }

    private fun open(): CheckPilotDatabase {
        val provider = DatabaseKeyProviderImpl(directory = folder, cipher = KeystorePassphraseCipherImpl(KEY_ALIAS))
        return CheckPilotDatabase.build(
            context = context,
            name = databaseFile.absolutePath,
            openHelperFactory = SupportOpenHelperFactory(provider.passphrase())
        )
    }

    private companion object {
        const val KEY_ALIAS = "encryption_at_rest_test"
        val PLAIN_HEADER = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)
    }
}
