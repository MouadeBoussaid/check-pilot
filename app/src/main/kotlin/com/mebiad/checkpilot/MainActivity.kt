package com.mebiad.checkpilot

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mebiad.checkpilot.domain.pin.PinUsecase
import com.mebiad.checkpilot.ui.generic.navigation.AppNavigation
import com.mebiad.checkpilot.ui.generic.theme.CheckPilotTheme
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val pin: PinUsecase by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        // The theme is dark whatever the system setting, so the system bars' icons must stay light.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        setContent {
            CheckPilotTheme {
                AppNavigation(onLockSessionList = pin::lock)
            }
        }
    }
}
