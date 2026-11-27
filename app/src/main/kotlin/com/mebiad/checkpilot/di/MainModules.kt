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
import com.mebiad.checkpilot.data.local.db.CatalogueRepositoryImpl
import com.mebiad.checkpilot.data.local.db.CheckPilotDatabase
import com.mebiad.checkpilot.data.local.db.GradeRepositoryImpl
import com.mebiad.checkpilot.data.local.db.NoteRepositoryImpl
import com.mebiad.checkpilot.data.local.db.SessionRepositoryImpl
import com.mebiad.checkpilot.data.local.settings.PinStoreImpl
import com.mebiad.checkpilot.domain.catalogue.CatalogueRepository
import com.mebiad.checkpilot.domain.catalogue.EditCatalogueUsecase
import com.mebiad.checkpilot.domain.catalogue.EditCatalogueUsecaseImpl
import com.mebiad.checkpilot.domain.catalogue.ObserveCatalogueUsecase
import com.mebiad.checkpilot.domain.catalogue.ObserveCatalogueUsecaseImpl
import com.mebiad.checkpilot.domain.debrief.ObserveDebriefUsecase
import com.mebiad.checkpilot.domain.debrief.ObserveDebriefUsecaseImpl
import com.mebiad.checkpilot.domain.grade.GradeRepository
import com.mebiad.checkpilot.domain.grade.ObserveGradesUsecase
import com.mebiad.checkpilot.domain.grade.ObserveGradesUsecaseImpl
import com.mebiad.checkpilot.domain.grade.SetGradeUsecase
import com.mebiad.checkpilot.domain.grade.SetGradeUsecaseImpl
import com.mebiad.checkpilot.domain.note.EditNotesUsecase
import com.mebiad.checkpilot.domain.note.EditNotesUsecaseImpl
import com.mebiad.checkpilot.domain.note.NoteRepository
import com.mebiad.checkpilot.domain.note.ObserveNotesUsecase
import com.mebiad.checkpilot.domain.note.ObserveNotesUsecaseImpl
import com.mebiad.checkpilot.domain.pin.PinHasher
import com.mebiad.checkpilot.domain.pin.PinHasherImpl
import com.mebiad.checkpilot.domain.pin.PinStore
import com.mebiad.checkpilot.domain.pin.PinUsecase
import com.mebiad.checkpilot.domain.pin.PinUsecaseImpl
import com.mebiad.checkpilot.domain.session.ChangeSessionDateUsecase
import com.mebiad.checkpilot.domain.session.ChangeSessionDateUsecaseImpl
import com.mebiad.checkpilot.domain.session.DeleteSessionUsecase
import com.mebiad.checkpilot.domain.session.DeleteSessionUsecaseImpl
import com.mebiad.checkpilot.domain.session.DuplicateSessionUsecase
import com.mebiad.checkpilot.domain.session.DuplicateSessionUsecaseImpl
import com.mebiad.checkpilot.domain.session.ObserveSessionUsecase
import com.mebiad.checkpilot.domain.session.ObserveSessionUsecaseImpl
import com.mebiad.checkpilot.domain.session.SearchSessionsUsecase
import com.mebiad.checkpilot.domain.session.SearchSessionsUsecaseImpl
import com.mebiad.checkpilot.domain.session.SessionRepository
import com.mebiad.checkpilot.domain.session.StartSessionUsecase
import com.mebiad.checkpilot.domain.session.StartSessionUsecaseImpl
import com.mebiad.checkpilot.ui.apps.capture.CaptureViewModel
import com.mebiad.checkpilot.ui.apps.catalogue.CatalogueViewModel
import com.mebiad.checkpilot.ui.apps.debrief.DebriefViewModel
import com.mebiad.checkpilot.ui.apps.home.HomeViewModel
import com.mebiad.checkpilot.ui.apps.pin.PinViewModel
import com.mebiad.checkpilot.ui.apps.sessions.SessionsViewModel
import com.mebiad.checkpilot.ui.apps.setup.SetupViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModelOf
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

val domainModule =
    module {
        single<SessionRepository> { SessionRepositoryImpl(dao = get<CheckPilotDatabase>().sessionDao()) }
        single<CatalogueRepository> { CatalogueRepositoryImpl(dao = get<CheckPilotDatabase>().competencyDao()) }
        single<StartSessionUsecase> { StartSessionUsecaseImpl(repository = get(), clock = get()) }
        single<ObserveSessionUsecase> { ObserveSessionUsecaseImpl(repository = get()) }
        single<ObserveCatalogueUsecase> { ObserveCatalogueUsecaseImpl(repository = get()) }
        // One writer for the whole app keeps every Catalogue edit in call order.
        single<EditCatalogueUsecase> { EditCatalogueUsecaseImpl(repository = get()) }
        single<NoteRepository> { NoteRepositoryImpl(dao = get<CheckPilotDatabase>().noteDao()) }
        single<ObserveNotesUsecase> { ObserveNotesUsecaseImpl(repository = get()) }
        // One writer for the whole app keeps every Note write in call order.
        single<EditNotesUsecase> { EditNotesUsecaseImpl(repository = get(), catalogue = get()) }
        single<ChangeSessionDateUsecase> { ChangeSessionDateUsecaseImpl(repository = get()) }
        single<GradeRepository> { GradeRepositoryImpl(dao = get<CheckPilotDatabase>().gradeDao()) }
        single<ObserveGradesUsecase> { ObserveGradesUsecaseImpl(repository = get()) }
        single<SetGradeUsecase> { SetGradeUsecaseImpl(repository = get()) }
        single<ObserveDebriefUsecase> {
            ObserveDebriefUsecaseImpl(catalogue = get(), notes = get(), grades = get())
        }
        single<SearchSessionsUsecase> { SearchSessionsUsecaseImpl(sessions = get(), grades = get(), catalogue = get()) }
        single<DeleteSessionUsecase> { DeleteSessionUsecaseImpl(repository = get()) }
        single<DuplicateSessionUsecase> { DuplicateSessionUsecaseImpl(repository = get(), clock = get()) }
        single<PinStore> { PinStoreImpl(dataStore = get()) }
        single<PinHasher> { PinHasherImpl() }
        // One instance for the whole app: it holds the Session list's unlock in memory.
        single<PinUsecase> { PinUsecaseImpl(store = get(), hasher = get(), clock = get(), sessions = get()) }
    }

val uiModule =
    module {
        viewModelOf(::HomeViewModel)
        viewModelOf(::SetupViewModel)
        viewModelOf(::CaptureViewModel)
        viewModelOf(::DebriefViewModel)
        viewModelOf(::PinViewModel)
        viewModelOf(::SessionsViewModel)
        viewModelOf(::CatalogueViewModel)
    }

val mainModules = listOf(coreModule, dataModule, domainModule, uiModule)
