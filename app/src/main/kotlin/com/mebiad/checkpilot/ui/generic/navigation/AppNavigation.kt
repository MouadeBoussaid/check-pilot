package com.mebiad.checkpilot.ui.generic.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.mebiad.checkpilot.ui.apps.capture.CaptureScreen
import com.mebiad.checkpilot.ui.apps.home.HomeScreen
import com.mebiad.checkpilot.ui.apps.setup.SetupScreen
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Serializable
data object Home : NavKey

@Serializable
data object Setup : NavKey

@Serializable
data class Capture(
    val sessionId: String,
) : NavKey

@Composable
fun AppNavigation() {
    val backStack = rememberNavBackStack(Home)
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators =
            listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator()
            ),
        entryProvider =
            entryProvider {
                entry<Home> { HomeScreen(onNewSession = { backStack.add(Setup) }) }
                entry<Setup> {
                    SetupScreen(
                        viewModel = koinViewModel(),
                        onBack = { backStack.removeLastOrNull() },
                        // The setup screen is done once the Session exists: Back from capture goes home.
                        onSessionStarted = { sessionId -> backStack[backStack.lastIndex] = Capture(sessionId) }
                    )
                }
                entry<Capture> { key -> CaptureScreen(viewModel = koinViewModel { parametersOf(key.sessionId) }) }
            }
    )
}
