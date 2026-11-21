package com.mebiad.checkpilot.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.mebiad.checkpilot.core.clock.Clock
import com.mebiad.checkpilot.core.clock.ClockImpl
import com.mebiad.checkpilot.data.local.crypto.DatabaseKeyProvider
import com.mebiad.checkpilot.data.local.crypto.DatabaseKeyProviderImpl
import com.mebiad.checkpilot.data.local.crypto.KeystorePassphraseCipherImpl
import com.mebiad.checkpilot.data.local.crypto.PassphraseCipher
import com.mebiad.checkpilot.data.local.db.CheckPilotDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

const val SETTINGS_FILE_NAME = "settings"

val coreModule =
    module {
        single<Clock> { ClockImpl() }
    }

val dataModule =
    module {
        single<PassphraseCipher> { KeystorePassphraseCipherImpl() }
        single<DatabaseKeyProvider> {
            DatabaseKeyProviderImpl(directory = androidContext().noBackupFilesDir, cipher = get())
        }
        single {
            System.loadLibrary("sqlcipher")
            CheckPilotDatabase.build(
                context = androidContext(),
                name = CheckPilotDatabase.FILE_NAME,
                openHelperFactory = SupportOpenHelperFactory(get<DatabaseKeyProvider>().passphrase())
            )
        }
        // Not the preferencesDataStore delegate: it allows one instance per process, and the test harness opens the
        // DataStore again after a restart.
        single<DataStore<Preferences>> {
            PreferenceDataStoreFactory.create(
                scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
                produceFile = { androidContext().preferencesDataStoreFile(SETTINGS_FILE_NAME) }
            )
        }
    }

val mainModules = listOf(coreModule, dataModule)
