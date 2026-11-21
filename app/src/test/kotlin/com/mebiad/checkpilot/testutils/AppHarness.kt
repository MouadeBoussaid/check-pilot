package com.mebiad.checkpilot.testutils

import android.app.Application
import android.os.Looper
import androidx.lifecycle.Lifecycle
import androidx.startup.AppInitializer
import androidx.startup.InitializationProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.mebiad.checkpilot.MainActivity
import com.mebiad.checkpilot.core.clock.Clock
import com.mebiad.checkpilot.core.clock.ClockFake
import com.mebiad.checkpilot.di.mainModules
import org.junit.rules.ExternalResource
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import java.time.Duration

/**
 * Runs the whole app on Robolectric for acceptance tests, with the real Koin modules and test overrides.
 *
 * Koin starts before each test, so Given steps can seed state through [get] before [launch] opens the app.
 * Use it with `createEmptyComposeRule()`, which finds whichever activity [launch] started.
 */
class AppHarness : ExternalResource() {
    val clock = ClockFake()

    private var scenario: ActivityScenario<MainActivity>? = null

    override fun before() {
        startProcess()
        startApp()
    }

    override fun after() {
        stopApp()
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
        val overrides =
            module {
                single<Clock> { clock }
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
        stopKoin()
    }

    /** ProcessLifecycleOwner sends ON_PAUSE and ON_STOP 700 ms after the last activity pauses, through a Handler. */
    private fun waitForProcessLifecycle() {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
    }

    private fun activeScenario(): ActivityScenario<MainActivity> =
        checkNotNull(scenario) { "Call launch() before changing the app's lifecycle." }
}
