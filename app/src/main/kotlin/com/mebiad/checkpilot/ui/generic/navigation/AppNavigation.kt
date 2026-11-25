package com.mebiad.checkpilot.ui.generic.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.mebiad.checkpilot.domain.pin.PinUsecase
import com.mebiad.checkpilot.domain.session.CrewMember
import com.mebiad.checkpilot.ui.apps.capture.CaptureScreen
import com.mebiad.checkpilot.ui.apps.debrief.DebriefScreen
import com.mebiad.checkpilot.ui.apps.home.HomeScreen
import com.mebiad.checkpilot.ui.apps.pin.PinMode
import com.mebiad.checkpilot.ui.apps.pin.PinScreen
import com.mebiad.checkpilot.ui.apps.sessions.SessionsScreen
import com.mebiad.checkpilot.ui.apps.setup.SetupScreen
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject
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

@Serializable
data class Debrief(
    val sessionId: String,
    val crewMember: CrewMember,
) : NavKey

@Serializable
data object Sessions : NavKey

/**
 * [onLockSessionList] runs whenever the Session list leaves the back stack and whenever the app goes to the background,
 * so the PIN is asked again the next time the list shows.
 */
@Composable
fun AppNavigation(onLockSessionList: () -> Unit) {
    val backStack = rememberNavBackStack(Home)
    val listOpen = Sessions in backStack
    LaunchedEffect(listOpen) { if (!listOpen) onLockSessionList() }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP, lifecycleOwner = ProcessLifecycleOwner.get()) { onLockSessionList() }
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
                entry<Home> {
                    HomeScreen(
                        viewModel = koinViewModel(),
                        onContinueLastSession = { sessionId -> backStack.add(Capture(sessionId)) },
                        onNewSession = { backStack.add(Setup) },
                        onOpenSession = { backStack.add(Sessions) },
                        // Settings (US-8) comes with its own story.
                        onSettings = {}
                    )
                }
                entry<Setup> {
                    SetupScreen(
                        viewModel = koinViewModel(),
                        onBack = { backStack.removeLastOrNull() },
                        // The setup screen is done once the Session exists: Back from capture goes home.
                        onSessionStarted = { sessionId -> backStack[backStack.lastIndex] = Capture(sessionId) }
                    )
                }
                entry<Sessions> {
                    val unlocked by koinInject<PinUsecase>().unlocked.collectAsStateWithLifecycle()
                    if (unlocked) {
                        SessionsScreen(
                            viewModel = koinViewModel(),
                            onBack = { backStack.removeLastOrNull() },
                            onCapture = { sessionId -> backStack.add(Capture(sessionId)) },
                            onDebrief = { sessionId, crewMember -> backStack.add(Debrief(sessionId, crewMember)) }
                        )
                    } else {
                        PinScreen(
                            viewModel = koinViewModel { parametersOf(PinMode.Unlock) },
                            onBack = { backStack.removeLastOrNull() }
                        )
                    }
                }
                entry<Capture> { key ->
                    CaptureScreen(
                        viewModel = koinViewModel { parametersOf(key.sessionId) },
                        onGenerateDebrief = { crewMember -> backStack.add(Debrief(key.sessionId, crewMember)) },
                        onDone = { backStack.removeAll { it != Home } }
                    )
                }
                entry<Debrief> { key ->
                    DebriefScreen(
                        viewModel = koinViewModel { parametersOf(key.sessionId, key.crewMember) },
                        onLeave = { backStack.removeLastOrNull() }
                    )
                }
            }
    )
}
