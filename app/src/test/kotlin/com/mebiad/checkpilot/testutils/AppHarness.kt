package com.mebiad.checkpilot.testutils

import android.app.Application
import android.os.Looper
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.Lifecycle
import androidx.startup.AppInitializer
import androidx.startup.InitializationProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.mebiad.checkpilot.MainActivity
import com.mebiad.checkpilot.core.clock.Clock
import com.mebiad.checkpilot.core.clock.ClockFake
import com.mebiad.checkpilot.data.local.crypto.DatabaseKeyProvider
import com.mebiad.checkpilot.data.local.crypto.DatabaseKeyProviderFake
import com.mebiad.checkpilot.data.local.db.CheckPilotDatabase
import com.mebiad.checkpilot.di.SETTINGS_FILE_NAME
import com.mebiad.checkpilot.di.mainModules
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.rules.ExternalResource
import org.junit.rules.TemporaryFolder
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.dsl.onClose
import org.koin.core.module.dsl.withOptions
import org.koin.dsl.module
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import java.time.Duration

/**
 * Runs the whole app on Robolectric for acceptance tests, with the real Koin modules and test overrides: the
 * database on a temporary file with the framework's SQLite instead of SQLCipher, whose native library doesn't load on
 * the JVM, the DataStore in the same folder, [clock] and [keyProvider].
 *
 * Koin starts before each test, so Given steps can seed state through [get] before [launch] opens the app.
 * Use it with `createEmptyComposeRule()`, which finds whichever activity [launch] started.
 */
class AppHarness : ExternalResource() {
    val clock = ClockFake()
    val keyProvider = DatabaseKeyProviderFake()

    private val storage = TemporaryFolder()
    private var scenario: ActivityScenario<MainActivity>? = null
    private var dataStoreScope: CoroutineScope? = null

    override fun before() {
        storage.create()
        startProcess()
        startApp()
    }

    override fun after() {
        stopApp()
        storage.delete()
    }

    fun launch() {
        check(scenario == null) { "The app is already launched; use restart() to open it again." }
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    /** Closes the app and opens it again over the same stored data, as after a restart or an app update. */
    fun restart() {
        stopApp()
        startApp()
        launch()
    }

    fun sendToBackground() {
        activeScenario().moveToState(Lifecycle.State.CREATED)
        waitForProcessLifecycle()
    }

    fun bringToForeground() {
        activeScenario().moveToState(Lifecycle.State.RESUMED)
    }

    inline fun <reified T : Any> get(): T = GlobalContext.get().get()

    /**
     * Runs the app's androidx.startup initializers, which attach ProcessLifecycleOwner, as the system does when it
     * starts the process. Robolectric doesn't, and gives each test a new Application while AppInitializer keeps the
     * first one in a static field, so that field is cleared first. Runs once per test: a second run on the same
     * Application would count every activity twice.
     */
    private fun startProcess() {
        AppInitializer::class.java
            .getDeclaredField("sInstance")
            .apply { isAccessible = true }
            .set(null, null)
        val app = ApplicationProvider.getApplicationContext<Application>()
        Robolectric.setupContentProvider(InitializationProvider::class.java, "${app.packageName}.androidx-startup")
    }

    private fun startApp() {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob()).also { dataStoreScope = it }
        val overrides =
            module {
                single<Clock> { clock }
                single<DatabaseKeyProvider> { keyProvider }
                single {
                    CheckPilotDatabase.build(
                        context = get(),
                        name = File(storage.root, CheckPilotDatabase.FILE_NAME).absolutePath,
                        openHelperFactory = null
                    )
                } withOptions { onClose { it?.close() } }
                single<DataStore<Preferences>> {
                    PreferenceDataStoreFactory.create(
                        scope = scope,
                        produceFile = { File(storage.root, "$SETTINGS_FILE_NAME.preferences_pb") }
                    )
                }
            }
        startKoin {
            androidContext(ApplicationProvider.getApplicationContext<Application>())
            modules(mainModules + overrides)
        }
    }

    private fun stopApp() {
        scenario?.close()
        scenario = null
        waitForProcessLifecycle()
        // Closes the database through its onClose. Cancelling the scope frees the settings file for the next DataStore.
        stopKoin()
        dataStoreScope?.cancel()
        dataStoreScope = null
    }

    /** ProcessLifecycleOwner sends ON_PAUSE and ON_STOP 700 ms after the last activity pauses, through a Handler. */
    private fun waitForProcessLifecycle() {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
    }

    private fun activeScenario(): ActivityScenario<MainActivity> =
        checkNotNull(scenario) { "Call launch() before changing the app's lifecycle." }
}
